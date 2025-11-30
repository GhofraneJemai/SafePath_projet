package com.example.safepath.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.safepath.R;
import com.example.safepath.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private ImageView profileImage;
    private TextView nameText, emailText;
    private Button editProfileButton, changePasswordButton, logoutButton;
    private Switch notificationsSwitch;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        initViews();
        loadUserData();
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
        changePasswordButton.setOnClickListener(v -> changePassword());
        logoutButton.setOnClickListener(v -> logout());

        profileImage.setOnClickListener(v -> changeProfileImage());
    }

    private void loadUserData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    nameText.setText(user.getName());
                    emailText.setText(user.getEmail());

                    if (user.getProfileImage() != null) {
                        Glide.with(ProfileActivity.this).load(user.getProfileImage()).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Erreur chargement profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editProfile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier le profil");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText nameEditText = view.findViewById(R.id.nameEditText);
        EditText phoneEditText = view.findViewById(R.id.phoneEditText);

        // Pré-remplir les champs
        nameEditText.setText(nameText.getText());

        builder.setView(view);
        builder.setPositiveButton("Sauvegarder", (dialog, which) -> {
            String newName = nameEditText.getText().toString().trim();
            String phone = phoneEditText.getText().toString().trim();

            updateProfile(newName, phone);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updateProfile(String name, String phone) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (!phone.isEmpty()) {
            updates.put("phone", phone);
        }

        usersRef.child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    nameText.setText(name);
                    Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                });
    }

    private void changePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Changer le mot de passe");

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordEditText = view.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordEditText = view.findViewById(R.id.newPasswordEditText);
        EditText confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);

        builder.setView(view);
        builder.setPositiveButton("Changer", (dialog, which) -> {
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (newPassword.equals(confirmPassword)) {
                updatePassword(newPassword);
            } else {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Mot de passe mis à jour", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Erreur lors de la mise à jour du mot de passe", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void changeProfileImage() {
        // Implémentation changement photo de profil
        Toast.makeText(this, "Fonctionnalité à implémenter", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                .setPositiveButton("Déconnecter", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}