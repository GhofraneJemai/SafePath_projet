package com.example.safepath.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.models.SOSAlert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SOSAdapter extends RecyclerView.Adapter<SOSAdapter.SOSViewHolder> {

    private List<SOSAlert> sosList;
    private Context context;
    private SimpleDateFormat dateFormat;

    public SOSAdapter(List<SOSAlert> sosList, Context context) {
        this.sosList = sosList;
        this.context = context;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public SOSViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sos, parent, false);
        return new SOSViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SOSViewHolder holder, int position) {
        SOSAlert sosAlert = sosList.get(position);

        // Définir l'icône selon le type (SMS ou Appel)
        int iconRes = getIconForType(sosAlert.getType());
        holder.icon.setImageResource(iconRes);

        // Titre avec le type et le contact
        String title = "Alerte " + getTypeDisplayName(sosAlert.getType());
        if (sosAlert.getContactName() != null && !sosAlert.getContactName().isEmpty()) {
            title += " à " + sosAlert.getContactName();
        }
        holder.title.setText(title);

        // Numéro de téléphone
        if (sosAlert.getContactPhone() != null && !sosAlert.getContactPhone().isEmpty()) {
            holder.phone.setText(sosAlert.getContactPhone());
            holder.phone.setVisibility(View.VISIBLE);
        } else {
            holder.phone.setVisibility(View.GONE);
        }

        // Message
        if (sosAlert.getMessage() != null && !sosAlert.getMessage().isEmpty()) {
            holder.message.setText(sosAlert.getMessage());
            holder.message.setVisibility(View.VISIBLE);
        } else {
            holder.message.setVisibility(View.GONE);
        }

        // Date et heure
        if (sosAlert.getTimestamp() != 0) {
            Date date = new Date(sosAlert.getTimestamp());
            holder.dateTime.setText(dateFormat.format(date));
        } else if (sosAlert.getDate() != null) {
            holder.dateTime.setText(sosAlert.getDate());
        } else {
            holder.dateTime.setText("Date inconnue");
        }

        // Statut avec couleur
        String statusText = getStatusText(sosAlert.getStatus());
        holder.status.setText(statusText);
        int statusColor = getStatusColor(sosAlert.getStatus());
        holder.status.setTextColor(context.getResources().getColor(statusColor));

        // Position avec lien cliquable
        if (sosAlert.getLatitude() != 0 && sosAlert.getLongitude() != 0) {
            String location = String.format(Locale.getDefault(),
                    "Position: %.6f, %.6f", sosAlert.getLatitude(), sosAlert.getLongitude());
            holder.location.setText(location);

            // Rendre le TextView de localisation cliquable
            holder.location.setOnClickListener(v -> {
                openLocationInMaps(sosAlert.getLatitude(), sosAlert.getLongitude());
            });
            holder.location.setClickable(true);

            // Afficher l'icône de carte
            holder.mapIcon.setVisibility(View.VISIBLE);
            holder.mapIcon.setOnClickListener(v -> {
                openLocationInMaps(sosAlert.getLatitude(), sosAlert.getLongitude());
            });
        } else {
            holder.location.setText("Position non disponible");
            holder.location.setClickable(false);
            holder.mapIcon.setVisibility(View.GONE);
        }

        // Contacts notifiés (avec gestion d'erreur)
        if (sosAlert.getContactsNotified() > 0) {
            String contactsText = getContactsNotifiedText(sosAlert.getContactsNotified());
            holder.contactsNotified.setText(contactsText);
            holder.contactsNotified.setVisibility(View.VISIBLE);
        } else {
            holder.contactsNotified.setVisibility(View.GONE);
        }

        // URL de localisation
        if (sosAlert.getLocationUrl() != null && !sosAlert.getLocationUrl().isEmpty()) {
            holder.locationUrl.setText(sosAlert.getLocationUrl());
            holder.locationUrl.setVisibility(View.VISIBLE);
            holder.locationUrl.setOnClickListener(v -> {
                openUrlInBrowser(sosAlert.getLocationUrl());
            });
        } else {
            holder.locationUrl.setVisibility(View.GONE);
        }

        // Gestion du clic sur la carte entière
        holder.cardView.setOnClickListener(v -> {
            showSOSDetails(sosAlert);
        });

        // Bouton d'appel
        if (sosAlert.getContactPhone() != null && !sosAlert.getContactPhone().isEmpty()) {
            holder.callButton.setVisibility(View.VISIBLE);
            holder.callButton.setOnClickListener(v -> {
                makePhoneCall(sosAlert.getContactPhone());
            });
        } else {
            holder.callButton.setVisibility(View.GONE);
        }

        // Bouton SMS
        if (sosAlert.getContactPhone() != null && !sosAlert.getContactPhone().isEmpty()) {
            holder.smsButton.setVisibility(View.VISIBLE);
            holder.smsButton.setOnClickListener(v -> {
                sendSMS(sosAlert.getContactPhone());
            });
        } else {
            holder.smsButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return sosList.size();
    }

    private int getIconForType(String type) {
        if (type == null) return R.drawable.ic_sos;

        switch (type.toLowerCase()) {
            case "sms":
                return R.drawable.ic_sms;
            case "appel":
            case "call":
                return R.drawable.ic_phone;
            case "sos":
                return R.drawable.ic_sos;
            default:
                return R.drawable.ic_sos;
        }
    }

    private String getTypeDisplayName(String type) {
        if (type == null) return "SOS";

        switch (type.toLowerCase()) {
            case "sms":
                return "SMS";
            case "appel":
            case "call":
                return "Appel";
            case "sos":
                return "SOS";
            default:
                return type;
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "Inconnu";

        switch (status.toLowerCase()) {
            case "envoyé":
            case "sent":
                return "Envoyé";
            case "échoué":
            case "failed":
                return "Échoué";
            case "annulé":
            case "canceled":
                return "Annulé";
            case "réussi":
            case "success":
                return "Réussi";
            case "actif":
                return "Actif";
            default:
                return status;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return android.R.color.black;

        switch (status.toLowerCase()) {
            case "envoyé":
            case "sent":
            case "réussi":
            case "success":
                return R.color.safe_color;
            case "échoué":
            case "failed":
                return R.color.danger_color;
            case "annulé":
            case "canceled":
                return R.color.secondary_color;
            case "actif":
                return R.color.warning_color;
            default:
                return android.R.color.black;
        }
    }

    private String getContactsNotifiedText(int count) {
        if (count <= 0) return "";

        try {
            // Essayer d'utiliser la ressource plurielle
            return context.getResources().getQuantityString(
                    R.plurals.contacts_notified, count, count);
        } catch (Exception e) {
            // Fallback si la ressource n'existe pas
            if (count == 1) {
                return "1 contact notifié";
            } else {
                return count + " contacts notifiés";
            }
        }
    }

    private void openLocationInMaps(double latitude, double longitude) {
        try {
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f",
                    latitude, longitude, latitude, longitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            context.startActivity(intent);
        } catch (Exception e) {
            // Si Google Maps n'est pas installé, utiliser une URL web
            String url = String.format(Locale.ENGLISH,
                    "https://maps.google.com/maps?q=%f,%f", latitude, longitude);
            openUrlInBrowser(url);
        }
    }

    private void openUrlInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Impossible d'ouvrir le lien", Toast.LENGTH_SHORT).show();
        }
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Impossible de composer le numéro", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:" + phoneNumber));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Impossible d'ouvrir l'application SMS", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSOSDetails(SOSAlert sosAlert) {
        // Créer un dialogue avec les détails complets
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle("Détails de l'alerte");

        StringBuilder details = new StringBuilder();

        if (sosAlert.getContactName() != null) {
            details.append("Contact: ").append(sosAlert.getContactName()).append("\n");
        }

        if (sosAlert.getContactPhone() != null) {
            details.append("Téléphone: ").append(sosAlert.getContactPhone()).append("\n");
        }

        details.append("Type: ").append(getTypeDisplayName(sosAlert.getType())).append("\n");

        if (sosAlert.getTimestamp() != 0) {
            Date date = new Date(sosAlert.getTimestamp());
            details.append("Date: ").append(dateFormat.format(date)).append("\n");
        }

        if (sosAlert.getStatus() != null) {
            details.append("Statut: ").append(getStatusText(sosAlert.getStatus())).append("\n");
        }

        if (sosAlert.getLatitude() != 0 && sosAlert.getLongitude() != 0) {
            details.append("Latitude: ").append(sosAlert.getLatitude()).append("\n");
            details.append("Longitude: ").append(sosAlert.getLongitude()).append("\n");
        }

        if (sosAlert.getMessage() != null) {
            details.append("\nMessage:\n").append(sosAlert.getMessage());
        }

        builder.setMessage(details.toString());

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        // Ajouter un bouton pour ouvrir la localisation
        if (sosAlert.getLatitude() != 0 && sosAlert.getLongitude() != 0) {
            builder.setNeutralButton("Ouvrir la carte", (dialog, which) -> {
                openLocationInMaps(sosAlert.getLatitude(), sosAlert.getLongitude());
            });
        }

        builder.show();
    }

    static class SOSViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView icon, mapIcon, callButton, smsButton;
        TextView title, phone, message, dateTime, status, location, contactsNotified, locationUrl;

        public SOSViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            icon = itemView.findViewById(R.id.ivIcon);
            mapIcon = itemView.findViewById(R.id.ivMapIcon);
            callButton = itemView.findViewById(R.id.btnCall);
            smsButton = itemView.findViewById(R.id.btnSMS);
            title = itemView.findViewById(R.id.tvTitle);
            phone = itemView.findViewById(R.id.tvPhone);
            message = itemView.findViewById(R.id.tvMessage);
            dateTime = itemView.findViewById(R.id.tvDateTime);
            status = itemView.findViewById(R.id.tvStatus);
            location = itemView.findViewById(R.id.tvLocation);
            contactsNotified = itemView.findViewById(R.id.tvContactsNotified);
            locationUrl = itemView.findViewById(R.id.tvLocationUrl);
        }
    }
}