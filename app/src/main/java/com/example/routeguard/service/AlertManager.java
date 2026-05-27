package com.example.routeguard.service;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.routeguard.R;
import com.example.routeguard.data.model.Obstacle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AlertManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "AlertManager";
    private static final String CHANNEL_ID = "PROXIMITY_ALERTS";
    private static final long COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes cooldown for same obstacle

    private final Context context;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;

    // Track last alert for each obstacle: ID -> LastAlertInfo
    private final Map<String, LastAlertInfo> alertedObstacles = new HashMap<>();

    public AlertManager(Context context) {
        this.context = context;
        this.tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                ttsInitialized = true;
            }
        } else {
            Log.e(TAG, "TTS Initialization failed");
        }
    }

    public void resetAlertCooldowns() {
        alertedObstacles.clear();
        Log.d(TAG, "Alert cooldowns reset");
    }

    public void processObstacle(Obstacle obstacle, double distance) {
        String id = obstacle.getId();
        String currentSeverity = obstacle.getSeverity();
        long currentTime = System.currentTimeMillis();

        LastAlertInfo lastInfo = alertedObstacles.get(id);

        boolean shouldAlert = false;
        if (lastInfo == null) {
            shouldAlert = true;
        } else if (!lastInfo.severity.equals(currentSeverity)) {
            // Severity changed, alert again
            shouldAlert = true;
        } else if (currentTime - lastInfo.timestamp > COOLDOWN_MS) {
            // Cooldown passed, alert again if still in range
            shouldAlert = true;
        }

        if (shouldAlert) {
            showAlert(obstacle, distance);
            alertedObstacles.put(id, new LastAlertInfo(currentSeverity, currentTime));
        }
    }

    private void showAlert(Obstacle obstacle, double distance) {
        String type = obstacle.getType();
        String road = obstacle.getRoadName() != null ? " on " + obstacle.getRoadName() : "";
        
        String prefix = "Information: ";
        if (distance <= 100) prefix = "DANGER: ";
        else if (distance <= 300) prefix = "Warning: ";

        String message = String.format(Locale.getDefault(), "%s%s detected %.0fm ahead%s", prefix, type, distance, road);
        Log.i(TAG, "Showing Alert: " + message);

        // 1. Notification
        sendNotification(obstacle, message, distance);

        // 2. Voice Alert
        if (ttsInitialized) {
            Log.d(TAG, "Speaking: " + message);
            tts.speak(message, TextToSpeech.QUEUE_ADD, null, obstacle.getId());
        } else {
            Log.w(TAG, "TTS not initialized yet. Skipping voice alert.");
        }
    }

    private void sendNotification(Obstacle obstacle, String message, double distance) {
        int priority = NotificationCompat.PRIORITY_DEFAULT;
        int color = context.getResources().getColor(android.R.color.holo_blue_light);

        if (distance <= 100) {
            priority = NotificationCompat.PRIORITY_HIGH;
            color = context.getResources().getColor(android.R.color.holo_red_dark);
        } else if (distance <= 300) {
            color = context.getResources().getColor(android.R.color.holo_orange_light);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(obstacle.getType() + " Alert")
                .setContentText(message)
                .setPriority(priority)
                .setColor(color)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(obstacle.getId().hashCode(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission missing for notification", e);
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private static class LastAlertInfo {
        String severity;
        long timestamp;

        LastAlertInfo(String severity, long timestamp) {
            this.severity = severity;
            this.timestamp = timestamp;
        }
    }
}
