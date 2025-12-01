package com.example.safepath.models;

import java.util.Date;

public class Report {
    private String id;
    private String userId;
    private String dangerType;
    private String description;
    private String imageUrl;
    private double latitude;
    private double longitude;
    private String status;
    private String locationSource; // Ajoutez ce champ

    private Date createdAt;
    private Date updatedAt;
    private String moderatorId;

    public Report() {}

    public Report(String userId, String dangerType, String description, double latitude, double longitude) {
        this.userId = userId;
        this.dangerType = dangerType;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "active";
        this.createdAt = new Date();
    }
    public Report(String userId, String dangerType, String description, double latitude, double longitude, long timestamp) {
        this.userId = userId;
        this.dangerType = dangerType;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "active";
        this.createdAt = new Date(timestamp);
    }



    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDangerType() { return dangerType; }
    public void setDangerType(String dangerType) { this.dangerType = dangerType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public String getModeratorId() { return moderatorId; }

    public void setModeratorId(String moderatorId) { this.moderatorId = moderatorId; }
}