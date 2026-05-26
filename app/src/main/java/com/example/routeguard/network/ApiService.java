package com.example.routeguard.network;

import com.example.routeguard.data.model.Obstacle;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/obstacles/nearby")
    Call<List<Obstacle>> getNearbyObstacles(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radius") int radiusMeters
    );

    @POST("api/reports")
    Call<Obstacle> submitReport(@Body Obstacle report);

    @POST("api/users/fcm-token")
    Call<Void> updateFcmToken(@Query("token") String token);

    @POST("api/obstacles/{id}/confirm")
    Call<Void> confirmObstacle(@Path("id") String obstacleId);
}