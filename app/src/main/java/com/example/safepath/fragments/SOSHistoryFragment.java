package com.example.safepath.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.adapters.SOSAdapter;
import com.example.safepath.models.SOSAlert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SOSHistoryFragment extends Fragment {
    private static final String TAG = "SOSHistoryFragment";

    private RecyclerView recyclerView;
    private SOSAdapter sosAdapter;
    private List<SOSAlert> sosList;
    private ProgressBar progressBar;
    private TextView tvNoSOS;

    private DatabaseReference sosRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sos_history, container, false);

        initViews(view);
        initFirebase();
        setupRecyclerView();
        loadUserSOSAlerts();

        return view;
    }

    private void initViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.sosRecyclerView);
            progressBar = view.findViewById(R.id.progressBar);
            tvNoSOS = view.findViewById(R.id.tvNoSOS);
            sosList = new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Erreur initViews: " + e.getMessage());
            Toast.makeText(getContext(), "Erreur initialisation", Toast.LENGTH_SHORT).show();
        }
    }

    private void initFirebase() {
        try {
            // Initialiser Firebase
            auth = FirebaseAuth.getInstance();
            currentUser = auth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
                return;
            }

            // Utiliser la même URL que dans MainActivity
            String databaseUrl = "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app";
            FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);
            sosRef = database.getReference("sos_alerts");

            Log.d(TAG, "Firebase initialisé, userId: " + currentUser.getUid());
        } catch (Exception e) {
            Log.e(TAG, "Erreur initFirebase: " + e.getMessage());
            Toast.makeText(getContext(), "Erreur Firebase", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        try {
            sosAdapter = new SOSAdapter(sosList, getContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(sosAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Erreur setupRecyclerView: " + e.getMessage());
        }
    }

    private void loadUserSOSAlerts() {
        try {
            if (currentUser == null) {
                Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
                showEmptyState();
                return;
            }

            // Afficher le loading
            progressBar.setVisibility(View.VISIBLE);
            tvNoSOS.setVisibility(View.GONE);

            // Filtrer les alertes SOS par userId de l'utilisateur connecté
            Query userSOSQuery = sosRef.orderByChild("userId").equalTo(currentUser.getUid());

            Log.d(TAG, "Chargement des SOS pour userId: " + currentUser.getUid());

            userSOSQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        sosList.clear();

                        Log.d(TAG, "Données reçues, nombre: " + snapshot.getChildrenCount());

                        if (!snapshot.exists()) {
                            Log.d(TAG, "Aucune donnée trouvée");
                            showEmptyState();
                            return;
                        }

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            try {
                                // Méthode 1: Récupérer comme Map
                                Object data = dataSnapshot.getValue();
                                Log.d(TAG, "DataSnapshot: " + dataSnapshot.getKey() + " = " + data);

                                // Méthode 2: Essayer de parser comme SOSAlert
                                SOSAlert sosAlert = dataSnapshot.getValue(SOSAlert.class);

                                if (sosAlert != null) {
                                    // Assurez-vous que l'ID est défini
                                    sosAlert.setId(dataSnapshot.getKey());
                                    sosList.add(sosAlert);

                                    Log.d(TAG, "SOS chargé: " + sosAlert.getType() +
                                            ", timestamp: " + sosAlert.getTimestamp());
                                } else {
                                    Log.d(TAG, "SOSAlert est null pour la clé: " + dataSnapshot.getKey());
                                    // Essayer de créer un SOSAlert manuellement
                                    createSOSAlertFromMap(dataSnapshot);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur parsing snapshot: " + e.getMessage());
                            }
                        }

                        // Trier par timestamp (du plus récent au plus ancien)
                        Collections.sort(sosList, new Comparator<SOSAlert>() {
                            @Override
                            public int compare(SOSAlert s1, SOSAlert s2) {
                                return Long.compare(s2.getTimestamp(), s1.getTimestamp());
                            }
                        });

                        sosAdapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        if (sosList.isEmpty()) {
                            showEmptyState();
                        } else {
                            tvNoSOS.setVisibility(View.GONE);
                        }

                        // Debug: afficher le nombre d'alertes chargées
                        Toast.makeText(getContext(),
                                sosList.size() + " alertes SOS chargées",
                                Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.e(TAG, "Erreur dans onDataChange: " + e.getMessage());
                        e.printStackTrace();
                        progressBar.setVisibility(View.GONE);
                        showEmptyState();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Erreur Firebase: " + error.getMessage());
                    Toast.makeText(getContext(),
                            "Erreur: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur loadUserSOSAlerts: " + e.getMessage());
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            showEmptyState();
        }
    }

    private void createSOSAlertFromMap(DataSnapshot dataSnapshot) {
        try {
            SOSAlert sosAlert = new SOSAlert();
            sosAlert.setId(dataSnapshot.getKey());

            // Récupérer chaque champ individuellement
            if (dataSnapshot.child("userId").exists()) {
                sosAlert.setUserId(dataSnapshot.child("userId").getValue(String.class));
            }
            if (dataSnapshot.child("userName").exists()) {
                sosAlert.setUserName(dataSnapshot.child("userName").getValue(String.class));
            }
            if (dataSnapshot.child("type").exists()) {
                sosAlert.setType(dataSnapshot.child("type").getValue(String.class));
            }
            if (dataSnapshot.child("contactName").exists()) {
                sosAlert.setContactName(dataSnapshot.child("contactName").getValue(String.class));
            }
            if (dataSnapshot.child("contactPhone").exists()) {
                sosAlert.setContactPhone(dataSnapshot.child("contactPhone").getValue(String.class));
            }
            if (dataSnapshot.child("latitude").exists()) {
                Object lat = dataSnapshot.child("latitude").getValue();
                if (lat != null) {
                    if (lat instanceof Double) {
                        sosAlert.setLatitude((Double) lat);
                    } else if (lat instanceof Long) {
                        sosAlert.setLatitude(((Long) lat).doubleValue());
                    } else if (lat instanceof String) {
                        sosAlert.setLatitude(Double.parseDouble((String) lat));
                    }
                }
            }
            if (dataSnapshot.child("longitude").exists()) {
                Object lon = dataSnapshot.child("longitude").getValue();
                if (lon != null) {
                    if (lon instanceof Double) {
                        sosAlert.setLongitude((Double) lon);
                    } else if (lon instanceof Long) {
                        sosAlert.setLongitude(((Long) lon).doubleValue());
                    } else if (lon instanceof String) {
                        sosAlert.setLongitude(Double.parseDouble((String) lon));
                    }
                }
            }
            if (dataSnapshot.child("timestamp").exists()) {
                Object ts = dataSnapshot.child("timestamp").getValue();
                if (ts != null) {
                    if (ts instanceof Long) {
                        sosAlert.setTimestamp((Long) ts);
                    } else if (ts instanceof String) {
                        sosAlert.setTimestamp(Long.parseLong((String) ts));
                    }
                }
            }
            if (dataSnapshot.child("status").exists()) {
                sosAlert.setStatus(dataSnapshot.child("status").getValue(String.class));
            }
            if (dataSnapshot.child("message").exists()) {
                sosAlert.setMessage(dataSnapshot.child("message").getValue(String.class));
            }

            sosList.add(sosAlert);
            Log.d(TAG, "SOSAlert créé manuellement: " + sosAlert.getType());

        } catch (Exception e) {
            Log.e(TAG, "Erreur createSOSAlertFromMap: " + e.getMessage());
        }
    }

    private void showEmptyState() {
        try {
            progressBar.setVisibility(View.GONE);
            tvNoSOS.setText("Aucune alerte SOS trouvée");
            tvNoSOS.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Erreur showEmptyState: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rafraîchir les données quand le fragment redevient visible
        try {
            if (currentUser != null) {
                loadUserSOSAlerts();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur onResume: " + e.getMessage());
        }
    }
}