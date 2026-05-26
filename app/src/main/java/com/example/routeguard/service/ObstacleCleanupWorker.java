package com.example.routeguard.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.routeguard.data.local.ObstacleDao;
import com.example.routeguard.data.local.RouteGuardDatabase;
import com.example.routeguard.util.AppLogger;

public class ObstacleCleanupWorker extends Worker {

    public ObstacleCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            ObstacleDao dao = RouteGuardDatabase.getInstance(getApplicationContext()).obstacleDao();
            long now = System.currentTimeMillis();
            
            // This is a new query we'll add to deactivate expired obstacles
            dao.deactivateExpiredObstacles(now);
            
            AppLogger.init(getApplicationContext()); // Ensure logger is ready
            Log.d("CleanupWorker", "Expired obstacles cleaned up at " + now);
            
            return Result.success();
        } catch (Exception e) {
            AppLogger.e("CLEANUP", "Failed to clean up expired obstacles", e);
            return Result.retry();
        }
    }
}