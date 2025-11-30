package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.safepath.R;
import com.example.safepath.models.Report;
import com.example.safepath.models.SafeZone;
import com.example.safepath.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView profileImage, notificationIcon;
    private FloatingActionButton sosFab, reportFab, routeFab;
    private boolean fabExpanded = false;

    private DatabaseReference reportsRef, safeZonesRef, usersRef;

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

        // Références Firebase
        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        safeZonesRef = FirebaseDatabase.getInstance().getReference("safe_zones");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

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

        reportFab.setVisibility(FloatingActionButton.GONE);
        routeFab.setVisibility(FloatingActionButton.GONE);

        profileImage.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        notificationIcon.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        sosFab.setOnClickListener(v -> {
            if (!fabExpanded) expandFABs();
            else collapseFABs();
        });

        reportFab.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        routeFab.setOnClickListener(v -> startActivity(new Intent(this, RouteActivity.class)));
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
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        getCurrentLocation();
        loadDangerZones();
        loadSafeZones();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        }
                    });
        }
    }

    private void loadDangerZones() {
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                googleMap.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Report report = data.getValue(Report.class);
                    if (report != null) {
                        report.setId(data.getKey());
                        addDangerMarker(report);
                    }
                }
                loadSafeZones();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur chargement signalements", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSafeZones() {
        safeZonesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    SafeZone safeZone = data.getValue(SafeZone.class);
                    if (safeZone != null) addSafeZoneMarker(safeZone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur chargement zones sûres", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addDangerMarker(Report report) {
        LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());
        googleMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(report.getDangerType())
                .snippet(report.getDescription())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void addSafeZoneMarker(SafeZone safeZone) {
        LatLng pos = new LatLng(safeZone.getLatitude(), safeZone.getLongitude());
        googleMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(safeZone.getName())
                .snippet(safeZone.getType())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
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
                    Glide.with(MainActivity.this).load(user.getProfileImage()).into(profileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Erreur chargement profil", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDestroy();
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
