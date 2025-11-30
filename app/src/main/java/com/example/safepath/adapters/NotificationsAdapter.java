package com.example.safepath.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.models.Notification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    private List<Notification> notifications;

    public NotificationsAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.titleText.setText(notification.getTitle());
        holder.messageText.setText(notification.getMessage());

        // Formater la date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.dateText.setText(sdf.format(notification.getCreatedAt()));

        // Icône selon le type
        switch (notification.getType()) {
            case "danger_alert":
                holder.iconImage.setImageResource(R.drawable.ic_danger);
                break;
            case "sos":
                holder.iconImage.setImageResource(R.drawable.ic_sos);
                break;
            default:
                holder.iconImage.setImageResource(R.drawable.ic_notification);
                break;
        }

        // Style selon l'état de lecture
        if (notification.isRead()) {
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iconImage;
        public TextView titleText, messageText, dateText;

        public ViewHolder(View view) {
            super(view);
            iconImage = view.findViewById(R.id.iconImage);
            titleText = view.findViewById(R.id.titleText);
            messageText = view.findViewById(R.id.messageText);
            dateText = view.findViewById(R.id.dateText);
        }
    }
}