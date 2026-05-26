package com.example.routeguard.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.routeguard.data.model.Obstacle;
import com.example.routeguard.data.model.ErrorLog;

@Database(
        entities = { Obstacle.class, ErrorLog.class },
        version = 3,
        exportSchema = false
)
public abstract class RouteGuardDatabase extends RoomDatabase {

    private static volatile RouteGuardDatabase instance;
    private static final String DB_NAME = "routeguard_db";

    // Room generates this automatically from ObstacleDao
    public abstract ObstacleDao obstacleDao();
    public abstract ErrorDao errorDao();

    // Singleton — only one database instance ever created
    public static RouteGuardDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (RouteGuardDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    RouteGuardDatabase.class,
                                    DB_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}