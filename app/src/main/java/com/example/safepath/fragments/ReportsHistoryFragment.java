package com.example.safepath.fragments;

import android.os.Bundle;
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
import com.example.safepath.adapters.ReportsHistoryAdapter;
import com.example.safepath.models.Report;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ReportsHistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private ReportsHistoryAdapter reportAdapter;
    private List<Report> reportList;
    private ProgressBar progressBar;
    private TextView tvNoReports;

    private DatabaseReference reportsRef;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports_history, container, false);

        initViews(view);
        initFirebase();
        setupRecyclerView();
        loadUserReports();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.reportsRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoReports = view.findViewById(R.id.tvNoReports);
        reportList = new ArrayList<>();
    }

    private void initFirebase() {
        // Initialiser Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Utiliser la même URL que dans MainActivity
        String databaseUrl = "https://safepath-7da06-default-rtdb.europe-west1.firebasedatabase.app";
        FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);
        reportsRef = database.getReference("reports");
    }

    private void setupRecyclerView() {
        reportAdapter = new ReportsHistoryAdapter(reportList, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(reportAdapter);
    }

    private void loadUserReports() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        // Afficher le loading
        progressBar.setVisibility(View.VISIBLE);
        tvNoReports.setVisibility(View.GONE);

        // Filtrer les reports par userId de l'utilisateur connecté
        Query userReportsQuery = reportsRef.orderByChild("userId").equalTo(currentUser.getUid());

        userReportsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reportList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Report report = dataSnapshot.getValue(Report.class);
                    if (report != null) {
                        report.setId(dataSnapshot.getKey()); // Stocker l'ID Firebase
                        reportList.add(report);
                    }
                }

                // Trier par date (du plus récent au plus ancien)
                Collections.sort(reportList, new Comparator<Report>() {
                    @Override
                    public int compare(Report r1, Report r2) {
                        if (r1.getCreatedAt() == null || r2.getCreatedAt() == null) {
                            return 0;
                        }
                        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                    }
                });

                reportAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (reportList.isEmpty()) {
                    showEmptyState();
                } else {
                    tvNoReports.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        tvNoReports.setText("Aucun signalement trouvé");
        tvNoReports.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Rafraîchir les données quand le fragment redevient visible
        if (currentUser != null) {
            loadUserReports();
        }
    }
}