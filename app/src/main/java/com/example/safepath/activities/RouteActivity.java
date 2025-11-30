package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.example.safepath.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private MapView mapView;
    private EditText destinationEditText;
    private Button calculateRouteButton, startNavigationButton;
    private TextView distanceText, durationText, safetyLevelText;
    private ProgressBar routeProgressBar;

    private LatLng currentLocation, destinationLocation;
    private Polyline currentRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        initViews();
        setupMap();
        getCurrentLocation();
    }

    private void initViews() {
        destinationEditText = findViewById(R.id.destinationEditText);
        calculateRouteButton = findViewById(R.id.calculateRouteButton);
        startNavigationButton = findViewById(R.id.startNavigationButton);
        distanceText = findViewById(R.id.distanceText);
        durationText = findViewById(R.id.durationText);
        safetyLevelText = findViewById(R.id.safetyLevelText);
        routeProgressBar = findViewById(R.id.routeProgressBar);
        mapView = findViewById(R.id.mapView);

        calculateRouteButton.setOnClickListener(v -> calculateSafeRoute());
        startNavigationButton.setOnClickListener(v -> startNavigation());
    }

    private void setupMap() {
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void getCurrentLocation() {
        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        }
                    });
        }
    }

    private void calculateSafeRoute() {
        String destination = destinationEditText.getText().toString().trim();
        if (destination.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer une destination", Toast.LENGTH_SHORT).show();
            return;
        }

        routeProgressBar.setVisibility(View.VISIBLE);

        // Géocodage de la destination
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(destination, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                destinationLocation = new LatLng(address.getLatitude(), address.getLongitude());

                // Calculer l'itinéraire sécurisé
                calculateRouteWithSafety();
            } else {
                Toast.makeText(this, "Destination non trouvée", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Erreur de géocodage", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateRouteWithSafety() {
        // Effacer l'ancien itinéraire
        if (currentRoute != null) {
            currentRoute.remove();
        }

        // Simuler le calcul d'itinéraire sécurisé
        // En production, utiliser l'API Directions de Google Maps
        List<LatLng> routePoints = generateSafeRoute();

        currentRoute = googleMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .color(ContextCompat.getColor(this, R.color.safe_color))
                .width(8));

        // Ajuster la caméra pour montrer tout l'itinéraire
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(currentLocation);
        builder.include(destinationLocation);
        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        // Mettre à jour les informations
        updateRouteInfo();
        routeProgressBar.setVisibility(View.GONE);
        startNavigationButton.setVisibility(View.VISIBLE);
    }

    private List<LatLng> generateSafeRoute() {
        // Simulation d'un itinéraire sécurisé
        List<LatLng> points = new ArrayList<>();
        points.add(currentLocation);

        // Points intermédiaires (éviter les zones dangereuses)
        points.add(new LatLng((currentLocation.latitude + destinationLocation.latitude) / 2,
                (currentLocation.longitude + destinationLocation.longitude) / 2));

        points.add(destinationLocation);
        return points;
    }

    private void updateRouteInfo() {
        // Calculer distance et durée (simulation)
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                destinationLocation.latitude, destinationLocation.longitude,
                results);

        float distanceKm = results[0] / 1000;
        int durationMinutes = (int) (distanceKm * 2); // Estimation

        distanceText.setText(String.format("%.1f km", distanceKm));
        durationText.setText(String.format("%d min", durationMinutes));
        safetyLevelText.setText("Élevé");
        safetyLevelText.setTextColor(ContextCompat.getColor(this, R.color.safe_color));
    }

    private void startNavigation() {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("current_lat", currentLocation.latitude);
        intent.putExtra("current_lng", currentLocation.longitude);
        intent.putExtra("dest_lat", destinationLocation.latitude);
        intent.putExtra("dest_lng", destinationLocation.longitude);
        startActivity(intent);
    }
}