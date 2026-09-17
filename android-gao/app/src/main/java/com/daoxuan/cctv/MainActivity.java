package com.daoxuan.cctv;

import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.daoxuan.cctv.databinding.DialogExitMainBinding;
import com.daoxuan.cctv.impl.WebViewClientImpl;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.util.ValueUtil;
import com.daoxuan.cctv.utils.ToastUtils;
import com.daoxuan.cctv.util.WebViewDispatcher;
import com.daoxuan.cctv.util.DataCleanManager;

public class MainActivity extends BaseWebViewActivity {
    private long mClickBackTime = 0;
    private DialogExitMainBinding exitDialogBinding;
    private boolean isExitDialogShowing = false;
    /**
     * 退出确认框正在显示时，不要把焦点抢回 WebView，否则遥控器选不中按钮。
     */
    @Override
    protected boolean shouldFocusWebOnResume() {
        return !isExitDialogShowing;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            float x= event.getX();
            float y= event.getY();
            //LogUtil.i("dispatchTouchEvent", "x" + x+"y "+y);
            if(x<100f&&y<100f) {
                ctrl("menu");
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean ctrl(String code){
        if (mWebView != null) {
            String  js= "_menuCtrl."+code+"()";
            LogUtil.i(TAG,js);
            mWebView.evaluateJavascript(js,null);
        }
        return true;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LogUtil.i(TAG,"onConfigurationChanged...."+newConfig.orientation);
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebViewDispatcher.registerLoadUrlCallback(new WebViewDispatcher.LoadUrlCallback() {
            @Override
            public void accept(String u) {
                if (mWebView != null) {
                    mWebView.loadUrl(u);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        WebViewDispatcher.unregister();
        super.onPause();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event);
        }
        int keyCode = event.getKeyCode();
        LogUtil.i("keyDown keyCode ", keyCode+" event" + event);
        
        if(isExitDialogShowing){
            if(keyCode==KeyEvent.KEYCODE_BACK){
                finish();
                return true;
            }
            if(keyCode==KeyEvent.KEYCODE_DPAD_UP || keyCode==KeyEvent.KEYCODE_DPAD_DOWN || 
               keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_ENTER){
                return super.dispatchKeyEvent(event);
            }
            return true;
        }

        String liveUrl = null;
        try { liveUrl = mWebView.getUrl(); } catch (Exception ignore) { /* noop */ }
        if (liveUrl != null && liveUrl.contains("live.html")) {
            int webCode = -1;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER: webCode = 23; break;
                case KeyEvent.KEYCODE_ENTER:       webCode = 13; break;
                case KeyEvent.KEYCODE_DPAD_LEFT:   webCode = 37; break;
                case KeyEvent.KEYCODE_DPAD_UP:     webCode = 38; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:  webCode = 39; break;
                case KeyEvent.KEYCODE_DPAD_DOWN:   webCode = 40; break;
                default: webCode = -1;
            }
            if (webCode >= 0) {
                final int code = webCode;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        mWebView.evaluateJavascript("if(window.__cctvKey)window.__cctvKey(" + code + ");", null);
                    }
                });
                return true;
            }
        }

        boolean isMenuShow=isMenuShow();
        if(isMenuShow){
            if(keyCode==KeyEvent.KEYCODE_BACK||keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_TAB){
                hideMenu();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        if(keyCode==KeyEvent.KEYCODE_BACK){
            return keyBack();
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_CENTER||keyCode==KeyEvent.KEYCODE_ENTER){
            return ctrl("ok");
        }
        if(keyCode==KeyEvent.KEYCODE_MENU||keyCode==KeyEvent.KEYCODE_TAB){
            return ctrl("menu");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){
            return ctrl("right");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
            return ctrl("left");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            return ctrl("down");
        }
        if(keyCode==KeyEvent.KEYCODE_DPAD_UP){
            return ctrl("up");
        }
        if(keyCode==KeyEvent.KEYCODE_VOLUME_UP||keyCode==KeyEvent.KEYCODE_VOLUME_DOWN
                ||keyCode==KeyEvent.KEYCODE_VOLUME_MUTE){
            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }
    private boolean keyBack(){
        if (isExitDialogShowing) {
            finish();
            return true;
        }
        
        String url = WebViewClientImpl.backUrl();
        LogUtil.i("keyBack","keyBack "+url);
        if(null!=url&&null!=mWebView){
            if("$$GOBACK$$".equals(url)){
                return true;
            }
            mWebView.loadUrl(url);
            return true;
        }
        
        showExitDialog();
        return true;
    }
    
    private void showExitDialog() {
        if (exitDialogBinding == null) {
            initExitDialog();
        }
        isExitDialogShowing = true;
        exitDialogBinding.exitDialogContainer.setVisibility(View.VISIBLE);
        
        exitDialogBinding.btnCancel.setFocusable(true);
        exitDialogBinding.btnStartToggle.setFocusable(true);
        
        String currentStartPage = ValueUtil.getString(this, "startPage", "main");
        if ("main".equals(currentStartPage)) {
            exitDialogBinding.btnStartToggle.setText("启动即电视直播");
        } else {
            exitDialogBinding.btnStartToggle.setText("启动即视频点播");
        }
        
        exitDialogBinding.btnCancel.post(() -> exitDialogBinding.btnCancel.requestFocus());

        try {
            exitDialogBinding.btnCancel.setFocusable(true);
            exitDialogBinding.btnCancel.setFocusableInTouchMode(true);
            exitDialogBinding.btnCancel.post(() -> exitDialogBinding.btnCancel.requestFocus());
        } catch (Throwable ignore) {}





    }
    
    private void hideExitDialog() {
        if (exitDialogBinding != null) {
            isExitDialogShowing = false;
            exitDialogBinding.exitDialogContainer.setVisibility(View.GONE);
        }
    }
    
    private void initExitDialog() {
        View dialogView = findViewById(R.id.exitDialog);
        exitDialogBinding = DataBindingUtil.bind(dialogView);
        
        if (exitDialogBinding == null) {
            return;
        }

        exitDialogBinding.btnCancel.setOnClickListener(v -> {
            hideExitDialog();
        });
        
        
        exitDialogBinding.btnStartToggle.setOnClickListener(v -> {
            String currentStartPage = ValueUtil.getString(this, "startPage", "main");
            if ("main".equals(currentStartPage)) {
                ValueUtil.putString(this, "startPage", "live");
                ToastUtils.show(this, "已设置启动首页为：电视直播", Toast.LENGTH_SHORT);
                exitDialogBinding.btnStartToggle.setText("启动即视频点播");
            } else {
                ValueUtil.putString(this, "startPage", "main");
                ToastUtils.show(this, "已设置启动首页为：视频点播", Toast.LENGTH_SHORT);
                exitDialogBinding.btnStartToggle.setText("启动即电视直播");
            }
        });





        try {
            exitDialogBinding.btnClearCache.setOnClickListener(v -> {
                DataCleanManager.cleanInternalCache(this);
                DataCleanManager.cleanExternalCache(this);
                exitDialogBinding.btnClearCache.setText("缓存大小 " + DataCleanManager.getCacheSize(this) + " 点击清理缓存");
                ToastUtils.show(this, "清理缓存成功", Toast.LENGTH_SHORT);
            });
        } catch (Throwable ignore) {}

        try {
            exitDialogBinding.btnExitApp.setOnClickListener(v -> {
                finishAffinity();
            });
        } catch (Throwable ignore) {}
    }

    
}
