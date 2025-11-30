package com.example.safepath.models;

public class SecurityTip {
    private String title;
    private String description;
    private int iconRes;

    public SecurityTip(String title, String description, int iconRes) {
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
    }

    // Getters et setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }
}