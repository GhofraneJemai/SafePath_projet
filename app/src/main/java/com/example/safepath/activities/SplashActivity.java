package com.example.safepath.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.example.safepath.R;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            // Vérifier si l'utilisateur est déjà connecté
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new
                        Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, SPLASH_DURATION);
    }
}
