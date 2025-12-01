package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safepath.R;
import com.example.safepath.models.EmergencyContact;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SOSActivity extends AppCompatActivity {
    private static final String TAG = "SOSActivity";

    private Button sosButton, callPoliceButton, callAmbulanceButton;
    private TextView countdownText;
    private CountDownTimer countDownTimer;
    private boolean sosActive = false;
    private FusedLocationProviderClient fusedLocationClient;
    private Switch locationSharingSwitch;

    private DatabaseReference sosAlertsRef, contactsRef;
    private FirebaseUser currentUser;

    // Codes de permission
    private static final int PERMISSION_REQUEST_CALL = 101;
    private static final int PERMISSION_REQUEST_SMS = 102;
    private static final int PERMISSION_REQUEST_LOCATION = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        // Initialisation Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app"
        );
        sosAlertsRef = database.getReference("sos_alerts");
        contactsRef = database.getReference("emergency_contacts");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();

        Log.d(TAG, "Activité SOS démarrée, utilisateur: " + currentUser.getUid());

        // Vérification Firebase
        testFirebaseConnection();
    }

    private void initViews() {
        sosButton = findViewById(R.id.sosButton);
        callPoliceButton = findViewById(R.id.callPoliceButton);
        callAmbulanceButton = findViewById(R.id.callAmbulanceButton);
        countdownText = findViewById(R.id.countdownText);
        locationSharingSwitch = findViewById(R.id.locationSharingSwitch);

        sosButton.setOnClickListener(v -> toggleSOS());
        callPoliceButton.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Police cliqué");
            makeEmergencyCallWithHistory("17", "Police");
        });
        callAmbulanceButton.setOnClickListener(v -> {
            Log.d(TAG, "Bouton SAMU cliqué");
            makeEmergencyCallWithHistory("15", "SAMU");
        });

        // TEST: Ajouter un bouton de test
        Button testButton = findViewById(R.id.debugButton); // Assurez-vous d'avoir ce bouton dans votre layout
        if (testButton != null) {
            testButton.setOnClickListener(v -> {
                testFirebaseSaveManually("Test Police", "17");
                testFirebaseSaveManually("Test SAMU", "15");
            });
        }
    }

    private void toggleSOS() {
        if (!sosActive) {
            startSOSCountdown();
        } else {
            cancelSOS();
        }
    }

    private void startSOSCountdown() {
        sosButton.setBackgroundColor(ContextCompat.getColor(this, R.color.danger_color));
        sosButton.setText("ANNULER SOS");
        sosActive = true;

        countDownTimer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownText.setText("Lancement dans " + millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                triggerEmergencySOS();
            }
        }.start();
    }

    private void cancelSOS() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        sosButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color));
        sosButton.setText("ALERTE SOS");
        countdownText.setText("");
        sosActive = false;

        Toast.makeText(this, "SOS annulé", Toast.LENGTH_SHORT).show();
    }

    private void triggerEmergencySOS() {
        Log.d(TAG, "Déclenchement de l'alerte SOS");

        // Vérifier les permissions de localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
            return;
        }

        // Récupérer la position actuelle
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    double latitude = 0;
                    double longitude = 0;

                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d(TAG, "Position obtenue: " + latitude + ", " + longitude);
                    }

                    // 1. Sauvegarder l'alerte SOS
                    saveSOS("SOS", "Secours d'urgence", "112", latitude, longitude);

                    // 2. Sauvegarder l'appel au 112 (séparément)
                    saveSOS("Appel", "Secours", "112", latitude, longitude);

                    // 3. Passer l'appel
                    makeDirectCall("112");

                    // 4. Envoyer notifications aux contacts si partage activé
                    if (locationSharingSwitch.isChecked()) {
                        notifyEmergencyContacts(latitude, longitude);
                    }

                    Toast.makeText(this, "🚨 Alerte SOS activée! 🚨", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur localisation: " + e.getMessage());
                    // Sauvegarder sans position
                    saveSOS("SOS", "Secours d'urgence", "112", 0, 0);
                    saveSOS("Appel", "Secours", "112", 0, 0);
                    makeDirectCall("112");
                    Toast.makeText(this, "Alerte SOS activée!", Toast.LENGTH_LONG).show();
                });
    }

    private void makeEmergencyCallWithHistory(String number, String serviceName) {
        Log.d(TAG, "Tentative d'appel à " + serviceName + " (" + number + ")");

        // Vérifier les permissions d'appel
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Demande de permission CALL_PHONE");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CALL_PHONE
            }, PERMISSION_REQUEST_CALL);
            return;
        }

        // Vérifier la permission de localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Pas de permission localisation, sauvegarder sans position
            saveSOS("Appel", serviceName, number, 0, 0);
            makeDirectCall(number);
        } else {
            // Obtenir la position d'abord
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        double latitude = 0;
                        double longitude = 0;

                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }

                        // Sauvegarder AVANT l'appel - Utiliser la même méthode que ContactsActivity
                        saveSOS("Appel", serviceName, number, latitude, longitude);

                        // Passer l'appel
                        makeDirectCall(number);
                    })
                    .addOnFailureListener(e -> {
                        // Sauvegarder sans position
                        saveSOS("Appel", serviceName, number, 0, 0);
                        makeDirectCall(number);
                    });
        }
    }

    private void makeDirectCall(String number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            startActivity(callIntent);
            Toast.makeText(this, "Appel en cours...", Toast.LENGTH_SHORT).show();

            // Vérifier après 3 secondes si l'appel a été sauvegardé
            new Handler().postDelayed(() -> {
                checkIfSOSIsSaved("Appel", "Police/SAMU", number);
            }, 3000);

        } catch (SecurityException e) {
            Log.e(TAG, "Erreur sécurité appel: " + e.getMessage());
            Toast.makeText(this, "Erreur lors de l'appel", Toast.LENGTH_SHORT).show();
        }
    }

    // MÉTHODE SIMILAIRE À CELLE DE ContactsActivity
    private void saveSOS(String type, String contactName, String contactPhone, double latitude, double longitude) {
        if (currentUser == null) {
            Log.e(TAG, "Utilisateur non connecté pour sauvegarde SOS");
            return;
        }

        String sosId = sosAlertsRef.push().getKey();
        if (sosId == null) {
            Log.e(TAG, "Erreur génération ID SOS");
            return;
        }

        // Créer l'objet Map pour Firebase - EXACTEMENT comme dans ContactsActivity
        Map<String, Object> sosMap = new HashMap<>();
        sosMap.put("id", sosId);
        sosMap.put("userId", currentUser.getUid());
        sosMap.put("userName", getCurrentUserName());
        sosMap.put("type", type);
        sosMap.put("contactName", contactName);
        sosMap.put("contactPhone", contactPhone);
        sosMap.put("latitude", latitude);
        sosMap.put("longitude", longitude);
        sosMap.put("locationUrl", "https://maps.google.com/?q=" + latitude + "," + longitude);
        sosMap.put("timestamp", System.currentTimeMillis());
        sosMap.put("date", new Date().toString());
        sosMap.put("status", "envoyé");
        sosMap.put("message", "Alerte " + type + " envoyée à " + contactName);
        // PAS de contactsNotified ici pour être cohérent avec ContactsActivity

        // SAUVEGARDER DANS FIREBASE
        sosAlertsRef.child(sosId).setValue(sosMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ SOS sauvegardé dans Firebase: " + type + " à " + contactName + " (" + contactPhone + ")");
                    Toast.makeText(SOSActivity.this,
                            "Appel enregistré: " + contactName,
                            Toast.LENGTH_SHORT).show();

                    // Vérifier immédiatement après la sauvegarde
                    new Handler().postDelayed(() -> {
                        verifyRecentSave(contactPhone);
                    }, 1000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Erreur sauvegarde SOS: " + e.getMessage());
                    Toast.makeText(SOSActivity.this,
                            "Erreur: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Nouvelle méthode pour vérifier immédiatement
    private void verifyRecentSave(String phoneNumber) {
        Query query = sosAlertsRef.orderByChild("timestamp")
                .limitToLast(1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    String savedPhone = data.child("contactPhone").getValue(String.class);
                    if (phoneNumber.equals(savedPhone)) {
                        Log.d(TAG, "✅ VÉRIFICATION: Appel " + phoneNumber + " sauvegardé avec succès!");
                    } else {
                        Log.e(TAG, "❌ VÉRIFICATION: Appel " + phoneNumber + " NON trouvé!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erreur vérification: " + error.getMessage());
            }
        });
    }

    private String getCurrentUserName() {
        if (currentUser == null) return "Utilisateur";

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

    private void notifyEmergencyContacts(double latitude, double longitude) {
        if (currentUser == null) return;

        Log.d(TAG, "Notification des contacts, position: " + latitude + ", " + longitude);

        Query query = contactsRef.orderByChild("userId").equalTo(currentUser.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int contactCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    EmergencyContact contact = snapshot.getValue(EmergencyContact.class);
                    if (contact != null) {
                        sendEmergencySMSToContact(contact, latitude, longitude);
                        contactCount++;
                    }
                }

                if (contactCount > 0) {
                    Toast.makeText(SOSActivity.this,
                            "Alerte envoyée à " + contactCount + " contact(s)",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Erreur chargement contacts: " + databaseError.getMessage());
            }
        });
    }

    private void sendEmergencySMSToContact(EmergencyContact contact, double latitude, double longitude) {
        // Vérifier la permission SMS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Demande de permission SEND_SMS");
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.SEND_SMS
            }, PERMISSION_REQUEST_SMS);
            return;
        }

        String userName = getCurrentUserName();
        String message = buildEmergencyMessage(userName, latitude, longitude);

        try {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(contact.getPhone(), null, message, null, null);

            // Sauvegarder le SMS
            saveSOS("SMS", contact.getName(), contact.getPhone(), latitude, longitude);

            Log.d(TAG, "SMS envoyé à " + contact.getName());
        } catch (Exception e) {
            Log.e(TAG, "Erreur envoi SMS: " + e.getMessage());
        }
    }

    private String buildEmergencyMessage(String userName, double latitude, double longitude) {
        return "🚨 URGENCE - " + userName + " 🚨\n" +
                "Je suis en danger et j'ai besoin d'aide immédiate !\n\n" +
                "📍 Ma position :\n" +
                "https://maps.google.com/?q=" + latitude + "," + longitude + "\n\n" +
                "Latitude: " + latitude + "\n" +
                "Longitude: " + longitude + "\n\n" +
                "Merci de contacter les secours si nécessaire.\n" +
                "(Message automatique SafePath)";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "Résultat permission, code: " + requestCode);

        if (requestCode == PERMISSION_REQUEST_CALL) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission appel accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission appel refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission SMS accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission localisation accordée", Toast.LENGTH_SHORT).show();
                triggerEmergencySOS();
            } else {
                Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_SHORT).show();
                saveSOS("SOS", "Secours", "112", 0, 0);
                saveSOS("Appel", "Secours", "112", 0, 0);
                makeDirectCall("112");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // Méthode de debug pour vérifier les données Firebase
    private void debugFirebaseData() {
        sosAlertsRef.orderByChild("userId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "=== DONNÉES FIREBASE (sos_alerts) ===");
                        Log.d(TAG, "Nombre d'alertes: " + snapshot.getChildrenCount());
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Log.d(TAG, "ID: " + data.getKey());
                            Log.d(TAG, "  Type: " + data.child("type").getValue());
                            Log.d(TAG, "  Contact: " + data.child("contactName").getValue());
                            Log.d(TAG, "  Téléphone: " + data.child("contactPhone").getValue());
                            Log.d(TAG, "  Timestamp: " + data.child("timestamp").getValue());
                            Log.d(TAG, "---");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Erreur debug Firebase: " + error.getMessage());
                    }
                });
    }

    private void checkIfSOSIsSaved(String type, String serviceName, String phoneNumber) {
        Query query = sosAlertsRef.orderByChild("userId")
                .equalTo(currentUser.getUid())
                .limitToLast(5); // Check last 5 entries

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "=== VÉRIFICATION DES SAUVEGARDES ===");
                Log.d(TAG, "Recherche: " + type + " - " + serviceName + " - " + phoneNumber);
                Log.d(TAG, "Total alertes trouvées: " + snapshot.getChildrenCount());

                boolean found = false;
                for (DataSnapshot data : snapshot.getChildren()) {
                    String savedPhone = data.child("contactPhone").getValue(String.class);
                    String savedType = data.child("type").getValue(String.class);
                    String savedContact = data.child("contactName").getValue(String.class);

                    Log.d(TAG, "Enregistrement trouvé: " +
                            "Type=" + savedType +
                            ", Contact=" + savedContact +
                            ", Phone=" + savedPhone);

                    if (phoneNumber.equals(savedPhone)) {
                        found = true;
                        Log.d(TAG, "✅ Appel " + serviceName + " bien sauvegardé!");
                        break;
                    }
                }

                if (!found) {
                    Log.e(TAG, "❌ Appel " + serviceName + " NON sauvegardé!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erreur vérification: " + error.getMessage());
            }
        });
    }

    private void testFirebaseConnection() {
        String testKey = sosAlertsRef.push().getKey();
        Map<String, Object> testData = new HashMap<>();
        testData.put("test", "Test Firebase connection");
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("userId", currentUser.getUid());

        sosAlertsRef.child(testKey).setValue(testData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Firebase connection test: SUCCESS");
                    Toast.makeText(this, "Connexion Firebase OK", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firebase connection test: FAILED - " + e.getMessage());
                    Toast.makeText(this, "Erreur Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Nouvelle méthode pour tester manuellement
    private void testFirebaseSaveManually(String serviceName, String phoneNumber) {
        Log.d(TAG, "Test manuel: Sauvegarde de " + serviceName + " (" + phoneNumber + ")");
        saveSOS("Appel", serviceName, phoneNumber, 0.0, 0.0);
    }

    // Ajoutez ce bouton dans votre layout pour tester
    public void onDebugClick(View view) {
        debugFirebaseData();
    }
}