package com.example.routeguard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.routeguard.data.model.Obstacle;

import java.util.List;

@Dao
public interface ObstacleDao {

    // Insert one or many obstacles (replaces if same ID exists)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Obstacle> obstacles);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Obstacle obstacle);

    // Get all active obstacles as LiveData (auto-updates UI when data changes)
    @Query("SELECT * FROM obstacles WHERE isActive = 1")
    LiveData<List<Obstacle>> getAllActiveObstacles();

    // Get obstacles near a location (within bounding box) - Synchronous for Background Worker/Service
    @Query("SELECT * FROM obstacles WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon AND isActive = 1")
    List<Obstacle> getNearbyObstaclesSync(
            double minLat, double maxLat,
            double minLon, double maxLon);

    // Get obstacles near a location (within bounding box)
    @Query("SELECT * FROM obstacles WHERE lat BETWEEN :minLat AND :maxLat AND lon BETWEEN :minLon AND :maxLon AND isActive = 1")
    LiveData<List<Obstacle>> getNearbyObstacles(
            double minLat, double maxLat,
            double minLon, double maxLon);

    // Get single obstacle by ID
    @Query("SELECT * FROM obstacles WHERE id = :id")
    LiveData<Obstacle> getObstacleById(String id);

    // Get by type (FLOOD, ACCIDENT, etc)
    @Query("SELECT * FROM obstacles WHERE type = :type AND isActive = 1")
    LiveData<List<Obstacle>> getObstaclesByType(String type);

    @Update
    void update(Obstacle obstacle);

    @Delete
    void delete(Obstacle obstacle);

    // Mark obstacle as inactive instead of deleting
    @Query("UPDATE obstacles SET isActive = 0 WHERE id = :id")
    void deactivateObstacle(String id);

    // Auto-cleanup for expired obstacles
    @Query("UPDATE obstacles SET isActive = 0 WHERE expiresAt < :currentTime AND isActive = 1")
    void deactivateExpiredObstacles(long currentTime);

    // Clear all obstacles (used when refreshing from network)
    @Query("DELETE FROM obstacles")
    void deleteAll();

    // Count total obstacles
    @Query("SELECT COUNT(*) FROM obstacles WHERE isActive = 1")
    int getActiveCount();
}