package com.daoxuan.cctv.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


public class ToastUtils {
    private static final String TAG = "ToastUtils";
    private static final Map<Integer, Toast> toastMap = new HashMap<>();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());


    public static void showShort(Context context, String message) {
        show(context, message, Toast.LENGTH_SHORT);
    }


    public static void showLong(Context context, String message) {
        show(context, message, Toast.LENGTH_LONG);
    }


    public static void show(final Context context, final String message, final int duration) {
        if (context == null) return;
        
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> show(context, message, duration));
            return;
        }

        try {
            if (!Looper.getMainLooper().getThread().isAlive()) {
                Log.e(TAG, "Cannot show toast: Main Looper not alive");
                return;
            }
            
            final Context appContext = context.getApplicationContext();
            
            final int key = (message + duration).hashCode();
            
            if (toastMap.containsKey(key)) {
                try {
                    toastMap.get(key).cancel();
                } catch (Exception e) {
                    Log.e(TAG, "Error canceling previous toast: " + e.getMessage());
                }
                toastMap.remove(key);
            }
            
            Toast toast = Toast.makeText(appContext, message, duration);
            toastMap.put(key, toast);
            
            toast.show();
            
            mainHandler.postDelayed(() -> {
                if (toastMap.containsKey(key)) {
                    try {
                        toastMap.get(key).cancel();
                    } catch (Exception e) {
                    }
                    toastMap.remove(key);
                }
            }, duration == Toast.LENGTH_SHORT ? 2000 : 3500);
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), e);
        }
    }


    public static void cancelAll() {
        try {
            for (Toast toast : toastMap.values()) {
                try {
                    toast.cancel();
                } catch (Exception e) {
                    Log.e(TAG, "Error canceling toast: " + e.getMessage());
                }
            }
            toastMap.clear();
        } catch (Exception e) {
            Log.e(TAG, "Error canceling all toasts: " + e.getMessage());
        }
    }
} 