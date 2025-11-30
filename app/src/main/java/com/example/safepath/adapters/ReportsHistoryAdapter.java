package com.example.safepath.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.safepath.models.Report;
import com.example.safepath.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportsHistoryAdapter extends RecyclerView.Adapter<ReportsHistoryAdapter.ViewHolder> {
    private List<Report> reports;

    public ReportsHistoryAdapter(List<Report> reports) {
        this.reports = reports;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reports.get(position);
        holder.dangerTypeText.setText(report.getDangerType());
        holder.descriptionText.setText(report.getDescription());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.dateText.setText(sdf.format(report.getCreatedAt()));

        // Couleur selon le statut
        switch (report.getStatus()) {
            case "approved":
                holder.statusText.setText("Validé");
                holder.statusText.setTextColor(holder.itemView.getContext().getColor(R.color.safe_color));
                break;
            case "rejected":
                holder.statusText.setText("Rejeté");
                holder.statusText.setTextColor(holder.itemView.getContext().getColor(R.color.danger_color));
                break;
            default:
                holder.statusText.setText("En attente");
                holder.statusText.setTextColor(holder.itemView.getContext().getColor(R.color.secondary_color));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView dangerTypeText, descriptionText, dateText, statusText;

        public ViewHolder(View view) {
            super(view);
            dangerTypeText = view.findViewById(R.id.dangerTypeText);
            descriptionText = view.findViewById(R.id.descriptionText);
            dateText = view.findViewById(R.id.dateText);
            statusText = view.findViewById(R.id.statusText);
        }
    }
}