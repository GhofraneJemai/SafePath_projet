package com.example.safepath.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.adapters.ReportsHistoryAdapter;
import com.example.safepath.models.Report;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ReportsHistoryFragment extends Fragment {
    private RecyclerView reportsRecyclerView;
    private ReportsHistoryAdapter adapter;
    private List<Report> reports;
    private DatabaseReference reportsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports_history, container, false);

        reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        reportsRecyclerView = view.findViewById(R.id.reportsRecyclerView);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        reports = new ArrayList<>();
        adapter = new ReportsHistoryAdapter(reports);
        reportsRecyclerView.setAdapter(adapter);

        loadReportsHistory();
        return view;
    }

    private void loadReportsHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query query = reportsRef.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                reports.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Report report = snapshot.getValue(Report.class);
                    if (report != null) {
                        report.setId(snapshot.getKey());
                        reports.add(report);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Erreur chargement historique", Toast.LENGTH_SHORT).show();
            }
        });
    }
}