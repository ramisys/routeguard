package com.example.routeguard;

import android.app.Application;

import com.example.routeguard.util.AppLogger;
import com.example.routeguard.util.GlobalExceptionHandler;
import com.example.routeguard.service.ObstacleCleanupWorker;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class RouteGuardApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "ddu5mmcuv"); // Update with your actual Cloud Name
        MediaManager.init(this, config);

        // Initialize logger with app context
        AppLogger.init(this);

        // Set global exception handler
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        scheduleCleanup();
    }

    private void scheduleCleanup() {
        PeriodicWorkRequest cleanupRequest =
                new PeriodicWorkRequest.Builder(ObstacleCleanupWorker.class, 1, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ObstacleCleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
        );
    }
}