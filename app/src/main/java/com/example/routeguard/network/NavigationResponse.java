package com.example.routeguard.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NavigationResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("count")
    public int count;

    @SerializedName("routes")
    public List<SafeRoute> routes;

    public static class SafeRoute {
        @SerializedName("id")
        public String id;

        @SerializedName("geometry")
        public Geometry geometry;

        @SerializedName("duration")
        public double duration; // in seconds

        @SerializedName("distance")
        public double distance; // in meters

        @SerializedName("safetyScore")
        public int safetyScore;

        @SerializedName("hazards")
        public List<Hazard> hazards;

        @SerializedName("isRecommended")
        public boolean isRecommended;
    }

    public static class Geometry {
        @SerializedName("type")
        public String type;

        @SerializedName("coordinates")
        public List<List<Double>> coordinates; // [[lng, lat], ...]
    }

    public static class Hazard {
        @SerializedName("id")
        public String id;

        @SerializedName("type")
        public String type;

        @SerializedName("severity")
        public String severity;

        @SerializedName("distance")
        public double distance; // km

        @SerializedName("coordinates")
        public List<Double> coordinates; // [lng, lat]
    }
}
