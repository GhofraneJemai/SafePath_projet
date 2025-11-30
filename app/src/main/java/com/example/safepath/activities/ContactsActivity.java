package com.example.safepath.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.adapters.ContactsAdapter;
import com.example.safepath.models.EmergencyContact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.example.safepath.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsActivity extends AppCompatActivity {
    private RecyclerView contactsRecyclerView;
    private ContactsAdapter adapter;
    private List<EmergencyContact> contacts;
    private FloatingActionButton addContactFab;
    private DatabaseReference contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactsRef = FirebaseDatabase.getInstance().getReference("emergency_contacts");
        initViews();
        loadContacts();
    }

    private void initViews() {
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        addContactFab = findViewById(R.id.addContactFab);

        contacts = new ArrayList<>();
        adapter = new ContactsAdapter(contacts, this::deleteContact);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(adapter);

        addContactFab.setOnClickListener(v -> showAddContactDialog());
    }

    private void loadContacts() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query query = contactsRef.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                contacts.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    EmergencyContact contact = snapshot.getValue(EmergencyContact.class);
                    if (contact != null) {
                        contact.setId(snapshot.getKey());
                        contacts.add(contact);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ContactsActivity.this, "Erreur chargement contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ajouter un contact de confiance");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_contact, null);
        EditText nameEditText = view.findViewById(R.id.nameEditText);
        EditText phoneEditText = view.findViewById(R.id.phoneEditText);
        EditText emailEditText = view.findViewById(R.id.emailEditText);

        builder.setView(view);
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            String name = nameEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Nom et téléphone obligatoires", Toast.LENGTH_SHORT).show();
                return;
            }

            addContact(name, phone, email);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void addContact(String name, String phone, String email) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        EmergencyContact contact = new EmergencyContact(userId, name, phone, email);

        String contactId = contactsRef.push().getKey();
        contactsRef.child(contactId).setValue(contact)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Contact ajouté", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur ajout contact", Toast.LENGTH_SHORT).show());
    }

    private void deleteContact(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le contact")
                .setMessage("Êtes-vous sûr de vouloir supprimer " + contact.getName() + " ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    if (contact.getId() != null) {
                        contactsRef.child(contact.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Contact supprimé", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}