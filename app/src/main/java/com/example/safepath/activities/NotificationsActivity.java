package com.example.safepath.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.adapters.NotificationsAdapter;
import com.example.safepath.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private NotificationsAdapter adapter;
    private List<Notification> notifications;
    private DatabaseReference notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
        initViews();
        loadNotifications();
    }

    private void initViews() {
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications);
        notificationsRecyclerView.setAdapter(adapter);

        // Bouton retour
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void loadNotifications() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Query query = notificationsRef.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notifications.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Notification notification = snapshot.getValue(Notification.class);
                    if (notification != null) {
                        notification.setId(snapshot.getKey());
                        notifications.add(notification);
                    }
                }
                adapter.notifyDataSetChanged();

                if (notifications.isEmpty()) {
                    Toast.makeText(NotificationsActivity.this, "Aucune notification", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(NotificationsActivity.this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
            }
        });
    }
}