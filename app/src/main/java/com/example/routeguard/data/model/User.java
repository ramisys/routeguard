package com.example.routeguard.data.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private String id;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("displayName")
    private String displayName;
    
    @SerializedName("reputationPoints")
    private int reputationPoints;
    
    @SerializedName("level")
    private int level;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getReputationPoints() { return reputationPoints; }
    public void setReputationPoints(int reputationPoints) { this.reputationPoints = reputationPoints; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}