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
import com.example.safepath.models.SOSAlert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SOSHistoryFragment extends Fragment {
    private RecyclerView sosRecyclerView;
    private List<SOSAlert> sosAlerts;
    private DatabaseReference sosAlertsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sos_history, container, false);

        sosAlertsRef = FirebaseDatabase.getInstance().getReference("sos_alerts");
        sosRecyclerView = view.findViewById(R.id.sosRecyclerView);
        sosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        sosAlerts = new ArrayList<>();
        loadSOSHistory();
        return view;
    }

    private void loadSOSHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query query = sosAlertsRef.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sosAlerts.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    SOSAlert alert = snapshot.getValue(SOSAlert.class);
                    if (alert != null) {
                        alert.setId(snapshot.getKey());
                        sosAlerts.add(alert);
                    }
                }
                // Adapter pour SOS à implémenter si besoin
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Erreur chargement alertes SOS", Toast.LENGTH_SHORT).show();
            }
        });
    }
}