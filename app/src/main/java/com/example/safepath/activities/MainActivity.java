package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.safepath.R;
import com.example.safepath.models.EmergencyContact;
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
    private FloatingActionButton sosFab, reportFab, routeFab, sos;
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
        sos = findViewById(R.id.sos);

        reportFab.setVisibility(View.GONE);
        routeFab.setVisibility(View.GONE);
        sos.setVisibility(View.GONE);

        profileImage.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        notificationIcon.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        sosFab.setOnClickListener(v -> {
            if (!fabExpanded) expandFABs();
            else collapseFABs();
        });

        reportFab.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        routeFab.setOnClickListener(v -> startActivity(new Intent(this, RouteActivity.class)));
        sos.setOnClickListener(v -> showSOSDialog());
    }

    private void expandFABs() {
        fabExpanded = true;
        reportFab.show();
        routeFab.show();
        sos.show();
        sosFab.setImageResource(R.drawable.ic_close);
    }

    private void collapseFABs() {
        fabExpanded = false;
        reportFab.hide();
        routeFab.hide();
        sos.hide();
        sosFab.setImageResource(R.drawable.ic_sos);
    }

    private void setupMap() {
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        usersRef.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        getCurrentLocation();
        loadDangerZones();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

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
                    if (report != null) addDangerMarker(report);
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
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_history) startActivity(new Intent(this, HistoryActivity.class));
            else if (id == R.id.nav_tips) startActivity(new Intent(this, TipsActivity.class));
            else if (id == R.id.nav_contacts) startActivity(new Intent(this, ContactsActivity.class));
            return true;
        });
    }

    private void showSOSDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_sos, null);
        builder.setView(view);

        Button callPolice = view.findViewById(R.id.callPoliceButton);
        Button callSamu = view.findViewById(R.id.callSamuButton);
        Button callContact = view.findViewById(R.id.callContactButton);

        callPolice.setOnClickListener(v -> callPhoneNumber("17"));
        callSamu.setOnClickListener(v -> callPhoneNumber("15"));
        callContact.setOnClickListener(v -> showContactsDialog());

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    // -------------------------------------------------------------------------
    // ---------------------- PHONE CALL + SAVE SOS ----------------------------
    // -------------------------------------------------------------------------

    private void callPhoneNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {

            startActivity(intent);

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    saveSOS(
                            number.equals("17") ? "Appel Police" : "Appel SAMU",
                            number,
                            location.getLatitude(),
                            location.getLongitude()
                    );
                }
            });

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, 101);
        }
    }

    private void saveSOS(String type, String contactPhone, double latitude, double longitude) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference sosRef = FirebaseDatabase.getInstance().getReference("sos_alerts");
        String sosId = sosRef.push().getKey();
        if (sosId == null) return;

        sosRef.child(sosId).child("userId").setValue(user.getUid());
        sosRef.child(sosId).child("type").setValue(type);
        sosRef.child(sosId).child("contactPhone").setValue(contactPhone);
        sosRef.child(sosId).child("timestamp").setValue(System.currentTimeMillis());
        sosRef.child(sosId).child("locationUrl")
                .setValue("https://www.google.com/maps?q=" + latitude + "," + longitude);
    }


    // -------------------------------------------------------------------------
    // --------------------------- TRUSTED CONTACT -----------------------------
    // -------------------------------------------------------------------------

    private void showContactsDialog() {
        startActivity(new Intent(this, ContactsActivity.class));
    }


    // -------------------------------------------------------------------------
    // ---------------------------- SEND EMERGENCY SMS -------------------------
    // -------------------------------------------------------------------------

    private void sendEmergencySMS(String phone) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {

                String message = "Aide-moi, je suis en danger !\nMa position : " +
                        "https://www.google.com/maps?q=" +
                        location.getLatitude() + "," + location.getLongitude();

                SmsManager.getDefault().sendTextMessage(phone, null, message, null, null);

                Toast.makeText(MainActivity.this, "SMS envoyé à " + phone, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Position non disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
