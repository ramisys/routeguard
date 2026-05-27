package com.example.routeguard.util;

import androidx.annotation.NonNull;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultHandler;

    public GlobalExceptionHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        AppLogger.e("CRASH", "Uncaught exception in thread " + t.getName(), e);
        
        // Wait a bit for the logger to finish inserting into DB
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        }
    }
}