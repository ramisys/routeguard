package com.example.routeguard.network;

import com.google.gson.annotations.SerializedName;

public class SignatureResponse {
    @SerializedName("signature")
    public String signature;
    
    @SerializedName("timestamp")
    public long timestamp;
    
    @SerializedName("cloud_name")
    public String cloudName;
    
    @SerializedName("api_key")
    public String apiKey;
}
