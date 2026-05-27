package com.example.routeguard.network;

import com.example.routeguard.data.model.Obstacle;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NearbyObstaclesResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("obstacles")
    public List<Obstacle> obstacles;
}
