package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.safepath.R;
import com.example.safepath.models.Report;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Date;

public class ReportActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_IMAGE = 100;
    private static final int PERMISSION_REQUEST_STORAGE = 101;
    private static final int PERMISSION_REQUEST_LOCATION = 102;

    private static final String TAG = "ReportActivity";
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_LOCATION_INTERVAL = 5000; // 5 seconds
    private static final float MINIMUM_ACCURACY = 50.0f; // 50 meters

    private Spinner dangerTypeSpinner;
    private EditText descriptionEditText;
    private ImageView photoImageView;
    private Button takePhotoButton, submitButton;
    private ProgressBar progressBar;

    private DatabaseReference reportsRef;
    private FirebaseStorage storage;
    private Uri selectedImageUri;
    private boolean isSubmitting = false;

    // Variables pour la localisation
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private boolean hasValidLocation = false;
    private boolean isRequestingLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "=== REPORT ACTIVITY STARTED ===");
        Log.d(TAG, "═══════════════════════════════════════");

        // 1. Vérifier la connexion Internet
        if (!isNetworkAvailable()) {
            Log.e(TAG, "❌ NO INTERNET CONNECTION");
            Toast.makeText(this, "Pas de connexion Internet", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Initialiser Firebase
        try {
            Log.d(TAG, "Initializing Firebase with custom URL...");

            String databaseUrl = "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app";
            FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);

            Log.d(TAG, "✅ FirebaseDatabase instance created");
            Log.d(TAG, "Database URL: " + databaseUrl);

            reportsRef = database.getReference("reports");
            Log.d(TAG, "✅ Database reference created");
            Log.d(TAG, "Reports path: " + reportsRef.toString());

            storage = FirebaseStorage.getInstance();
            Log.d(TAG, "✅ FirebaseStorage instance created");

            testFirebaseConnectionImmediately();

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR initializing Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();

            try {
                Log.d(TAG, "Trying with default FirebaseDatabase instance...");
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                reportsRef = database.getReference("reports");
                Log.d(TAG, "✅ Default instance created");
            } catch (Exception e2) {
                Log.e(TAG, "❌ Default instance also failed: " + e2.getMessage());
                finish();
                return;
            }
        }

        // 3. Initialiser le système de localisation
        initLocationServices();

        // 4. Initialiser les vues
        initViews();

        Log.d(TAG, "✅ Activity initialization complete");
    }

    private void initLocationServices() {
        Log.d(TAG, "Initializing location services...");

        // Initialiser FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Créer la LocationRequest
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_LOCATION_INTERVAL);

        // Créer le LocationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "📍 Location result is null");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        handleNewLocation(location);
                    }
                }
            }
        };

        // Demander la permission de localisation
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        Log.d(TAG, "Requesting location permission...");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "✅ Location permission already granted");
            startLocationUpdates();
        } else {
            Log.d(TAG, "Requesting location permission from user...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠ Cannot start location updates: permission not granted");
            return;
        }

        Log.d(TAG, "📍 Starting location updates...");

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());

            // Récupérer la dernière localisation connue
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            handleNewLocation(location);
                        } else {
                            Log.w(TAG, "⚠ Last location is null, waiting for updates...");
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e(TAG, "❌ Failed to get last location: " + e.getMessage());
                    });

        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception in startLocationUpdates: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "📍 Stopping location updates");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void handleNewLocation(Location location) {
        if (location == null) return;

        float accuracy = location.getAccuracy();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        Log.d(TAG, "📍 New location received:");
        Log.d(TAG, "   - Latitude: " + latitude);
        Log.d(TAG, "   - Longitude: " + longitude);
        Log.d(TAG, "   - Accuracy: " + accuracy + " meters");
        Log.d(TAG, "   - Provider: " + location.getProvider());

        // Vérifier la précision
        if (accuracy <= MINIMUM_ACCURACY) {
            currentLatitude = latitude;
            currentLongitude = longitude;
            hasValidLocation = true;

            Log.d(TAG, "✅ Location is accurate enough (≤ " + MINIMUM_ACCURACY + "m)");
        } else {
            Log.w(TAG, "⚠ Location accuracy is poor: " + accuracy + "m > " + MINIMUM_ACCURACY + "m");

            // On l'accepte quand même si c'est la seule qu'on a
            if (!hasValidLocation) {
                currentLatitude = latitude;
                currentLongitude = longitude;
                hasValidLocation = true;
                Log.w(TAG, "⚠ Using location despite poor accuracy");
            }
        }
    }

    private void getCurrentLocationForReport(final LocationResultCallback callback) {
        if (isRequestingLocation) {
            Log.w(TAG, "⚠ Already requesting location, ignoring duplicate request");
            return;
        }

        isRequestingLocation = true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠ Location permission not granted");
            isRequestingLocation = false;
            callback.onLocationResult(null, false, "Permission de localisation non accordée");
            return;
        }

        Log.d(TAG, "📍 Getting current location for report...");

        // Essayer d'abord d'obtenir la dernière localisation
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && location.getAccuracy() <= MINIMUM_ACCURACY) {
                        Log.d(TAG, "✅ Using last known location with good accuracy");
                        isRequestingLocation = false;
                        callback.onLocationResult(location, true, null);
                    } else {
                        // Dernière localisation inexistante ou imprécise, demander une nouvelle
                        Log.d(TAG, "⚠ Last location not good enough, requesting fresh location...");
                        requestFreshLocation(callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get last location: " + e.getMessage());
                    requestFreshLocation(callback);
                });
    }

    private void requestFreshLocation(final LocationResultCallback callback) {
        // Créer une LocationRequest pour une seule mise à jour
        LocationRequest oneTimeRequest = LocationRequest.create();
        oneTimeRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        oneTimeRequest.setNumUpdates(1);
        oneTimeRequest.setMaxWaitTime(10000); // 10 secondes max d'attente

        final LocationCallback freshLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    Log.d(TAG, "✅ Fresh location obtained");
                    fusedLocationClient.removeLocationUpdates(this);
                    isRequestingLocation = false;
                    callback.onLocationResult(location, true, null);
                } else {
                    Log.w(TAG, "⚠ Fresh location request returned null");
                    fusedLocationClient.removeLocationUpdates(this);
                    isRequestingLocation = false;
                    callback.onLocationResult(null, false, "Impossible d'obtenir la localisation");
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(oneTimeRequest,
                    freshLocationCallback,
                    Looper.getMainLooper());

            // Timeout après 15 secondes
            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isRequestingLocation) {
                    Log.w(TAG, "⚠ Location request timeout");
                    fusedLocationClient.removeLocationUpdates(freshLocationCallback);
                    isRequestingLocation = false;
                    callback.onLocationResult(null, false, "Timeout de la localisation");
                }
            }, 15000);

        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception in requestFreshLocation: " + e.getMessage());
            isRequestingLocation = false;
            callback.onLocationResult(null, false, "Erreur de sécurité: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission refusée - Vous ne pouvez pas ajouter de photo", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Location permission granted by user");
                startLocationUpdates();
            } else {
                Log.w(TAG, "⚠ Location permission denied by user");
                Toast.makeText(this,
                        "Localisation désactivée - Le signalement utilisera une position par défaut",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void testFirebaseConnectionImmediately() {
        Log.d(TAG, "=== TESTING FIREBASE CONNECTION ===");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "✅ User authenticated: " + user.getUid());
            Log.d(TAG, "   Email: " + user.getEmail());
        } else {
            Log.w(TAG, "⚠ No user authenticated");
        }

        if (reportsRef != null) {
            DatabaseReference testRef = reportsRef.child("connection_test");
            String testValue = "test_" + System.currentTimeMillis();

            Log.d(TAG, "Writing test value: " + testValue);

            testRef.setValue(testValue)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅✅✅ FIREBASE CONNECTION TEST SUCCESSFUL!");
                        Log.d(TAG, "   Test value written: " + testValue);
                        testRef.removeValue();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌❌❌ FIREBASE CONNECTION TEST FAILED");
                        Log.e(TAG, "   Error: " + e.getMessage());
                    });
        }
    }

    private void initViews() {
        Log.d(TAG, "Initializing views...");

        dangerTypeSpinner = findViewById(R.id.dangerTypeSpinner);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        photoImageView = findViewById(R.id.photoImageView);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.danger_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dangerTypeSpinner.setAdapter(adapter);

        takePhotoButton.setText("Choisir une photo");
        takePhotoButton.setOnClickListener(v -> {
            Log.d(TAG, "Photo button clicked");
            checkStoragePermission();
        });

        submitButton.setOnClickListener(v -> {
            Log.d(TAG, "Submit button clicked");
            submitReport();
        });

        Log.d(TAG, "✅ Views initialized successfully");
    }

    private void checkStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    PERMISSION_REQUEST_STORAGE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        try {
            startActivityForResult(Intent.createChooser(intent, "Choisir une photo"), REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open gallery: " + e.getMessage());
            Toast.makeText(this, "Impossible d'ouvrir la galerie", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                Log.d(TAG, "Image selected: " + selectedImageUri);

                try {
                    Glide.with(this)
                            .load(selectedImageUri)
                            .into(photoImageView);
                    Toast.makeText(this, "Photo sélectionnée", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image: " + e.getMessage());
                    photoImageView.setImageURI(selectedImageUri);
                }
            }
        }
    }

    private void submitReport() {
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "=== SUBMIT REPORT STARTED ===");

        if (isSubmitting) {
            Log.d(TAG, "Already submitting, ignoring click");
            return;
        }

        // 1. Validation des données
        String dangerType = dangerTypeSpinner.getSelectedItem().toString();
        String description = descriptionEditText.getText().toString().trim();

        Log.d(TAG, "Form data:");
        Log.d(TAG, "  - Danger type: " + dangerType);
        Log.d(TAG, "  - Description: " + description);

        if (description.isEmpty()) {
            Toast.makeText(this, "Veuillez décrire le danger", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Vérifier l'utilisateur
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user");
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "User: " + currentUser.getUid() + " (" + currentUser.getEmail() + ")");

        // 3. Vérifier la connexion Firebase
        if (reportsRef == null) {
            Log.e(TAG, "CRITICAL: reportsRef is null!");
            Toast.makeText(this, "Erreur Firebase: réinitialisez l'application", Toast.LENGTH_LONG).show();
            return;
        }

        // 4. Démarrer la soumission
        isSubmitting = true;
        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        submitButton.setText("Envoi en cours...");

        // 5. Obtenir la localisation actuelle
        Log.d(TAG, "📍 Requesting current location for report...");
        getCurrentLocationForReport(new LocationResultCallback() {
            @Override
            public void onLocationResult(Location location, boolean success, String errorMessage) {
                double latitude;
                double longitude;
                String locationSource = "";

                if (success && location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    locationSource = "GPS actuel";

                    Log.d(TAG, "✅ Using current GPS location:");
                    Log.d(TAG, "   - Latitude: " + latitude);
                    Log.d(TAG, "   - Longitude: " + longitude);
                    Log.d(TAG, "   - Accuracy: " + location.getAccuracy() + "m");

                } else if (hasValidLocation) {
                    // Utiliser la dernière localisation valide connue
                    latitude = currentLatitude;
                    longitude = currentLongitude;
                    locationSource = "Dernière localisation connue";

                    Log.d(TAG, "⚠ Using last known location:");
                    Log.d(TAG, "   - Latitude: " + latitude);
                    Log.d(TAG, "   - Longitude: " + longitude);

                    if (errorMessage != null) {
                        Log.w(TAG, "   - Error: " + errorMessage);
                    }

                } else {
                    // Utiliser les coordonnées par défaut (Tunis)
                    latitude = 36.8065;
                    longitude = 10.1815;
                    locationSource = "Coordonnées par défaut (Tunis)";

                    Log.w(TAG, "❌ No location available, using default coordinates");
                    Log.w(TAG, "   - Latitude: " + latitude);
                    Log.w(TAG, "   - Longitude: " + longitude);

                    if (errorMessage != null) {
                        Log.w(TAG, "   - Error: " + errorMessage);
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(ReportActivity.this,
                                "Localisation non disponible - Utilisation de coordonnées par défaut",
                                Toast.LENGTH_LONG).show();
                    });
                }

                // 6. Créer l'objet Report
                String userId = currentUser.getUid();

                Report report = new Report(userId, dangerType, description, latitude, longitude);
                report.setStatus("active");
                report.setCreatedAt(new Date());
                report.setLocationSource(locationSource);

                Log.d(TAG, "Report object created:");
                Log.d(TAG, "  - UserId: " + report.getUserId());
                Log.d(TAG, "  - Type: " + report.getDangerType());
                Log.d(TAG, "  - Desc: " + report.getDescription());
                Log.d(TAG, "  - Lat/Lng: " + report.getLatitude() + "/" + report.getLongitude());
                Log.d(TAG, "  - Source: " + report.getLocationSource());

                // 7. Sauvegarder dans Firebase
                Log.d(TAG, "Saving to Firebase Realtime Database...");
                saveReportToDatabase(report, null);
            }
        });
    }

    private void saveReportToDatabase(Report report, String imageUrl) {
        Log.d(TAG, "=== SAVE TO DATABASE ===");

        try {
            if (imageUrl != null) {
                report.setImageUrl(imageUrl);
            }

            String reportId = reportsRef.push().getKey();
            if (reportId == null) {
                Log.e(TAG, "Failed to generate report ID");
                resetSubmitState();
                Toast.makeText(this, "Erreur: impossible de générer l'ID", Toast.LENGTH_SHORT).show();
                return;
            }

            report.setId(reportId);

            Log.d(TAG, "Generated ID: " + reportId);
            Log.d(TAG, "Saving to path: reports/" + reportId);

            reportsRef.child(reportId).setValue(report)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅✅✅✅✅✅ SUCCESS! Report saved to Firebase");
                            Log.d(TAG, "   Path: reports/" + reportId);
                            Log.d(TAG, "   Time: " + new Date().toString());
                            Log.d(TAG, "   Location: " + report.getLatitude() + ", " + report.getLongitude());

                            runOnUiThread(() -> {
                                Toast.makeText(ReportActivity.this,
                                        "✅ Signalement envoyé avec succès!",
                                        Toast.LENGTH_LONG).show();

                                new AlertDialog.Builder(ReportActivity.this)
                                        .setTitle("Succès")
                                        .setMessage("Votre signalement a été enregistré.\n" +
                                                "ID: " + reportId + "\n" +
                                                "Localisation: " + report.getLocationSource())
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            finish();
                                        })
                                        .setCancelable(false)
                                        .show();
                            });

                        } else {
                            Exception e = task.getException();
                            Log.e(TAG, "❌❌❌ FAILED to save report");
                            Log.e(TAG, "   Error: " + (e != null ? e.getMessage() : "Unknown error"));

                            runOnUiThread(() -> {
                                Toast.makeText(ReportActivity.this,
                                        "Erreur: " + (e != null ? e.getMessage() : "Inconnue"),
                                        Toast.LENGTH_LONG).show();
                                resetSubmitState();
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ setValue() failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(ReportActivity.this,
                                    "Échec: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            resetSubmitState();
                        });
                    });

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in saveReportToDatabase: " + e.getMessage(), e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                resetSubmitState();
            });
        }
    }

    private void resetSubmitState() {
        Log.d(TAG, "Resetting submit state");
        runOnUiThread(() -> {
            isSubmitting = false;
            progressBar.setVisibility(View.GONE);
            submitButton.setEnabled(true);
            submitButton.setText("Envoyer le signalement");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity resumed");

        // Redémarrer les mises à jour de localisation si la permission est accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused");
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "=== REPORT ACTIVITY DESTROYED ===");
        Log.d(TAG, "═══════════════════════════════════════");

        stopLocationUpdates();
    }

    // Interface pour le callback de localisation
    private interface LocationResultCallback {
        void onLocationResult(Location location, boolean success, String errorMessage);
    }
}