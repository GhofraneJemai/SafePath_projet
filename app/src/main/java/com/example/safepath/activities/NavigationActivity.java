package com.example.safepath.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safepath.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private MapView mapView;
    private TextView distanceText, durationText, nextInstructionText;
    private Button stopNavigationButton;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private LatLng currentLocation;
    private LatLng destinationLocation;
    private List<LatLng> routePoints;
    private Handler navigationHandler;
    private Runnable navigationRunnable;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // Récupérer les données de l'itinéraire
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            double currentLat = extras.getDouble("current_lat");
            double currentLng = extras.getDouble("current_lng");
            double destLat = extras.getDouble("dest_lat");
            double destLng = extras.getDouble("dest_lng");

            currentLocation = new LatLng(currentLat, currentLng);
            destinationLocation = new LatLng(destLat, destLng);
        }

        initViews();
        setupMap();
        setupLocationUpdates();
        startNavigation();
    }

    private void initViews() {
        mapView = findViewById(R.id.mapView);
        distanceText = findViewById(R.id.distanceText);
        durationText = findViewById(R.id.durationText);
        nextInstructionText = findViewById(R.id.nextInstructionText);
        stopNavigationButton = findViewById(R.id.stopNavigationButton);

        stopNavigationButton.setOnClickListener(v -> stopNavigation());
    }

    private void setupMap() {
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Afficher les points de départ et d'arrivée
        if (currentLocation != null && destinationLocation != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Position actuelle"));

            googleMap.addMarker(new MarkerOptions()
                    .position(destinationLocation)
                    .title("Destination"));

            // Centrer la carte sur l'itinéraire
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

            // Dessiner l'itinéraire simulé
            drawRoute();
        }
    }

    private void drawRoute() {
        if (currentLocation != null && destinationLocation != null) {
            // Générer des points intermédiaires pour la route
            routePoints = generateRoutePoints(currentLocation, destinationLocation);

            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(routePoints)
                    .color(ContextCompat.getColor(this, R.color.primary_color))
                    .width(10);

            googleMap.addPolyline(polylineOptions);
        }
    }

    private List<LatLng> generateRoutePoints(LatLng start, LatLng end) {
        List<LatLng> points = new ArrayList<>();
        points.add(start);

        // Ajouter des points intermédiaires pour une route plus réaliste
        int segments = 5;
        for (int i = 1; i < segments; i++) {
            double lat = start.latitude + (end.latitude - start.latitude) * i / segments;
            double lng = start.longitude + (end.longitude - start.longitude) * i / segments;
            points.add(new LatLng(lat, lng));
        }

        points.add(end);
        return points;
    }

    private void setupLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_UPDATE_INTERVAL / 2);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    updateCurrentLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }
        };
    }

    private void startNavigation() {
        if (checkLocationPermission()) {
            startLocationUpdates();
            startNavigationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateCurrentLocation(LatLng newLocation) {
        currentLocation = newLocation;

        // Mettre à jour la position sur la carte
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(newLocation));
        }

        // Mettre à jour les informations de navigation
        updateNavigationInfo();
    }

    private void updateNavigationInfo() {
        if (currentLocation != null && destinationLocation != null) {
            // Calculer la distance restante
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    destinationLocation.latitude, destinationLocation.longitude,
                    results
            );

            float distanceKm = results[0] / 1000;
            int durationMinutes = (int) (distanceKm * 2); // Estimation

            distanceText.setText(String.format("%.1f km", distanceKm));
            durationText.setText(String.format("%d min", durationMinutes));

            // Mettre à jour les instructions
            updateNavigationInstructions();

            // Vérifier si la destination est atteinte
            if (distanceKm < 0.1) { // 100 mètres
                destinationReached();
            }
        }
    }

    private void updateNavigationInstructions() {
        // Instructions de navigation simulées
        String[] instructions = {
                "Continuez tout droit sur 200m",
                "Tournez à gauche dans 100m",
                "Prenez la deuxième à droite",
                "Destination à droite"
        };

        if (routePoints != null && routePoints.size() > 2) {
            int instructionIndex = (int) (Math.random() * instructions.length);
            nextInstructionText.setText(instructions[instructionIndex]);
        }
    }

    private void startNavigationUpdates() {
        navigationHandler = new Handler();
        navigationRunnable = new Runnable() {
            @Override
            public void run() {
                updateNavigationInfo();
                navigationHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        };
        navigationHandler.postDelayed(navigationRunnable, LOCATION_UPDATE_INTERVAL);
    }

    private void destinationReached() {
        Toast.makeText(this, "Destination atteinte !", Toast.LENGTH_LONG).show();
        stopNavigation();
    }

    private void stopNavigation() {
        if (navigationHandler != null && navigationRunnable != null) {
            navigationHandler.removeCallbacks(navigationRunnable);
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        finish();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Permission de localisation requise pour la navigation", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Gestion du cycle de vie MapView
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNavigation();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}