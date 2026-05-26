package com.example.routeguard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.routeguard.data.model.ErrorLog;

import java.util.List;

@Dao
public interface ErrorDao {

    @Insert
    void insert(ErrorLog errorLog);

    @Query("SELECT * FROM error_logs ORDER BY timestamp DESC")
    LiveData<List<ErrorLog>> getAllErrors();

    @Query("DELETE FROM error_logs")
    void clearAll();
}