package com.example.safepath.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safepath.R;
import com.example.safepath.models.SecurityTip;

import java.util.List;

public class TipsAdapter extends RecyclerView.Adapter<TipsAdapter.ViewHolder> {
    private List<SecurityTip> tips;

    public TipsAdapter(List<SecurityTip> tips) {
        this.tips = tips;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SecurityTip tip = tips.get(position);
        holder.titleText.setText(tip.getTitle());
        holder.descriptionText.setText(tip.getDescription());
        holder.iconImage.setImageResource(tip.getIconRes());
    }

    @Override
    public int getItemCount() {
        return tips.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleText, descriptionText;
        public ImageView iconImage;

        public ViewHolder(View view) {
            super(view);
            titleText = view.findViewById(R.id.titleText);
            descriptionText = view.findViewById(R.id.descriptionText);
            iconImage = view.findViewById(R.id.iconImage);
        }
    }
}