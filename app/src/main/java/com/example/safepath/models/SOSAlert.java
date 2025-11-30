package com.example.safepath.models;

import java.util.Date;

public class SOSAlert {
    private String id;
    private String userId;
    private double latitude;
    private double longitude;
    private Date createdAt;
    private boolean active;

    public SOSAlert() {}

    public SOSAlert(String userId, double latitude, double longitude) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = new Date();
        this.active = true;
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}