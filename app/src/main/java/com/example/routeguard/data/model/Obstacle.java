package com.example.routeguard.data.model;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "obstacles")
public class Obstacle {

    @PrimaryKey
    @NonNull
    @SerializedName(value = "id", alternate = {"_id"})
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("severity")
    private String severity;

    @Embedded
    @SerializedName("location")
    private LocationData location;

    @SerializedName("reportedAt")
    private String reportedAt;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("roadName")
    private String roadName;

    @SerializedName("description")
    private String description;

    @SerializedName(value = "isActive", alternate = {"active"})
    private boolean isActive = true;

    @SerializedName("confirmCount")
    private int confirmCount;

    @SerializedName("reporterId")
    private String reporterId;

    @SerializedName("reporterName")
    private String reporterName;

    public static class LocationData {
        @ColumnInfo(name = "location_type")
        @SerializedName("type")
        private String type = "Point";

        @SerializedName("coordinates")
        private double[] coordinates; // [longitude, latitude]

        // These fields are often sent by backends alongside coordinates
        @Ignore
        @SerializedName("lat")
        private double lat;
        @Ignore
        @SerializedName("lng")
        private double lon;

        public LocationData() {}

        public LocationData(double lng, double lat) {
            this.coordinates = new double[]{lng, lat};
            this.lat = lat;
            this.lon = lng;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double[] getCoordinates() { return coordinates; }
        public void setCoordinates(double[] coordinates) { 
            this.coordinates = coordinates; 
            if (coordinates != null && coordinates.length >= 2) {
                this.lon = coordinates[0];
                this.lat = coordinates[1];
            }
        }

        public double getLat() { 
            if (lat != 0) return lat;
            return coordinates != null && coordinates.length > 1 ? coordinates[1] : 0; 
        }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { 
            if (lon != 0) return lon;
            return coordinates != null && coordinates.length > 0 ? coordinates[0] : 0; 
        }
        public void setLon(double lon) { this.lon = lon; }

        public double getLng() { return getLon(); }
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocationData getLocation() { return location; }
    public void setLocation(LocationData location) { 
        this.location = location;
        syncCoordinates(); // Automatically sync whenever location is set (e.g., by Retrofit)
    }

    public String getReportedAt() { return reportedAt; }
    public void setReportedAt(String reportedAt) { this.reportedAt = reportedAt; }

    @Ignore
    public long getReportedAtMillis() { 
        try {
            if (reportedAt == null) return 0;
            if (reportedAt.contains("T")) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = sdf.parse(reportedAt);
                return date != null ? date.getTime() : 0;
            }
            return Long.parseLong(reportedAt);
        } catch (Exception e) {
            return 0;
        }
    }
    public void setReportedAtMillis(long reportedAt) { this.reportedAt = String.valueOf(reportedAt); }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    @Ignore
    public long getExpiresAtMillis() { 
        try {
            if (expiresAt == null) return 0;
            if (expiresAt.contains("T")) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = sdf.parse(expiresAt);
                return date != null ? date.getTime() : 0;
            }
            return Long.parseLong(expiresAt);
        } catch (Exception e) {
            return 0;
        }
    }
    public void setExpiresAtMillis(long expiresAt) { this.expiresAt = String.valueOf(expiresAt); }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getConfirmCount() { return confirmCount; }
    public void setConfirmCount(int confirmCount) { this.confirmCount = confirmCount; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    // ROOM FIELDS - We calculate these for queries
    @ColumnInfo(name = "lat")
    private double latitude;
    @ColumnInfo(name = "lon")
    private double longitude;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Helper methods to keep existing code working
    public double getLat() {
        if (latitude != 0) return latitude;
        return location != null ? location.getLat() : 0;
    }

    public double getLon() {
        if (longitude != 0) return longitude;
        return location != null ? location.getLng() : 0;
    }

    public void syncCoordinates() {
        if (location != null) {
            this.latitude = location.getLat();
            this.longitude = location.getLon();
            android.util.Log.d("Obstacle", "Synced coords for " + id + ": " + latitude + ", " + longitude);
        } else {
            android.util.Log.w("Obstacle", "Cannot sync coords for " + id + ": location is null");
        }
    }
}