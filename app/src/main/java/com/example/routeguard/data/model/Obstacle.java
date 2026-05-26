package com.example.routeguard.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "obstacles")
public class Obstacle {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("severity")
    private String severity;

    @SerializedName("lat")
    private double lat;

    @SerializedName("lon")
    private double lon;

    @SerializedName("reportedAt")
    private long reportedAt;

    @SerializedName("expiresAt")
    private long expiresAt;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("confirmCount")
    private int confirmCount;

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public long getReportedAt() { return reportedAt; }
    public void setReportedAt(long reportedAt) { this.reportedAt = reportedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getConfirmCount() { return confirmCount; }
    public void setConfirmCount(int confirmCount) { this.confirmCount = confirmCount; }
}