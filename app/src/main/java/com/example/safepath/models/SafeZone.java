package com.example.safepath.models;

public class SafeZone {
    private String id;
    private String name;
    private String type;
    private double latitude;
    private double longitude;
    private String address;

    public SafeZone() {}

    public SafeZone(String name, String type, double latitude, double longitude, String address) {
        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}