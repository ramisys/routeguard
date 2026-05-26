package com.example.routeguard.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RoutingService {

    // OSRM Public API (for demo purposes)
    @GET("route/v1/driving/{coords}")
    Call<OsrmResponse> getRoute(
            @Path("coords") String coordinates,
            @Query("overview") String overview,
            @Query("geometries") String geometries,
            @Query("steps") boolean steps
    );

    class OsrmResponse {
        @SerializedName("routes")
        public List<Route> routes;
    }

    class Route {
        @SerializedName("geometry")
        public String geometry;
        @SerializedName("duration")
        public double duration;
        @SerializedName("distance")
        public double distance;
    }
}