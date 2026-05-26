package com.example.routeguard.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "error_logs")
public class ErrorLog {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private long timestamp;
    private String tag;
    private String message;
    private String stackTrace;

    public ErrorLog(long timestamp, String tag, String message, String stackTrace) {
        this.timestamp = timestamp;
        this.tag = tag;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public String getTag() { return tag; }
    public String getMessage() { return message; }
    public String getStackTrace() { return stackTrace; }
}