package com.daoxuan.cctv.util;

import android.os.Handler;
import android.os.Looper;

public final class WebViewDispatcher {
    public interface LoadUrlCallback {
        void accept(String url);
    }

    private static volatile LoadUrlCallback loadUrlCallback;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WebViewDispatcher() {}

    public static void registerLoadUrlCallback(LoadUrlCallback callback) {
        loadUrlCallback = callback;
    }

    public static void unregister() {
        loadUrlCallback = null;
    }

    public static void loadUrl(final String url) {
        final LoadUrlCallback callback = loadUrlCallback;
        if (callback == null) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.accept(url);
                } catch (Throwable ignore) {}
            }
        });
    }
}