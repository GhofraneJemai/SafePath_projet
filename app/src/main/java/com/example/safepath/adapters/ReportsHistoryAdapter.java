package com.example.safepath.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.models.Report;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportsHistoryAdapter extends RecyclerView.Adapter<ReportsHistoryAdapter.ReportViewHolder> {

    private List<Report> reportList;
    private Context context;
    private SimpleDateFormat dateFormat;

    public ReportsHistoryAdapter(List<Report> reportList, Context context) {
        this.reportList = reportList;
        this.context = context;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        // Définir l'icône selon le type de danger
        int iconRes = getIconForDangerType(report.getDangerType());
        holder.icon.setImageResource(iconRes);

        holder.dangerType.setText(report.getDangerType());

        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            holder.description.setText(report.getDescription());
            holder.description.setVisibility(View.VISIBLE);
        } else {
            holder.description.setVisibility(View.GONE);
        }

        // Formater et afficher la date
        if (report.getCreatedAt() != null) {
            holder.date.setText(dateFormat.format(report.getCreatedAt()));
        } else {
            holder.date.setText("Date inconnue");
        }

        // Afficher le statut
        if (report.getStatus() != null) {
            holder.status.setText(report.getStatus());
            int statusColor = getStatusColor(report.getStatus());
            holder.status.setTextColor(context.getResources().getColor(statusColor));
        }

        // Afficher la position
        if (report.getLatitude() != 0 && report.getLongitude() != 0) {
            String location = String.format(Locale.getDefault(),
                    "%.4f, %.4f", report.getLatitude(), report.getLongitude());
            holder.location.setText(location);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    private int getIconForDangerType(String dangerType) {
        if (dangerType == null) return R.drawable.ic_report;

        switch (dangerType.toLowerCase()) {
            case "accident":
                return R.drawable.ic_accident;
            case "incendie":
                return R.drawable.ic_fire;
            case "inondation":
                return R.drawable.ic_flood;
            case "manifestation":
                return R.drawable.ic_protest;
            case "route barrée":
                return R.drawable.ic_road_blocked;
            case "travaux":
                return R.drawable.ic_construction;
            default:
                return R.drawable.ic_report;
        }
    }

    private int getStatusColor(String status) {
        if (status == null) return android.R.color.black;

        switch (status.toLowerCase()) {
            case "actif":
                return R.color.danger_color;
            case "résolu":
                return R.color.safe_color;
            case "en cours":
                return R.color.secondary_color;
            default:
                return android.R.color.black;
        }
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView dangerType, description, date, status, location;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivIcon);
            dangerType = itemView.findViewById(R.id.tvDangerType);
            description = itemView.findViewById(R.id.tvDescription);
            date = itemView.findViewById(R.id.tvDate);
            status = itemView.findViewById(R.id.tvStatus);
            location = itemView.findViewById(R.id.tvLocation);
        }
    }
}