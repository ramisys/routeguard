package com.example.routeguard.ui.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.data.repository.ObstacleRepository;

import java.util.List;

public class MapViewModel extends AndroidViewModel {

    private final ObstacleRepository repository;
    private final LiveData<List<Obstacle>> nearbyObstacles;
    private final MutableLiveData<double[]> currentLocation = new MutableLiveData<>(new double[]{11.2543, 124.9999});

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MapViewModel(@NonNull Application application) {
        super(application);
        repository = new ObstacleRepository(application);

        // Different Approach: Observe ALL active obstacles in the database.
        // This eliminates coordinate-range query errors.
        nearbyObstacles = repository.getAllActiveObstacles();
    }

    public LiveData<List<Obstacle>> getNearbyObstacles() {
        return nearbyObstacles;
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void updateUserLocation(double lat, double lon) {
        currentLocation.setValue(new double[]{lat, lon});
        refreshObstacles();
    }

    public void refreshObstacles() {
        isLoading.setValue(true);
        double[] loc = currentLocation.getValue();
        if (loc != null) {
            android.util.Log.d("MapViewModel", "Refreshing obstacles at: " + loc[0] + ", " + loc[1]);
            repository.fetchAndCacheObstacles(loc[0], loc[1]);
        } else {
            android.util.Log.w("MapViewModel", "Cannot refresh obstacles: currentLocation is null");
        }
        isLoading.setValue(false);
    }

    public void deactivateObstacle(String id) {
        repository.deactivate(id);
    }
}