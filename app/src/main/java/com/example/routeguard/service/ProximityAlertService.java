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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.routeguard.R;
import com.example.routeguard.data.local.ObstacleDao;
import com.example.routeguard.data.local.RouteGuardDatabase;
import com.example.routeguard.data.model.Obstacle;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProximityAlertService extends Service {

    private static final String CHANNEL_ID = "PROXIMITY_ALERTS";
    private static final int NOTIF_ID = 1001;
    private static final float RADIUS_METERS = 500f;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ObstacleDao obstacleDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<String> alertedObstacles = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        obstacleDao = RouteGuardDatabase.getInstance(this).obstacleDao();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    checkProximity(location);
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
            e.printStackTrace();
        }
    }

    private void checkProximity(Location userLocation) {
        executor.execute(() -> {
            // Rough bounding box for 500m (approx 0.0045 degrees)
            double delta = 0.0045;
            List<Obstacle> nearby = obstacleDao.getNearbyObstaclesSync(
                    userLocation.getLatitude() - delta, userLocation.getLatitude() + delta,
                    userLocation.getLongitude() - delta, userLocation.getLongitude() + delta);

            if (nearby != null) {
                for (Obstacle o : nearby) {
                    float[] results = new float[1];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            o.getLat(), o.getLon(), results);
                    
                    if (results[0] < RADIUS_METERS && !alertedObstacles.contains(o.getId())) {
                        sendProximityNotification(o);
                        alertedObstacles.add(o.getId());
                    }
                }
            }
        });
    }

    private void sendProximityNotification(Obstacle obstacle) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(obstacle.getType() + " nearby!")
                .setContentText("A " + obstacle.getType().toLowerCase() + " was reported within 500 meters.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(obstacle.getId().hashCode(), builder.build());
        }
    }

    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RouteGuard Active")
                .setContentText("Monitoring for nearby hazards...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
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
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}