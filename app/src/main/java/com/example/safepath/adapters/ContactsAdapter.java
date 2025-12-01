package com.example.safepath.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
    private Context context;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onDelete(EmergencyContact contact);
        void onCall(EmergencyContact contact);
        void onSMS(EmergencyContact contact);
    }

    public ContactsAdapter(List<EmergencyContact> contacts, OnContactActionListener listener, Context context) {
        this.contacts = contacts;
        this.listener = listener;
        this.context = context;
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

        holder.deleteButton.setOnClickListener(v -> listener.onDelete(contact));
        holder.callButton.setOnClickListener(v -> listener.onCall(contact));
        holder.smsButton.setOnClickListener(v -> listener.onSMS(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameText, phoneText;
        public ImageButton deleteButton, callButton, smsButton;

        public ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            phoneText = view.findViewById(R.id.phoneText);
            deleteButton = view.findViewById(R.id.deleteButton);
            callButton = view.findViewById(R.id.callButton);
            smsButton = view.findViewById(R.id.smsButton);
        }
    }
}