package com.example.routeguard.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePrefsManager {

    private static final String PREF_NAME = "routeguard_secure_prefs";
    private static SecurePrefsManager instance;
    private SharedPreferences sharedPreferences;

    private SecurePrefsManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            AppLogger.e("SecurePrefsManager", "Error initializing EncryptedSharedPreferences", e);
        }
    }

    public static synchronized SecurePrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SecurePrefsManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveString(String key, String value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(key, value).apply();
        }
    }

    public String getString(String key, String defaultValue) {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public void remove(String key) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().remove(key).apply();
        }
    }
}
