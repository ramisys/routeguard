package com.example.routeguard.data.model;

import com.google.gson.annotations.SerializedName;

public class Comment {
    @SerializedName("id")
    private String id;
    
    @SerializedName("obstacleId")
    private String obstacleId;
    
    @SerializedName("userName")
    private String userName;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("timestamp")
    private long timestamp;

    public Comment(String id, String userName, String text, long timestamp) {
        this.id = id;
        this.userName = userName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}
