package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.safepath.R;
import com.example.safepath.models.Report;
import com.example.safepath.models.SafeZone;
import com.example.safepath.models.User;
import com.example.safepath.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_LOCATION = 1001;

    private GoogleMap googleMap;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView profileImage, notificationIcon, logoutButton;
    private FloatingActionButton sosFab, reportFab, routeFab;
    private boolean fabExpanded = false;
    private TextView locationStatusText;

    private DatabaseReference reportsRef, safeZonesRef, usersRef;

    // Pour stocker les listeners Firebase
    private ValueEventListener reportsListener;
    private ValueEventListener safeZonesListener;

    // Pour stocker les markers avec leurs IDs
    private Map<String, Marker> reportMarkers = new HashMap<>();
    private Map<String, Circle> reportCircles = new HashMap<>();
    private Map<String, Marker> safeZoneMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Initialiser Firebase avec le Singleton
        try {
            FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
            reportsRef = firebaseHelper.getReportsRef();
            safeZonesRef = firebaseHelper.getSafeZonesRef();
            usersRef = firebaseHelper.getUsersRef();

            Log.d(TAG, "✅ Firebase initialized with singleton");
            Log.d(TAG, "Database URL: " + FirebaseHelper.DATABASE_URL);
            Log.d(TAG, "Reports ref: " + reportsRef.toString());

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR initializing Firebase: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();
        setupBottomNavigation();
        loadUserData();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        notificationIcon = findViewById(R.id.notificationIcon);
        sosFab = findViewById(R.id.sosFab);
        reportFab = findViewById(R.id.reportFab);
        routeFab = findViewById(R.id.routeFab);
        mapView = findViewById(R.id.mapView);
        logoutButton = findViewById(R.id.logoutButton);

        reportFab.setVisibility(FloatingActionButton.GONE);
        routeFab.setVisibility(FloatingActionButton.GONE);

        profileImage.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        notificationIcon.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        // Configuration du bouton de déconnexion
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());

        sosFab.setOnClickListener(v -> {
            if (!fabExpanded) expandFABs();
            else collapseFABs();
        });

        reportFab.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        routeFab.setOnClickListener(v -> startActivity(new Intent(this, RouteActivity.class)));
    }

    private void showLogoutConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setPositiveButton("Déconnecter", (dialog, which) -> logoutUser())
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logoutUser() {
        try {
            // Arrêter les listeners Firebase pour éviter les fuites de mémoire
            if (reportsRef != null && reportsListener != null) {
                reportsRef.removeEventListener(reportsListener);
            }
            if (safeZonesRef != null && safeZonesListener != null) {
                safeZonesRef.removeEventListener(safeZonesListener);
            }

            // Déconnecter de Firebase Auth
            FirebaseAuth.getInstance().signOut();

            Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();

            // Rediriger vers LoginActivity et effacer l'historique de navigation
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la déconnexion: " + e.getMessage());
            Toast.makeText(this, "Erreur de déconnexion", Toast.LENGTH_SHORT).show();
        }
    }

    private void expandFABs() {
        fabExpanded = true;
        reportFab.show();
        routeFab.show();
        sosFab.setImageResource(R.drawable.ic_close);
    }

    private void collapseFABs() {
        fabExpanded = false;
        reportFab.hide();
        routeFab.hide();
        sosFab.setImageResource(R.drawable.ic_sos);
    }

    private void setupMap() {
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Configurer la carte
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Définir le listener pour les markers
        googleMap.setOnMarkerClickListener(this);

        // Vérifier et configurer les permissions de localisation
        setupLocationPermissions();

        // Charger les données
        loadReports();
        loadSafeZones();

        Log.d(TAG, "✅ Map is ready");
    }

    private void setupLocationPermissions() {
        // Vérifier la permission de localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission accordée
            enableLocationFeatures();
        } else {
            // Permission non accordée, demander à l'utilisateur
            requestLocationPermission();
        }
    }

    private void enableLocationFeatures() {
        try {
            // Activer le bouton "Ma position" sur la carte
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }

            // Centrer sur la position de l'utilisateur
            getCurrentLocation();

            if (locationStatusText != null) {
                locationStatusText.setText("Localisation activée");
                locationStatusText.setVisibility(View.VISIBLE);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception in enableLocationFeatures: " + e.getMessage());
            showLocationPermissionError();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Expliquer à l'utilisateur pourquoi nous avons besoin de la permission
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permission de localisation")
                    .setMessage("Cette application a besoin de votre localisation pour " +
                            "afficher votre position sur la carte et les dangers à proximité.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Demander la permission après l'explication
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_REQUEST_LOCATION);
                    })
                    .setNegativeButton("Annuler", (dialog, which) -> {
                        // Utilisateur a refusé, centrer sur Tunis
                        centerOnDefaultLocation();
                    })
                    .show();
        } else {
            // Demander directement la permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                        getCurrentLocation();
                    } catch (SecurityException e) {
                        Log.e(TAG, "❌ Security exception after permission granted: " + e.getMessage());
                    }
                }
                if (locationStatusText != null) {
                    locationStatusText.setText("Localisation activée");
                    locationStatusText.setVisibility(View.VISIBLE);
                }
                Toast.makeText(this, "Localisation activée", Toast.LENGTH_SHORT).show();
            } else {
                // Permission refusée
                showLocationPermissionError();
            }
        }
    }

    private void getCurrentLocation() {
        // Vérifier explicitement la permission avant d'appeler getLastLocation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠ Location permission not granted, cannot get current location");
            showLocationPermissionError();
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));

                            if (locationStatusText != null) {
                                locationStatusText.setText("Position: " +
                                        String.format(Locale.getDefault(), "%.4f, %.4f",
                                                location.getLatitude(), location.getLongitude()));
                                locationStatusText.setVisibility(View.VISIBLE);
                            }

                            Log.d(TAG, "📍 Current location: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Log.w(TAG, "⚠ Last location is null");
                            centerOnDefaultLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Error getting location: " + e.getMessage());
                        centerOnDefaultLocation();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception in getCurrentLocation: " + e.getMessage());
            centerOnDefaultLocation();
        }
    }

    private void centerOnDefaultLocation() {
        // Centrer sur Tunis par défaut
        LatLng tunis = new LatLng(36.8065, 10.1815);
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tunis, 12));
        }

        if (locationStatusText != null) {
            locationStatusText.setText("Position par défaut (Tunis)");
            locationStatusText.setVisibility(View.VISIBLE);
        }
    }

    private void showLocationPermissionError() {
        centerOnDefaultLocation();

        if (locationStatusText != null) {
            locationStatusText.setText("Localisation désactivée - Utilisez le bouton de localisation");
            locationStatusText.setVisibility(View.VISIBLE);
        }

        Toast.makeText(this,
                "La localisation est nécessaire pour de meilleures fonctionnalités. " +
                        "Vous pouvez l'activer dans les paramètres.",
                Toast.LENGTH_LONG).show();
    }

    private void loadReports() {
        Log.d(TAG, "=== LOADING REPORTS ===");

        reportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Reports data changed, count: " + snapshot.getChildrenCount());

                // Garder les safe zones
                for (Map.Entry<String, Marker> entry : safeZoneMarkers.entrySet()) {
                    entry.getValue().remove();
                }
                safeZoneMarkers.clear();

                // Supprimer tous les anciens markers et cercles de reports
                for (Map.Entry<String, Marker> entry : reportMarkers.entrySet()) {
                    entry.getValue().remove();
                }
                reportMarkers.clear();

                for (Map.Entry<String, Circle> entry : reportCircles.entrySet()) {
                    entry.getValue().remove();
                }
                reportCircles.clear();

                // Ajouter les nouveaux reports
                for (DataSnapshot data : snapshot.getChildren()) {
                    Report report = data.getValue(Report.class);
                    if (report != null) {
                        report.setId(data.getKey()); // S'assurer que l'ID est défini
                        addReportMarker(report);
                        Log.d(TAG, "Added report: " + report.getId() + " at " +
                                report.getLatitude() + ", " + report.getLongitude());
                    }
                }

                // Recharger les safe zones
                loadSafeZones();

                // Afficher le nombre de signalements
                if (locationStatusText != null) {
                    locationStatusText.setText(snapshot.getChildrenCount() + " signalements actifs");
                    locationStatusText.setVisibility(View.VISIBLE);
                }

                Toast.makeText(MainActivity.this,
                        snapshot.getChildrenCount() + " signalements chargés",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error loading reports: " + error.getMessage());
                Toast.makeText(MainActivity.this,
                        "Erreur chargement signalements: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        reportsRef.addValueEventListener(reportsListener);
    }

    private void loadSafeZones() {
        safeZonesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Safe zones data changed, count: " + snapshot.getChildrenCount());

                for (DataSnapshot data : snapshot.getChildren()) {
                    SafeZone safeZone = data.getValue(SafeZone.class);
                    if (safeZone != null) {
                        safeZone.setId(data.getKey());
                        addSafeZoneMarker(safeZone);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error loading safe zones: " + error.getMessage());
            }
        };

        safeZonesRef.addValueEventListener(safeZonesListener);
    }

    private void addReportMarker(Report report) {
        if (googleMap == null || report == null) return;

        LatLng position = new LatLng(report.getLatitude(), report.getLongitude());

        // Créer un marker avec une icône rouge
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(report.getDangerType())
                .snippet(formatReportSnippet(report))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        Marker marker = googleMap.addMarker(markerOptions);
        if (marker != null && report.getId() != null) {
            reportMarkers.put(report.getId(), marker);
            marker.setTag(report); // Stocker l'objet Report dans le tag

            // Ajouter un cercle rouge autour du marker
            addReportCircle(report, position);
        }
    }

    private void addReportCircle(Report report, LatLng position) {
        // Ajouter un cercle rouge semi-transparent
        CircleOptions circleOptions = new CircleOptions()
                .center(position)
                .radius(50) // 50 mètres de rayon
                .strokeColor(Color.RED)
                .strokeWidth(2)
                .fillColor(Color.argb(70, 255, 0, 0)); // Rouge semi-transparent

        Circle circle = googleMap.addCircle(circleOptions);
        if (circle != null && report.getId() != null) {
            reportCircles.put(report.getId(), circle);
        }
    }

    private void addSafeZoneMarker(SafeZone safeZone) {
        if (googleMap == null || safeZone == null) return;

        LatLng position = new LatLng(safeZone.getLatitude(), safeZone.getLongitude());

        // Créer un marker avec une icône verte
        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(safeZone.getName())
                .snippet(safeZone.getType())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        Marker marker = googleMap.addMarker(markerOptions);
        if (marker != null && safeZone.getId() != null) {
            safeZoneMarkers.put(safeZone.getId(), marker);
            marker.setTag(safeZone); // Stocker l'objet SafeZone dans le tag
        }
    }

    private String formatReportSnippet(Report report) {
        StringBuilder snippet = new StringBuilder();

        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            snippet.append(report.getDescription());
        }

        // Ajouter la date si disponible
        if (report.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            snippet.append("\nDate: ").append(sdf.format(report.getCreatedAt()));
        }

        // Ajouter le statut
        if (report.getStatus() != null) {
            snippet.append("\nStatut: ").append(report.getStatus());
        }

        return snippet.toString();
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();

        if (tag instanceof Report) {
            Report report = (Report) tag;

            // Afficher une info-bulle personnalisée
            marker.showInfoWindow();

            // Vous pourriez aussi ouvrir un dialogue avec plus d'informations
            showReportDetails(report);

            return true; // Nous avons géré le clic
        } else if (tag instanceof SafeZone) {
            SafeZone safeZone = (SafeZone) tag;
            marker.showInfoWindow();
            return true;
        }

        return false;
    }

    private void showReportDetails(Report report) {
        // Créer un dialogue avec les détails du report
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Détails du signalement");

        StringBuilder message = new StringBuilder();
        message.append("Type: ").append(report.getDangerType()).append("\n\n");

        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            message.append("Description: ").append(report.getDescription()).append("\n\n");
        }

        if (report.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            message.append("Date: ").append(sdf.format(report.getCreatedAt())).append("\n\n");
        }

        message.append("Position: ")
                .append(String.format(Locale.getDefault(), "%.6f", report.getLatitude()))
                .append(", ")
                .append(String.format(Locale.getDefault(), "%.6f", report.getLongitude()));

        if (report.getLocationSource() != null) {
            message.append("\nSource: ").append(report.getLocationSource());
        }

        if (report.getStatus() != null) {
            message.append("\nStatut: ").append(report.getStatus());
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        // Option pour signaler comme résolu (si vous êtes l'auteur)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(report.getUserId())) {
            builder.setNegativeButton("Marquer comme résolu", (dialog, which) -> {
                markReportAsResolved(report.getId());
            });
        }

        builder.show();
    }

    private void markReportAsResolved(String reportId) {
        if (reportId == null) return;

        DatabaseReference reportRef = reportsRef.child(reportId);
        reportRef.child("status").setValue("resolved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Signalement marqué comme résolu", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Déjà sur la page d'accueil
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_tips) {
                startActivity(new Intent(this, TipsActivity.class));
                return true;
            } else if (id == R.id.nav_contacts) {
                startActivity(new Intent(this, ContactsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getProfileImage() != null) {
                    Glide.with(MainActivity.this)
                            .load(user.getProfileImage())
                            .placeholder(R.drawable.ic_profile)
                            .into(profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user data: " + error.getMessage());
            }
        });
    }

    // Méthodes du cycle de vie pour MapView
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();

        // Nettoyer les listeners Firebase
        if (reportsRef != null && reportsListener != null) {
            reportsRef.removeEventListener(reportsListener);
        }
        if (safeZonesRef != null && safeZonesListener != null) {
            safeZonesRef.removeEventListener(safeZonesListener);
        }

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();

        // Recharger les données quand on revient à l'activité
        if (googleMap != null) {
            loadReports();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }
}