package com.example.safepath.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.safepath.models.Report;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.safepath.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {
    private Spinner dangerTypeSpinner;
    private EditText descriptionEditText;
    private ImageView photoImageView;
    private Button takePhotoButton, submitButton;
    private ProgressBar progressBar;

    private DatabaseReference reportsRef;
    private FirebaseStorage storage;
    private String currentPhotoPath;
    private double currentLatitude, currentLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        storage = FirebaseStorage.getInstance();
        getCurrentLocation();
        initViews();
    }

    private void initViews() {
        dangerTypeSpinner = findViewById(R.id.dangerTypeSpinner);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        photoImageView = findViewById(R.id.photoImageView);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);

        // Adapter pour le spinner des types de danger
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.danger_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dangerTypeSpinner.setAdapter(adapter);

        takePhotoButton.setOnClickListener(v -> takePhoto());
        submitButton.setOnClickListener(v -> submitReport());
    }

    private void getCurrentLocation() {
        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLatitude = location.getLatitude();
                            currentLongitude = location.getLongitude();
                        }
                    });
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Erreur création fichier", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.safepath.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 100);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            Glide.with(this).load(currentPhotoPath).into(photoImageView);
        }
    }

    private void submitReport() {
        String dangerType = dangerTypeSpinner.getSelectedItem().toString();
        String description = descriptionEditText.getText().toString().trim();

        if (description.isEmpty()) {
            Toast.makeText(this, "Veuillez décrire le danger", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // CRÉER LE SIGNALEMENT AVEC STATUT "ACTIF" DIRECTEMENT
        Report report = new Report(userId, dangerType, description, currentLatitude, currentLongitude);
        report.setStatus("active"); // Au lieu de "pending"

        if (currentPhotoPath != null) {
            uploadImageAndSaveReport(report);
        } else {
            saveReportToDatabase(report, null);
        }
    }

    private void uploadImageAndSaveReport(Report report) {
        Uri fileUri = Uri.fromFile(new File(currentPhotoPath));
        StorageReference photoRef = storage.getReference()
                .child("report_photos/" + fileUri.getLastPathSegment());

        photoRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> photoRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveReportToDatabase(report, uri.toString())))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur upload photo", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveReportToDatabase(Report report, String imageUrl) {
        report.setImageUrl(imageUrl);

        String reportId = reportsRef.push().getKey();
        reportsRef.child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);

                    // Afficher le message de confirmation
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Erreur envoi signalement", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Signalement envoyé")
                .setMessage("Votre signalement a été soumis avec succès.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}