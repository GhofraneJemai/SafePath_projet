package com.example.safepath.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.safepath.R;
import com.example.safepath.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton, googleLoginButton;
    private TextView registerTextView, forgotPasswordTextView;
    private ProgressBar progressBar;

    private GoogleSignInClient googleSignInClient;
    private DatabaseReference usersRef;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisation Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Configuration Google Sign-In
        configureGoogleSignIn();

        initViews();
        setupClickListeners();

    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleLoginButton = findViewById(R.id.googleLoginButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> loginUser());
        registerTextView.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordTextView.setOnClickListener(v -> resetPassword());
        googleLoginButton.setOnClickListener(v -> loginWithGoogle());
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        redirectToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Échec de la connexion: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Entrez votre email", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email de réinitialisation envoyé", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Échec de l'envoi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Connexion Google réussie, authentifier avec Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Erreur connexion Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            // Connexion réussie
                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (firebaseUser != null) {
                                // Vérifier si c'est une nouvelle inscription
                                checkIfNewUser(firebaseUser);
                            }
                        } else {
                            // Échec de l'authentification
                            Toast.makeText(LoginActivity.this,
                                    "Échec authentification Google: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkIfNewUser(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();

        usersRef.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    // Utilisateur existant, rediriger directement
                    redirectToMainActivity();
                } else {
                    // Nouvel utilisateur, créer le profil
                    createUserProfile(firebaseUser);
                }
            } else {
                Toast.makeText(LoginActivity.this, "Erreur vérification utilisateur", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserProfile(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Utilisateur";
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String profileImage = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null;

        User user = new User(userId, name, email, "user");
        user.setProfileImage(profileImage);

        usersRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        redirectToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Erreur création profil", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirectToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Vérifier si l'utilisateur est déjà connecté
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            redirectToMainActivity();
        }
    }
}