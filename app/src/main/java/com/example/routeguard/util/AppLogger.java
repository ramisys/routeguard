package com.example.routeguard.util;

import android.content.Context;
import android.util.Log;

import com.example.routeguard.data.local.RouteGuardDatabase;
import com.example.routeguard.data.model.ErrorLog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppLogger {

    private static final String DEFAULT_TAG = "RouteGuard";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static RouteGuardDatabase database;

    public static void init(Context context) {
        database = RouteGuardDatabase.getInstance(context);
    }

    public static void e(String message, Throwable throwable) {
        e(DEFAULT_TAG, message, throwable);
    }

    public static void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);

        if (database != null) {
            executor.execute(() -> {
                String stackTrace = "";
                if (throwable != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    stackTrace = sw.toString();
                }

                ErrorLog errorLog = new ErrorLog(
                        System.currentTimeMillis(),
                        tag,
                        message,
                        stackTrace
                );
                database.errorDao().insert(errorLog);
            });
        }
    }
}