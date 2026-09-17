package com.daoxuan.cctv.util;

import android.util.Log;


public class LogUtil {
    private static final String DEFAULT_TAG = "DaoxuanApp";
    private static final String NULL_MESSAGE = "null";


    public static void d(String tag, String message) {
        Log.d(tag != null ? tag : DEFAULT_TAG, message != null ? message : NULL_MESSAGE);
    }


    public static void i(String tag, String message) {
        Log.i(tag != null ? tag : DEFAULT_TAG, message != null ? message : NULL_MESSAGE);
    }


    public static void w(String tag, String message) {
        Log.w(tag != null ? tag : DEFAULT_TAG, message != null ? message : NULL_MESSAGE);
    }


    public static void e(String tag, String message) {
        Log.e(tag != null ? tag : DEFAULT_TAG, message != null ? message : NULL_MESSAGE);
    }


    public static void e(String tag, String message, Throwable throwable) {
        String safeTag = tag != null ? tag : DEFAULT_TAG;
        String safeMessage = message != null ? message : NULL_MESSAGE;

        if (throwable != null) {
            String throwableMessage = throwable.getMessage();
            if (throwableMessage != null) {
                Log.e(safeTag, safeMessage + ": " + throwableMessage, throwable);
            } else {
                Log.e(safeTag, safeMessage + ": " + throwable.getClass().getName(), throwable);
            }
        } else {
            Log.e(safeTag, safeMessage);
        }
    }


    public static void d(String message) {
        d(DEFAULT_TAG, message);
    }


    public static void i(String message) {
        i(DEFAULT_TAG, message);
    }


    public static void w(String message) {
        w(DEFAULT_TAG, message);
    }


    public static void e(String message) {
        e(DEFAULT_TAG, message);
    }


    public static void e(String message, Throwable throwable) {
        e(DEFAULT_TAG, message, throwable);
    }


    public static void exception(String tag, Throwable throwable) {
        if (throwable != null) {
            e(tag, "Exception", throwable);
        } else {
            e(tag, "Unknown exception");
        }
    }


    public static void exception(Throwable throwable) {
        exception(DEFAULT_TAG, throwable);
    }
}