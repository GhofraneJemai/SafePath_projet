package com.example.safepath.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safepath.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.safepath.R;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private CheckBox termsCheckBox;
    private ProgressBar progressBar;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usersRef = FirebaseDatabase.getInstance().getReference("users");
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (!validateInputs(name, email, password, confirmPassword)) return;

        progressBar.setVisibility(View.VISIBLE);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Registration successful, redirect to MainActivity
                        Toast.makeText(RegisterActivity.this, "Inscription réussie!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        // Registration failed
                        Toast.makeText(RegisterActivity.this,
                                "Échec de l'inscription: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    private boolean validateInputs(String name, String email, String password, String confirmPassword) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "Veuillez accepter les conditions", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveUserToDatabase(String name, String email) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        User user = new User(userId, name, email, "user");

        usersRef.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "Inscription réussie!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show();
                });
    }
}