package com.example.safepath.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.safepath.R;
import com.example.safepath.models.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView nameText, emailText;
    private Button editProfileButton, changePasswordButton, logoutButton;
    private Switch notificationsSwitch;

    private DatabaseReference usersRef;
    private FirebaseUser firebaseUser;

    // Choisir image depuis galerie
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initViews();
        loadUserData();
        initImagePicker();
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        editProfileButton = findViewById(R.id.editProfileButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        logoutButton = findViewById(R.id.logoutButton);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);

        editProfileButton.setOnClickListener(v -> editProfile());
        changePasswordButton.setOnClickListener(v -> changePasswordDialog());
        logoutButton.setOnClickListener(v -> logout());
        profileImage.setOnClickListener(v -> pickImage());
    }

    // ------------------- CHARGEMENT DES DONNÉES USER ------------------------
    private void loadUserData() {

        if (firebaseUser == null) return;

        String userId = firebaseUser.getUid();

        usersRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                User user = snapshot.getValue(User.class);

                if (user == null) return;

                nameText.setText(user.getName());
                emailText.setText(user.getEmail());

                if (user.getProfileImage() != null) {
                    Glide.with(ProfileActivity.this)
                            .load(user.getProfileImage())
                            .into(profileImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Erreur chargement profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------------ EDIT PROFIL -----------------------------------
    private void editProfile() {

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText nameEdit = view.findViewById(R.id.nameEditText);
        EditText phoneEdit = view.findViewById(R.id.phoneEditText);

        nameEdit.setText(nameText.getText());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Modifier le profil")
                .setView(view)
                .setPositiveButton("Sauvegarder", null)
                .setNegativeButton("Annuler", null)
                .create();

        dialog.setOnShowListener(dlg -> {
            Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String newName = nameEdit.getText().toString().trim();
                String newPhone = phoneEdit.getText().toString().trim();

                if (newName.isEmpty()) {
                    nameEdit.setError("Nom obligatoire");
                    return;
                }

                updateProfile(newName, newPhone);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void updateProfile(String name, String phone) {
        String userId = firebaseUser.getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (!TextUtils.isEmpty(phone)) updates.put("phone", phone);

        usersRef.child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    nameText.setText(name);
                    Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur mise à jour", Toast.LENGTH_SHORT).show()
                );
    }

    // ----------------------- CHANGE PASSWORD ---------------------------------
    private void changePasswordDialog() {

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        EditText currentPass = view.findViewById(R.id.currentPasswordEditText);
        EditText newPass = view.findViewById(R.id.newPasswordEditText);
        EditText confirmPass = view.findViewById(R.id.confirmPasswordEditText);

        new AlertDialog.Builder(this)
                .setTitle("Changer le mot de passe")
                .setView(view)
                .setPositiveButton("Changer", (dialog, which) -> {

                    String oldP = currentPass.getText().toString();
                    String newP = newPass.getText().toString();
                    String confirmP = confirmPass.getText().toString();

                    if (!newP.equals(confirmP)) {
                        Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reAuthenticateAndUpdatePassword(oldP, newP);

                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void reAuthenticateAndUpdatePassword(String oldPass, String newPass) {

        AuthCredential credential =
                EmailAuthProvider.getCredential(firebaseUser.getEmail(), oldPass);

        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    firebaseUser.updatePassword(newPass)
                            .addOnSuccessListener(v ->
                                    Toast.makeText(this, "Mot de passe mis à jour", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ancien mot de passe incorrect", Toast.LENGTH_SHORT).show());
    }

    // ----------------------- UPDATE PROFILE IMAGE ---------------------------
    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri imageUri = result.getData().getData();

                        profileImage.setImageURI(imageUri);

                        // Option : upload sur Firebase Storage ici
                    }
                });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    // ---------------------------- LOGOUT ------------------------------------
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}