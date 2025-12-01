package com.example.safepath.models;

import java.util.Date;

public class SOSAlert {
    private String id;
    private String userId;
    private String userName;
    private String type; // "SMS" ou "Appel"
    private String contactName;
    private String contactPhone;
    private double latitude;
    private double longitude;
    private String locationUrl;
    private long timestamp;
    private String date;
    private String status; // "envoyé", "échoué", "annulé"
    private String message;
    private int contactsNotified;

    // Constructeur vide requis pour Firebase
    public SOSAlert() {}

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocationUrl() { return locationUrl; }
    public void setLocationUrl(String locationUrl) { this.locationUrl = locationUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getContactsNotified() { return contactsNotified; }
    public void setContactsNotified(int contactsNotified) { this.contactsNotified = contactsNotified; }
    // Dans la classe SOSAlert, ajoutez ce constructeur :

    // Constructeur pour l'alerte SOS
    public SOSAlert(String userId, double latitude, double longitude) {
        this.userId = userId;
        this.userName = "Utilisateur"; // Vous pouvez récupérer le nom depuis Firebase Auth
        this.type = "SOS";
        this.contactName = "Secours";
        this.contactPhone = "112";
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationUrl = "https://maps.google.com/?q=" + latitude + "," + longitude;
        this.timestamp = System.currentTimeMillis();
        this.date = new Date().toString();
        this.status = "actif";
        this.message = "Alerte SOS déclenchée - URGENCE!";
        this.contactsNotified = 0;
    }

    // Constructeur pour SMS/Appel
    public SOSAlert(String userId, String type, String contactName, String contactPhone,
                    double latitude, double longitude, String message) {
        this.userId = userId;
        this.userName = "Utilisateur";
        this.type = type;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationUrl = "https://maps.google.com/?q=" + latitude + "," + longitude;
        this.timestamp = System.currentTimeMillis();
        this.date = new Date().toString();
        this.status = "envoyé";
        this.message = message;
        this.contactsNotified = 0;
    }
}