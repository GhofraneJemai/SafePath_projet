package com.example.safepath.models;

import java.util.Date;

public class Notification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String type; // danger_alert, sos, system
    private boolean read;
    private Date createdAt;
    private double latitude;
    private double longitude;

    public Notification() {}

    public Notification(String userId, String title, String message, String type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.createdAt = new Date();
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}