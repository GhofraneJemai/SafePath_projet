package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safepath.R;
import com.example.safepath.models.EmergencyContact;
import com.example.safepath.models.SOSAlert;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class SOSActivity extends AppCompatActivity {
    private Button sosButton, callPoliceButton, callAmbulanceButton;
    private TextView countdownText;
    private CountDownTimer countDownTimer;
    private boolean sosActive = false;
    private FusedLocationProviderClient fusedLocationClient;
    private Switch locationSharingSwitch;

    private DatabaseReference sosAlertsRef, contactsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        // Initialisation Realtime Database
        sosAlertsRef = FirebaseDatabase.getInstance().getReference("sos_alerts");
        contactsRef = FirebaseDatabase.getInstance().getReference("emergency_contacts");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
    }

    private void initViews() {
        sosButton = findViewById(R.id.sosButton);
        callPoliceButton = findViewById(R.id.callPoliceButton);
        callAmbulanceButton = findViewById(R.id.callAmbulanceButton);
        countdownText = findViewById(R.id.countdownText);
        locationSharingSwitch = findViewById(R.id.locationSharingSwitch);

        sosButton.setOnClickListener(v -> toggleSOS());
        callPoliceButton.setOnClickListener(v -> callEmergency("17"));
        callAmbulanceButton.setOnClickListener(v -> callEmergency("15"));
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
    }

    private void triggerEmergencySOS() {
        // Récupérer la position actuelle
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            sendSOSAlert(location.getLatitude(), location.getLongitude());
                        }
                    });
        }

        // Appeler les secours automatiquement
        callEmergency("112");

        // Envoyer notifications aux contacts si partage activé
        if (locationSharingSwitch.isChecked()) {
            notifyEmergencyContacts();
        }

        Toast.makeText(this, "Alerte SOS activée! Les secours ont été alertés.", Toast.LENGTH_LONG).show();
    }

    private void sendSOSAlert(double latitude, double longitude) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SOSAlert sosAlert = new SOSAlert(userId, latitude, longitude);

        // CORRECTION : Utiliser Realtime Database au lieu de Firestore
        String alertId = sosAlertsRef.push().getKey();
        sosAlertsRef.child(alertId).setValue(sosAlert)
                .addOnSuccessListener(aVoid -> {
                    // Alert envoyée avec succès
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur envoi alerte", Toast.LENGTH_SHORT).show();
                });
    }

    private void callEmergency(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, 101);
        }
    }

    private void notifyEmergencyContacts() {
        // CORRECTION : Utiliser Realtime Database au lieu de Firestore
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query query = contactsRef.orderByChild("userId").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    EmergencyContact contact = snapshot.getValue(EmergencyContact.class);
                    if (contact != null) {
                        sendNotificationToContact(contact);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SOSActivity.this, "Erreur chargement contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendNotificationToContact(EmergencyContact contact) {
        // Implémentation FCM à ajouter plus tard
        // Pour l'instant, juste un Toast
        Toast.makeText(this, "Alerte envoyée à " + contact.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }



}