package com.example.safepath.utils;

import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";

    // URL Firebase Realtime Database
    private static final String DATABASE_URL =
            "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app";

    private static FirebaseDatabase firebaseDatabase;
    private static FirebaseStorage firebaseStorage;

    // ================================
    //  GET DATABASE
    // ================================
    public static FirebaseDatabase getDatabase() {
        if (firebaseDatabase == null) {
            try {
                firebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL);

                // Activer une seule fois
                firebaseDatabase.setPersistenceEnabled(true);

                Log.d(TAG, "✅ FirebaseDatabase instance created with URL: " + DATABASE_URL);
            } catch (Exception e) {
                Log.e(TAG, "❌ Error using explicit URL: " + e.getMessage());
                firebaseDatabase = FirebaseDatabase.getInstance();
            }
        }
        return firebaseDatabase;
    }

    // ================================
    //  GET STORAGE
    // ================================
    public static FirebaseStorage getStorage() {
        if (firebaseStorage == null) {
            firebaseStorage = FirebaseStorage.getInstance();
            Log.d(TAG, "✅ FirebaseStorage instance created");
        }
        return firebaseStorage;
    }

    // ================================
    //  RETURN URL FOR DEBUG ONLY
    // ================================
    public static String getDatabaseUrl() {
        return DATABASE_URL;
    }
}
