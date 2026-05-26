package com.example.routeguard.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.routeguard.data.local.ObstacleDao;
import com.example.routeguard.data.local.RouteGuardDatabase;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.network.ApiService;
import com.example.routeguard.network.RetrofitClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ObstacleRepository {

    private final ObstacleDao obstacleDao;
    private final ApiService apiService;

    // Background thread executor — Room cannot run on main thread
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ObstacleRepository(Application application) {
        RouteGuardDatabase db = RouteGuardDatabase.getInstance(application);
        obstacleDao = db.obstacleDao();
        apiService  = RetrofitClient.getInstance().getApiService();
    }

    // --- Local DB reads (returns LiveData, observed by ViewModel) ---

    public LiveData<List<Obstacle>> getAllActiveObstacles() {
        return obstacleDao.getAllActiveObstacles();
    }

    public LiveData<List<Obstacle>> getNearbyObstacles(
            double lat, double lon, double radiusDeg) {
        return obstacleDao.getNearbyObstacles(
                lat - radiusDeg, lat + radiusDeg,
                lon - radiusDeg, lon + radiusDeg);
    }

    public LiveData<Obstacle> getObstacleById(String id) {
        return obstacleDao.getObstacleById(id);
    }

    // --- Network fetch → saves to local DB ---

    public void fetchAndCacheObstacles(double lat, double lon) {
        apiService.getNearbyObstacles(lat, lon, 5000)
                .enqueue(new Callback<List<Obstacle>>() {
                    @Override
                    public void onResponse(Call<List<Obstacle>> call,
                                           Response<List<Obstacle>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Save to local DB on background thread
                            executor.execute(() -> {
                                // REMOVED: obstacleDao.deleteAll(); 
                                // We rely on REPLACE strategy to update existing ones.
                                obstacleDao.insertAll(response.body());
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Obstacle>> call,
                                          Throwable t) {
                        // Network failed — local DB data still shows
                        t.printStackTrace();
                    }
                });
    }

    // --- Local DB writes (must run on background thread) ---

    public void insert(Obstacle obstacle) {
        executor.execute(() -> obstacleDao.insert(obstacle));
    }

    public void deactivate(String obstacleId) {
        executor.execute(() -> obstacleDao.deactivateObstacle(obstacleId));
    }

    public void deleteAll() {
        executor.execute(obstacleDao::deleteAll);
    }
}