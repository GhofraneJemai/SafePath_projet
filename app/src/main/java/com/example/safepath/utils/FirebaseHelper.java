package com.example.safepath.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    private static FirebaseHelper instance;
    private FirebaseDatabase database;
    private DatabaseReference reportsRef;
    private DatabaseReference safeZonesRef;
    private DatabaseReference usersRef;

    public static final String DATABASE_URL = "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app";

    private FirebaseHelper() {
        // Utilisez toujours la même instance avec la même URL
        database = FirebaseDatabase.getInstance(DATABASE_URL);
        reportsRef = database.getReference("reports");
        safeZonesRef = database.getReference("safe_zones");
        usersRef = database.getReference("users");
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public DatabaseReference getReportsRef() {
        return reportsRef;
    }

    public DatabaseReference getSafeZonesRef() {
        return safeZonesRef;
    }

    public DatabaseReference getUsersRef() {
        return usersRef;
    }
}