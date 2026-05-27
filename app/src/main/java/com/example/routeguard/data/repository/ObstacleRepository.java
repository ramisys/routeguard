package com.example.routeguard.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.routeguard.data.local.ObstacleDao;
import com.example.routeguard.data.local.RouteGuardDatabase;
import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.network.ApiService;
import com.example.routeguard.network.NearbyObstaclesResponse;
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
                .enqueue(new Callback<NearbyObstaclesResponse>() {
                    @Override
                    public void onResponse(Call<NearbyObstaclesResponse> call,
                                           Response<NearbyObstaclesResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            NearbyObstaclesResponse body = response.body();
                            android.util.Log.d("ObstacleRepository", "Response success: " + body.success);
                            
                            List<Obstacle> obstacles = body.obstacles;
                            if (obstacles == null) {
                                android.util.Log.w("ObstacleRepository", "Response successful but obstacles list is null");
                                return;
                            }
                            android.util.Log.d("ObstacleRepository", "Fetched " + obstacles.size() + " obstacles from server");
                            
                            for (Obstacle o : obstacles) {
                                o.syncCoordinates();
                                // Debug log for first few obstacles
                                if (obstacles.indexOf(o) < 3) {
                                    android.util.Log.d("ObstacleRepository", "Obstacle " + o.getId() + " at " + o.getLat() + ", " + o.getLon());
                                }
                                
                                // Ensure they are active if they come from the 'nearby' endpoint
                                if (!o.isActive()) {
                                    o.setActive(true);
                                }
                            }
                            // Save to local DB on background thread
                            executor.execute(() -> {
                                try {
                                    obstacleDao.insertAll(obstacles);
                                    android.util.Log.d("ObstacleRepository", "Successfully cached " + obstacles.size() + " obstacles to DB");
                                } catch (Exception e) {
                                    android.util.Log.e("ObstacleRepository", "Failed to cache obstacles: " + e.getMessage());
                                }
                            });
                        } else {
                            android.util.Log.e("ObstacleRepository", "Fetch failed: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<NearbyObstaclesResponse> call,
                                          Throwable t) {
                        android.util.Log.e("ObstacleRepository", "Network failure fetching obstacles: " + t.getMessage());
                        t.printStackTrace();
                    }
                });
    }

    // --- Local DB writes (must run on background thread) ---

    public void insert(Obstacle obstacle) {
        obstacle.syncCoordinates();
        executor.execute(() -> obstacleDao.insert(obstacle));
    }

    public void deactivate(String obstacleId) {
        executor.execute(() -> obstacleDao.deactivateObstacle(obstacleId));
    }

    public void deleteAll() {
        executor.execute(obstacleDao::deleteAll);
    }
}