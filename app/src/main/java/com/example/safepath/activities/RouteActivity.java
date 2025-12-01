package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.safepath.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap googleMap;
    private MapView mapView;
    private EditText destinationEditText;
    private Button calculateRouteButton, startNavigationButton, searchButton;
    private TextView distanceText, durationText, safetyLevelText, selectedLocationText;
    private ProgressBar routeProgressBar;
    private LinearLayout routeInfoLayout;

    private LatLng currentLocation, destinationLocation;
    private Polyline currentRoute;
    private Marker destinationMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private OkHttpClient httpClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // API KEY CORRECTEMENT CONFIGURÉE POUR DIRECTIONS API
    private static final String MAPS_API_KEY = "AIzaSyCWAeD7DS4l3_2Q15dQ4B3cDq0eLw9n8Jk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpClient = new OkHttpClient();

        initViews();
        setupMap(savedInstanceState);
        getCurrentLocation();
    }

    private void initViews() {
        destinationEditText = findViewById(R.id.destinationEditText);
        calculateRouteButton = findViewById(R.id.calculateRouteButton);
        startNavigationButton = findViewById(R.id.startNavigationButton);
        searchButton = findViewById(R.id.searchButton);
        distanceText = findViewById(R.id.distanceText);
        durationText = findViewById(R.id.durationText);
        safetyLevelText = findViewById(R.id.safetyLevelText);
        selectedLocationText = findViewById(R.id.selectedLocationText);
        routeProgressBar = findViewById(R.id.routeProgressBar);
        mapView = findViewById(R.id.mapView);
        routeInfoLayout = findViewById(R.id.routeInfoLayout);

        calculateRouteButton.setOnClickListener(v -> calculateSafeRoute());
        startNavigationButton.setOnClickListener(v -> startNavigation());
        searchButton.setOnClickListener(v -> searchLocation());

        // Désactiver le bouton au début
        calculateRouteButton.setEnabled(false);
        startNavigationButton.setVisibility(View.GONE);
        routeInfoLayout.setVisibility(View.GONE);
    }

    private void setupMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Activer le clic sur la carte
        googleMap.setOnMapClickListener(this);

        // Activer la couche de localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e("ROUTE", "SecurityException: " + e.getMessage());
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        // Quand l'utilisateur clique sur la carte
        setDestinationFromMap(latLng);
    }

    private void setDestinationFromMap(LatLng latLng) {
        destinationLocation = latLng;

        // Supprimer l'ancien marqueur
        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        // Ajouter un nouveau marqueur
        destinationMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Destination")
                .snippet("Cliquez pour calculer l'itinéraire"));

        // Centrer la carte sur la destination
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        // Récupérer l'adresse
        getAddressFromLocation(latLng);

        // Activer le bouton de calcul
        calculateRouteButton.setEnabled(true);
        selectedLocationText.setText("Destination sélectionnée sur la carte");

        // Cacher les infos d'itinéraire précédent
        routeInfoLayout.setVisibility(View.GONE);
        startNavigationButton.setVisibility(View.GONE);
    }

    private void getAddressFromLocation(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressLine = address.getAddressLine(0);
                destinationEditText.setText(addressLine);
                selectedLocationText.setText("Destination: " + addressLine);
            }
        } catch (IOException e) {
            Log.e("ROUTE", "Erreur géocodage: " + e.getMessage());
            selectedLocationText.setText("Destination: Lat " + latLng.latitude + ", Lng " + latLng.longitude);
        }
    }

    private void searchLocation() {
        String destination = destinationEditText.getText().toString().trim();
        if (destination.isEmpty()) {
            Toast.makeText(this, "Entrez une adresse", Toast.LENGTH_SHORT).show();
            return;
        }

        routeProgressBar.setVisibility(View.VISIBLE);

        // Géocodage de la destination
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(destination, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                setDestinationFromMap(latLng);
            } else {
                Toast.makeText(this, "Adresse non trouvée", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Erreur de recherche: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ROUTE", "Erreur recherche: " + e.getMessage());
        } finally {
            routeProgressBar.setVisibility(View.GONE);
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            if (googleMap != null) {
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            }
                        } else {
                            // Si la localisation est null, utiliser une position par défaut
                            currentLocation = new LatLng(48.8566, 2.3522); // Paris
                            Toast.makeText(this, "Localisation non disponible, utilisation position par défaut", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ROUTE", "Erreur localisation: " + e.getMessage());
                        currentLocation = new LatLng(48.8566, 2.3522); // Paris par défaut
                    });
        } else {
            // Demander la permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            currentLocation = new LatLng(48.8566, 2.3522); // Paris par défaut
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.e("ROUTE", "SecurityException: " + e.getMessage());
                    }
                }
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void calculateSafeRoute() {
        if (currentLocation == null || destinationLocation == null) {
            Toast.makeText(this, "Sélectionnez une destination d'abord", Toast.LENGTH_SHORT).show();
            return;
        }

        routeProgressBar.setVisibility(View.VISIBLE);
        calculateRouteButton.setEnabled(false);
        routeInfoLayout.setVisibility(View.GONE);
        startNavigationButton.setVisibility(View.GONE);

        // Utiliser la vraie API Google Directions
        calculateRealRoute();
    }

    private void calculateRealRoute() {
        String origin = currentLocation.latitude + "," + currentLocation.longitude;
        String destination = destinationLocation.latitude + "," + destinationLocation.longitude;

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin +
                "&destination=" + destination +
                "&mode=driving" +
                "&key=" + MAPS_API_KEY;

        Log.d("ROUTE", "=== API REQUEST ===");
        Log.d("ROUTE", "URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e("ROUTE", "API Network Error: " + e.getMessage());
                    Toast.makeText(RouteActivity.this, "Erreur réseau, utilisation du mode simulation", Toast.LENGTH_SHORT).show();
                    calculateRouteWithSafety(); // Fallback
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("ROUTE", "HTTP Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Log.d("ROUTE", "API Response received, length: " + jsonData.length());
                    processDirectionsResponse(jsonData);
                } else {
                    runOnUiThread(() -> {
                        Log.e("ROUTE", "HTTP error: " + response.code() + " - " + response.message());
                        Toast.makeText(RouteActivity.this, "Erreur serveur, utilisation du mode simulation", Toast.LENGTH_SHORT).show();
                        calculateRouteWithSafety(); // Fallback
                    });
                }
            }
        });
    }

    private void processDirectionsResponse(String jsonResponse) {
        runOnUiThread(() -> {
            Log.d("ROUTE", "=== PROCESSING API RESPONSE ===");

            try {
                JSONObject json = new JSONObject(jsonResponse);
                String status = json.getString("status");
                Log.d("ROUTE", "API Status: " + status);

                if ("OK".equals(status)) {
                    JSONArray routes = json.getJSONArray("routes");
                    Log.d("ROUTE", "Number of routes found: " + routes.length());

                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);

                        // Vérifier s'il y a un polyline
                        if (route.has("overview_polyline")) {
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String encodedPath = overviewPolyline.getString("points");
                            Log.d("ROUTE", "Polyline encoded, length: " + encodedPath.length());

                            displayRealRouteFromJSON(route);
                            return;
                        }
                    }
                }

                // Si on arrive ici, c'est qu'il y a une erreur
                String errorMessage = json.optString("error_message", "Unknown error");
                Log.e("ROUTE", "API Error - Status: " + status + ", Message: " + errorMessage);

                // Message utilisateur plus clair
                String userMessage = "Impossible de calculer l'itinéraire. ";
                if ("ZERO_RESULTS".equals(status)) {
                    userMessage += "Aucun itinéraire trouvé entre ces points.";
                } else if ("OVER_QUERY_LIMIT".equals(status)) {
                    userMessage += "Limite d'API atteinte. Réessayez plus tard.";
                } else {
                    userMessage += "Erreur: " + status;
                }

                Toast.makeText(RouteActivity.this, userMessage, Toast.LENGTH_LONG).show();
                calculateRouteWithSafety(); // Fallback vers la simulation

            } catch (JSONException e) {
                Log.e("ROUTE", "JSON parsing error: " + e.getMessage());
                Toast.makeText(RouteActivity.this, "Erreur traitement des données", Toast.LENGTH_SHORT).show();
                calculateRouteWithSafety(); // Fallback
            }
        });
    }

    private void displayRealRouteFromJSON(JSONObject route) {
        try {
            // Effacer l'ancien itinéraire
            if (currentRoute != null) {
                currentRoute.remove();
            }

            List<LatLng> points = new ArrayList<>();

            // Récupérer le polyline overview
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String encodedPath = overviewPolyline.getString("points");

            // Décoder le polyline
            points = decodePolyline(encodedPath);
            Log.d("ROUTE", "Decoded points: " + points.size());

            if (points.isEmpty()) {
                throw new JSONException("Empty polyline after decoding");
            }

            // Dessiner la ligne sur la carte
            currentRoute = googleMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(ContextCompat.getColor(this, R.color.safe_route_color))
                    .width(12));

            // Ajuster la caméra pour montrer tout l'itinéraire
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

            // Mettre à jour les informations
            updateRealRouteInfoFromJSON(route);

            Toast.makeText(this, "Itinéraire Google Maps calculé!", Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            Log.e("ROUTE", "Error displaying route: " + e.getMessage());
            calculateRouteWithSafety(); // Fallback
        }
    }

    // Méthode pour décoder le polyline Google
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void updateRealRouteInfoFromJSON(JSONObject route) {
        try {
            JSONArray legs = route.getJSONArray("legs");
            if (legs.length() > 0) {
                JSONObject leg = legs.getJSONObject(0);

                // Distance
                JSONObject distance = leg.getJSONObject("distance");
                String distanceTextValue = distance.getString("text");

                // Durée
                JSONObject duration = leg.getJSONObject("duration");
                String durationTextValue = duration.getString("text");

                // Mettre à jour l'UI
                distanceText.setText(distanceTextValue);
                durationText.setText(durationTextValue);

                // Calculer le score de sécurité
                int safetyScore = calculateSafetyScoreFromRealRoute(leg);
                safetyLevelText.setText(getSafetyLevelText(safetyScore));
                safetyLevelText.setTextColor(getSafetyColor(safetyScore));

                // Afficher les infos
                routeInfoLayout.setVisibility(View.VISIBLE);

                Log.d("ROUTE", "Route info: " + distanceTextValue + " en " + durationTextValue);

            }
        } catch (JSONException e) {
            Log.e("ROUTE", "Error updating route info: " + e.getMessage());
            updateRouteInfo(); // Utiliser les valeurs par défaut
        } finally {
            routeProgressBar.setVisibility(View.GONE);
            startNavigationButton.setVisibility(View.VISIBLE);
            calculateRouteButton.setEnabled(true);
        }
    }

    private int calculateSafetyScoreFromRealRoute(JSONObject leg) {
        try {
            int baseScore = 80;

            // Analyser les steps pour détecter les zones potentiellement dangereuses
            if (leg.has("steps")) {
                JSONArray steps = leg.getJSONArray("steps");
                Log.d("ROUTE", "Number of steps in route: " + steps.length());

                // Réduire le score basé sur certains facteurs
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    String htmlInstructions = step.optString("html_instructions", "").toLowerCase();

                    // Détecter les éléments potentiellement dangereux
                    if (htmlInstructions.contains("tunnel") ||
                            htmlInstructions.contains("dark") ||
                            htmlInstructions.contains("alley") ||
                            htmlInstructions.contains("isolated")) {
                        baseScore -= 5;
                        Log.d("ROUTE", "Dangerous area detected: " + htmlInstructions);
                    }
                }
            }

            return Math.max(50, baseScore); // Minimum 50

        } catch (JSONException e) {
            Log.e("ROUTE", "Error calculating safety score: " + e.getMessage());
            return 75; // Score par défaut en cas d'erreur
        }
    }

    private String getSafetyLevelText(int score) {
        if (score >= 80) return "Très Sûr";
        if (score >= 70) return "Sûr";
        if (score >= 60) return "Modéré";
        return "Risqué";
    }

    private int getSafetyColor(int score) {
        if (score >= 80) return ContextCompat.getColor(this, R.color.safe_color);
        if (score >= 70) return ContextCompat.getColor(this, R.color.warning_color);
        return ContextCompat.getColor(this, R.color.danger_color);
    }

    // Fallback method (simulation améliorée)
    private void calculateRouteWithSafety() {
        Log.d("ROUTE", "Using fallback simulation mode");

        // Effacer l'ancien itinéraire
        if (currentRoute != null) {
            currentRoute.remove();
        }

        // Générer un itinéraire simulé INTELLIGENT
        List<LatLng> routePoints = generateSmartRouteSimulation();

        // Dessiner la ligne sur la carte
        currentRoute = googleMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .color(ContextCompat.getColor(this, R.color.safe_route_color))
                .width(12));

        // Ajuster la caméra pour montrer tout l'itinéraire
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        // Mettre à jour les informations
        updateRouteInfo();

        routeProgressBar.setVisibility(View.GONE);
        startNavigationButton.setVisibility(View.VISIBLE);
        calculateRouteButton.setEnabled(true);
        routeInfoLayout.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Itinéraire sécurisé calculé (mode simulation)", Toast.LENGTH_SHORT).show();
    }

    private List<LatLng> generateSmartRouteSimulation() {
        List<LatLng> points = new ArrayList<>();
        points.add(currentLocation);

        double lat1 = currentLocation.latitude;
        double lng1 = currentLocation.longitude;
        double lat2 = destinationLocation.latitude;
        double lng2 = destinationLocation.longitude;

        // SIMULATION D'UN VRAI ITINÉRAIRE ROUTIER AVEC POINTS INTERMÉDIAIRES
        // Point intermédiaire 1 (25% du trajet)
        points.add(new LatLng(
                lat1 + (lat2 - lat1) * 0.25 + 0.0002,
                lng1 + (lng2 - lng1) * 0.25 - 0.00015
        ));

        // Point intermédiaire 2 (50% du trajet) - virage principal
        points.add(new LatLng(
                lat1 + (lat2 - lat1) * 0.5 - 0.0001,
                lng1 + (lng2 - lng1) * 0.5 + 0.00025
        ));

        // Point intermédiaire 3 (75% du trajet)
        points.add(new LatLng(
                lat1 + (lat2 - lat1) * 0.75 + 0.00015,
                lng1 + (lng2 - lng1) * 0.75 - 0.0002
        ));

        points.add(destinationLocation);

        Log.d("ROUTE", "Generated simulation route with " + points.size() + " points");
        return points;
    }

    private void updateRouteInfo() {
        if (currentLocation == null || destinationLocation == null) return;

        // Calculer la distance réelle
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                destinationLocation.latitude, destinationLocation.longitude,
                results
        );

        float distanceKm = results[0] / 1000;
        int durationMinutes = (int) (distanceKm * 2.5); // Estimation 40km/h

        // Calculer le score de sécurité
        int safetyScore = calculateSafetyScore(distanceKm);

        distanceText.setText(String.format("%.1f km", distanceKm));
        durationText.setText(String.format("%d min", durationMinutes));

        safetyLevelText.setText(getSafetyLevelText(safetyScore));
        safetyLevelText.setTextColor(getSafetyColor(safetyScore));

        routeInfoLayout.setVisibility(View.VISIBLE);
    }

    private int calculateSafetyScore(float distance) {
        int baseScore = 85;
        if (distance > 10) baseScore -= 20;
        else if (distance > 5) baseScore -= 10;
        return Math.max(50, baseScore);
    }

    private void startNavigation() {
        if (currentLocation == null || destinationLocation == null) {
            Toast.makeText(this, "Calculez un itinéraire d'abord", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("current_lat", currentLocation.latitude);
        intent.putExtra("current_lng", currentLocation.longitude);
        intent.putExtra("dest_lat", destinationLocation.latitude);
        intent.putExtra("dest_lng", destinationLocation.longitude);
        startActivity(intent);
    }

    // Gestion du lifecycle MapView
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
}