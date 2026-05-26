package com.example.routeguard.ui.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.data.repository.ObstacleRepository;

import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private final ObstacleRepository repository;

    // LiveData the UI observes
    private final LiveData<List<Obstacle>> nearbyObstacles;

    // Current user location (updated by GPS)
    private final MutableLiveData<double[]> userLocation =
            new MutableLiveData<>();

    // Loading state for showing/hiding progress
    private final MutableLiveData<Boolean> isLoading =
            new MutableLiveData<>(false);

    // Error messages
    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

    // Default to Tacloban area
    private double currentLat = 11.2543;
    private double currentLon = 124.9999;

    public MapViewModel(@NonNull Application application) {
        super(application);
        repository = new ObstacleRepository(application);

        // 0.05 degrees ≈ 5km radius
        nearbyObstacles = repository.getNearbyObstacles(
                currentLat, currentLon, 0.05);
    }

    public LiveData<List<Obstacle>> getNearbyObstacles() {
        return nearbyObstacles;
    }

    public LiveData<double[]> getUserLocation() {
        return userLocation;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Called when GPS updates user position
    public void updateUserLocation(double lat, double lon) {
        currentLat = lat;
        currentLon = lon;
        userLocation.setValue(new double[]{lat, lon});
        refreshObstacles();
    }

    // Fetch fresh data from network → caches in Room
    public void refreshObstacles() {
        isLoading.setValue(true);
        repository.fetchAndCacheObstacles(currentLat, currentLon);
        isLoading.setValue(false);
    }

    public void deactivateObstacle(String id) {
        repository.deactivate(id);
    }
}