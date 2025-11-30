package com.example.safepath.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.safepath.R;
import com.example.safepath.models.EmergencyContact;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private List<EmergencyContact> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(EmergencyContact contact);
    }

    public ContactsAdapter(List<EmergencyContact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.nameText.setText(contact.getName());
        holder.phoneText.setText(contact.getPhone());

        holder.deleteButton.setOnClickListener(v -> listener.onContactClick(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameText, phoneText;
        public ImageButton deleteButton;

        public ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            phoneText = view.findViewById(R.id.phoneText);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
}