package com.example.routeguard.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.routeguard.R;
import com.example.routeguard.data.local.ObstacleDao;
import com.example.routeguard.data.local.RouteGuardDatabase;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.network.ApiService;
import com.example.routeguard.network.NearbyObstaclesResponse;
import com.example.routeguard.network.RetrofitClient;
import com.example.routeguard.util.DistanceUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProximityAlertService extends Service {
    private static final String TAG = "ProximityAlertService";
    private static final String CHANNEL_ID = "PROXIMITY_ALERTS";
    private static final int NOTIF_ID = 1001;
    
    // Radiuses
    private static final double RADIUS_INFO = 500.0;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ObstacleDao obstacleDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AlertManager alertManager;
    private ApiService apiService;
    
    private Location lastFetchedLocation;
    private long lastFetchTime = 0;
    private static final long FETCH_COOLDOWN_MS = 30000; // 30 seconds
    private static final float FETCH_DISTANCE_THRESHOLD = 100; // 100 meters

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        obstacleDao = RouteGuardDatabase.getInstance(this).obstacleDao();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        alertManager = new AlertManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    checkProximity(location);
                    checkApiUpdates(location);
                }
            }
        };

        startForeground(NOTIF_ID, createForegroundNotification());
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
        }
    }

    private void checkProximity(Location userLocation) {
        executor.execute(() -> {
            // Check local DB first for quick response
            double delta = 0.005; // ~500m
            List<Obstacle> nearby = obstacleDao.getNearbyObstaclesSync(
                    userLocation.getLatitude() - delta, userLocation.getLatitude() + delta,
                    userLocation.getLongitude() - delta, userLocation.getLongitude() + delta);

            if (nearby != null) {
                Log.d(TAG, "Found " + nearby.size() + " active obstacles in DB within bounding box");
                for (Obstacle o : nearby) {
                    double dist = DistanceUtils.calculateDistance(
                            userLocation.getLatitude(), userLocation.getLongitude(),
                            o.getLat(), o.getLon());
                    
                    Log.d(TAG, "Obstacle " + o.getId() + " (" + o.getType() + ") at distance: " + dist + "m");
                    
                    if (dist <= RADIUS_INFO) {
                        Log.i(TAG, "Obstacle within radius! Processing alert for: " + o.getId());
                        alertManager.processObstacle(o, dist);
                    }
                }
            } else {
                Log.d(TAG, "No nearby obstacles found in DB within bounding box");
            }
        });
    }

    private void checkApiUpdates(Location location) {
        long now = System.currentTimeMillis();
        float distFromLastFetch = lastFetchedLocation != null ? location.distanceTo(lastFetchedLocation) : Float.MAX_VALUE;

        if (now - lastFetchTime > FETCH_COOLDOWN_MS || distFromLastFetch > FETCH_DISTANCE_THRESHOLD) {
            lastFetchTime = now;
            lastFetchedLocation = location;
            fetchNearbyFromApi(location);
        }
    }

    private void fetchNearbyFromApi(Location location) {
        apiService.getNearbyObstacles(location.getLatitude(), location.getLongitude(), (int) RADIUS_INFO)
                .enqueue(new Callback<NearbyObstaclesResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<NearbyObstaclesResponse> call, @NonNull Response<NearbyObstaclesResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().success) {
                            List<Obstacle> obstacles = response.body().obstacles;
                            if (obstacles != null) {
                                executor.execute(() -> {
                                    for (Obstacle o : obstacles) {
                                        o.syncCoordinates();
                                        obstacleDao.insert(o); // Sync to local DB
                                        
                                        double dist = DistanceUtils.calculateDistance(
                                                location.getLatitude(), location.getLongitude(),
                                                o.getLat(), o.getLon());
                                        
                                        if (dist <= RADIUS_INFO) {
                                            alertManager.processObstacle(o, dist);
                                        }
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<NearbyObstaclesResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Failed to fetch nearby obstacles from API", t);
                    }
                });
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RouteGuard Active")
                .setContentText("Monitoring for nearby hazards...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Proximity Alerts", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (alertManager != null) {
            alertManager.resetAlertCooldowns();
        }
        
        // Immediate check on start/re-entry using last known location
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d(TAG, "Triggering immediate proximity check from onStartCommand");
                    checkProximity(location);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (alertManager != null) {
            alertManager.shutdown();
        }
        executor.shutdown();
    }
}
