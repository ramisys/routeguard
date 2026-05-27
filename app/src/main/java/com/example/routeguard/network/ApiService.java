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
    Call<NearbyObstaclesResponse> getNearbyObstacles(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radius") int radiusMeters
    );

    @POST("api/obstacles")
    Call<Obstacle> submitReport(@Body Obstacle report);

    @POST("api/users/fcm-token")
    Call<Void> updateFcmToken(@Query("token") String token);

    @POST("api/obstacles/{id}/confirm")
    Call<Void> confirmObstacle(@Path("id") String obstacleId);

    @POST("api/obstacles/{id}/clear")
    Call<Void> clearObstacle(@Path("id") String obstacleId);

    @GET("api/users/profile")
    Call<com.example.routeguard.data.model.User> getUserProfile();

    @retrofit2.http.PATCH("api/users/profile")
    Call<com.example.routeguard.data.model.User> updateUserProfile(@Body com.example.routeguard.data.model.User user);

    @GET("api/obstacles/{id}/comments")
    Call<List<com.example.routeguard.data.model.Comment>> getComments(@Path("id") String obstacleId);

    @POST("api/obstacles/{id}/comments")
    Call<com.example.routeguard.data.model.Comment> postComment(@Path("id") String obstacleId, @Body com.example.routeguard.data.model.Comment comment);

    @GET("api/media/sign")
    Call<SignatureResponse> getUploadSignature();

    @GET("api/navigation/routes")
    Call<NavigationResponse> getSafeRoutes(
            @Query("start") String startCoords, // "lng,lat"
            @Query("end") String endCoords      // "lng,lat"
    );
}