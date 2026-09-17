package com.daoxuan.cctv;

import static com.daoxuan.cctv.util.PermissionUtil.REQUEST_EXTERNAL_STORAGE;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.databinding.ViewDataBinding;

import java.util.HashMap;
import java.util.Map;

import com.daoxuan.cctv.impl.WebViewClientImpl;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.utils.ToastUtils;

public abstract class BaseActivity extends Activity {
    protected String TAG = "BaseActivity";
    private boolean isWebViewDestroyed = false;
    protected WebView mWebView;
    protected Context thisContext;
    protected ViewDataBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        getWindow().getDecorView().setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        );
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                getWindow().getDecorView().setImportantForContentCapture(
                        View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
                );
            } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        initWebViewFallback();
        createInit();
        thisContext = this;
        if (getActionBar() != null) {
            getActionBar().hide();
        }
    }

    private void initWebViewFallback() {
        mWebView = new WebView(this);
    }

    protected abstract void createInit();

    protected abstract void initWebChromeClient();

    protected void initWebView() {
        WebSettings webSetting = mWebView.getSettings();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                mWebView.setImportantForContentCapture(
                        View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
                );
            } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
            }
        }
        webSetting.setJavaScriptEnabled(true);
        webSetting.setAllowFileAccess(true);
        // 页面以 file:// 方式加载，需要跨域请求各电视台接口，因此保留这两项
        webSetting.setAllowFileAccessFromFileURLs(true);
        webSetting.setAllowUniversalAccessFromFileURLs(true);
        webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setNeedInitialFocus(false);
        webSetting.setSupportZoom(false);
        webSetting.setBuiltInZoomControls(false);
        webSetting.setDisplayZoomControls(false);
        webSetting.setUseWideViewPort(true);
        webSetting.setLoadWithOverviewMode(true);
        webSetting.setMixedContentMode(WebSettings.LOAD_NORMAL);
        webSetting.setMediaPlaybackRequiresUserGesture(false);
        webSetting.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        webSetting.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSetting.setJavaScriptCanOpenWindowsAutomatically(false);
        webSetting.setGeolocationEnabled(false);
        /* 电视端不需要多窗口与缩放回弹：关掉可省下页面加载期的一批开销，
           也避免遥控器误触时页面跟着滑动。 */
        webSetting.setSupportMultipleWindows(false);
        webSetting.setDefaultTextEncodingName("UTF-8");
        initWebViewClient();
        initWebChromeClient();
        mWebView.setScrollContainer(false);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        try {
            mWebView.setBackgroundColor(0xFF121212);
        } catch (Throwable ignore) {
        }

        /* 调试开关只在 debug 包打开，release 包关闭以免渲染进程多背一份调试开销。 */
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        mWebView.addJavascriptInterface(getJsInterface(), "_api");
    }

    protected abstract Object getJsInterface();

    protected void initWebViewClient() {
        mWebView.setWebViewClient(new WebViewClientImpl(getBaseContext(), mWebView, 0));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            LogUtil.i(TAG, "onRequestPermissionsResult: initWebView");
            if (null != mWebView) {
                initWebView();
            }
        }
    }

    //key event
    private static Map<String, Integer> keyCodeMap = new HashMap<>();

    static {
        keyCodeMap.put("SPACE", 62);
        keyCodeMap.put("F", 34);
        keyCodeMap.put("MENU", 82);
        keyCodeMap.put("82", 82);
    }

    protected void keyCodeAllByCode(String keyCode) {
        Integer keyCodeNum = keyCodeMap.get(keyCode);
        if (null == keyCodeNum) {
            return;
        }
        LogUtil.i("onKeyEvent", "keyCodeStr " + keyCode);
        keyEventAll(keyCodeNum);
    }

    /**
     * 把按键直接派发给 WebView。
     * 原先使用 Instrumentation.sendKeySync，普通应用不具备 INJECT_EVENTS 权限会直接抛异常，
     * 导致 H5 触发的按键注入一直是失效的；改用 dispatchKeyEvent 后才能真正生效。
     */
    protected void keyEventAll(final int keyCode) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mWebView == null || isWebViewDestroyed) {
                        return;
                    }
                    long now = android.os.SystemClock.uptimeMillis();
                    mWebView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
                    mWebView.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
                } catch (Exception e) {
                    LogUtil.e("onKeyEvent", "dispatch key error: " + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyWebView();
    }

    private void destroyWebView() {
        if (mWebView != null && !isWebViewDestroyed) {
            isWebViewDestroyed = true;
            try {
                mWebView.removeJavascriptInterface("_api");
                mWebView.loadUrl("about:blank");
                mWebView.clearHistory();
                ViewParent parent = mWebView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(mWebView);
                }
                mWebView.stopLoading();
                mWebView.clearCache(true);
                mWebView.clearFormData();
                mWebView.clearSslPreferences();
                mWebView.destroy();
                mWebView = null;
            } catch (Exception e) {
                LogUtil.e(TAG, "Error destroying WebView", e);
            }
        }
    }

    /**
     * 回到前台时是否把焦点交给 WebView。
     * 电视上若不主动抢焦点，切回来后遥控器可能没反应。
     */
    protected boolean shouldFocusWebOnResume() {
        return true;
    }

    // 页面加载回调：驱动加载遮罩与失败提示
    private final WebViewClientImpl.PageLoadCallback pageLoadCallback = new WebViewClientImpl.PageLoadCallback() {
        @Override
        public void onStarted(final String url) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onPageLoadStarted(url);
                }
            });
        }

        @Override
        public void onFinished(final String url) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onPageLoadFinished(url);
                }
            });
        }

        @Override
        public void onError(final int code, final String description, final String url) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onPageLoadError(code, description, url);
                }
            });
        }
    };

    protected void onPageLoadStarted(String url) {
    }

    protected void onPageLoadFinished(String url) {
    }

    protected void onPageLoadError(int code, String description, String url) {
        ToastUtils.show(this, "页面加载失败，请检查网络", Toast.LENGTH_SHORT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWebView != null) {
            try {
                mWebView.stopLoading();
            } catch (Exception e) {
                LogUtil.e(TAG, "Error stopping WebView", e);
            }
        }
    }

    @Override
    protected void onPause() {
        WebViewClientImpl.setPageLoadCallback(null);
        if (mWebView != null) {
            try {
                mWebView.onPause();
            } catch (Exception e) {
                LogUtil.e(TAG, "Error pausing WebView", e);
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebViewClientImpl.setPageLoadCallback(pageLoadCallback);
        if (mWebView != null && !isWebViewDestroyed) {
            try {
                mWebView.onResume();
                // 不再在暂停时关闭 JS：关闭后再回来，H5 播放器和页面状态会失效
                mWebView.getSettings().setJavaScriptEnabled(true);
                if (shouldFocusWebOnResume()) {
                    mWebView.requestFocus();
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "Error resuming WebView", e);
            }
        }
    }
}
