package com.example.safepath.activities;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.adapters.ContactsAdapter;
import com.example.safepath.models.EmergencyContact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity {

    private RecyclerView contactsRecyclerView;
    private ContactsAdapter adapter;
    private List<EmergencyContact> contacts;
    private FloatingActionButton addContactFab;
    private DatabaseReference contactsRef, sosRef;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseUser currentUser;

    private ValueEventListener contactsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        // Bouton retour
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(ContactsActivity.this, MainActivity.class));
            finish();
        });

        // Initialisation Firebase - SEULEMENT Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app"
        );
        contactsRef = database.getReference("emergency_contacts");
        sosRef = database.getReference("sos_alerts");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        initViews();
        setupContactsListener(); // Configure le listener pour écouter les changements
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nettoyer le listener pour éviter les fuites mémoire
        if (contactsListener != null) {
            contactsRef.removeEventListener(contactsListener);
        }
    }

    private void initViews() {
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        addContactFab = findViewById(R.id.addContactFab);

        contacts = new ArrayList<>();

        adapter = new ContactsAdapter(contacts, new ContactsAdapter.OnContactActionListener() {
            @Override
            public void onDelete(EmergencyContact contact) {
                deleteContact(contact);
            }

            @Override
            public void onCall(EmergencyContact contact) {
                makeEmergencyCall(contact);
            }

            @Override
            public void onSMS(EmergencyContact contact) {
                sendEmergencySMS(contact);
            }
        }, this);

        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(adapter);

        addContactFab.setOnClickListener(v -> showAddContactDialog());
    }

    private void setupContactsListener() {
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        // Filtrer les contacts par userId
        Query query = contactsRef.orderByChild("userId").equalTo(currentUser.getUid());

        // Créer le listener qui écoute en temps réel
        contactsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<EmergencyContact> updatedContacts = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    EmergencyContact contact = snapshot.getValue(EmergencyContact.class);
                    if (contact != null) {
                        contact.setId(snapshot.getKey());
                        updatedContacts.add(contact);
                    }
                }

                // Mettre à jour la liste locale
                contacts.clear();
                contacts.addAll(updatedContacts);
                adapter.notifyDataSetChanged();

                Log.d(TAG, "Contacts mis à jour depuis Firebase: " + contacts.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ContactsActivity.this,
                        "Erreur: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Attacher le listener à la référence
        query.addValueEventListener(contactsListener);
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter un contact de confiance");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_contact, null);
        EditText nameEditText = view.findViewById(R.id.nameEditText);
        EditText phoneEditText = view.findViewById(R.id.phoneEditText);
        EditText emailEditText = view.findViewById(R.id.emailEditText);

        builder.setView(view);
        builder.setPositiveButton("Ajouter", null);
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        addButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Nom et téléphone obligatoires", Toast.LENGTH_SHORT).show();
                return;
            }

            // AJOUTER DIRECTEMENT À FIREBASE - PAS À LA LISTE LOCALE
            addContactToFirebase(name, phone, email);
            dialog.dismiss();
        });
    }

    // Méthode qui ajoute UNIQUEMENT à Firebase
    private void addContactToFirebase(String name, String phone, String email) {
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifier si le contact existe déjà
        Query checkQuery = contactsRef.orderByChild("userId").equalTo(currentUser.getUid());
        checkQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean contactExists = false;

                for (DataSnapshot data : snapshot.getChildren()) {
                    String existingPhone = data.child("phone").getValue(String.class);
                    if (phone.equals(existingPhone)) {
                        contactExists = true;
                        break;
                    }
                }

                if (contactExists) {
                    Toast.makeText(ContactsActivity.this,
                            "Ce numéro est déjà enregistré",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Créer le contact
                EmergencyContact contact = new EmergencyContact(
                        currentUser.getUid(),
                        name,
                        phone,
                        email
                );

                // Générer un ID unique
                String contactId = contactsRef.push().getKey();
                if (contactId == null) {
                    Toast.makeText(ContactsActivity.this,
                            "Erreur création contact",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                contact.setId(contactId);

                // AJOUTER UNIQUEMENT À FIREBASE
                contactsRef.child(contactId).setValue(contact)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ContactsActivity.this,
                                    "Contact ajouté",
                                    Toast.LENGTH_SHORT).show();

                            // NE PAS AJOUTER À LA LISTE LOCALE ICI
                            // Le ValueEventListener se chargera de mettre à jour l'interface
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ContactsActivity.this,
                                    "Erreur: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactsActivity.this,
                        "Erreur vérification: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteContact(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Supprimer " + contact.getName() + " ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    if (contact.getId() != null) {
                        // SUPPRIMER UNIQUEMENT DE FIREBASE
                        contactsRef.child(contact.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this,
                                            "Contact supprimé",
                                            Toast.LENGTH_SHORT).show();
                                    // Le listener mettra à jour automatiquement l'interface
                                });
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void makeEmergencyCall(EmergencyContact contact) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 2);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Sauvegarder l'appel dans l'historique SOS
                saveSOS("Appel", contact, location.getLatitude(), location.getLongitude());

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + contact.getPhone()));
                startActivity(callIntent);

                Toast.makeText(this, "Appel à " + contact.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Impossible d'obtenir votre position", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendEmergencySMS(EmergencyContact contact) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String message = buildEmergencyMessage(contact.getName(), location.getLatitude(), location.getLongitude());

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(contact.getPhone(), null, message, null, null);

                // Sauvegarder dans l'historique SOS
                saveSOS("SMS", contact, location.getLatitude(), location.getLongitude());

                Toast.makeText(ContactsActivity.this, "SMS envoyé à " + contact.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ContactsActivity.this, "Impossible d'obtenir votre position", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildEmergencyMessage(String contactName, double latitude, double longitude) {
        return "🚨 URGENCE - " + contactName + " 🚨\n" +
                "Je suis en danger et j'ai besoin d'aide immédiate !\n\n" +
                "📍 Ma position :\n" +
                "https://maps.google.com/?q=" + latitude + "," + longitude + "\n\n" +
                "Latitude: " + latitude + "\n" +
                "Longitude: " + longitude + "\n\n" +
                "Merci de contacter les secours si nécessaire.\n" +
                "(Message automatique SafePath)";
    }

    private void saveSOS(String type, EmergencyContact contact, double latitude, double longitude) {
        if (currentUser == null) return;

        String sosId = sosRef.push().getKey();
        if (sosId == null) return;

        Map<String, Object> sosMap = new HashMap<>();
        sosMap.put("id", sosId);
        sosMap.put("userId", currentUser.getUid());
        sosMap.put("userName", getCurrentUserName());
        sosMap.put("type", type);
        sosMap.put("contactName", contact.getName());
        sosMap.put("contactPhone", contact.getPhone());
        sosMap.put("latitude", latitude);
        sosMap.put("longitude", longitude);
        sosMap.put("locationUrl", "https://maps.google.com/?q=" + latitude + "," + longitude);
        sosMap.put("timestamp", System.currentTimeMillis());
        sosMap.put("date", new Date().toString());
        sosMap.put("status", "envoyé");
        sosMap.put("message", "Alerte " + type + " envoyée à " + contact.getName());

        // SAUVEGARDER UNIQUEMENT DANS FIREBASE
        sosRef.child(sosId).setValue(sosMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "SOS sauvegardé dans Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur sauvegarde SOS: " + e.getMessage());
                });
    }

    private String getCurrentUserName() {
        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }

        String email = currentUser.getEmail();
        if (email != null && !email.isEmpty()) {
            return email.split("@")[0];
        }

        return "Utilisateur";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) { // SMS
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission SMS accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 2) { // Appel
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission appel accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission appel refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
}