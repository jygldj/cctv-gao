package com.daoxuan.cctv;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.Arrays;

import com.daoxuan.cctv.databinding.ActivityDouyinBinding;
import com.daoxuan.cctv.impl.WebViewClientImpl;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.utils.ToastUtils;

public class DouyinActivity extends Activity {
    protected String TAG = "DouyinActivity";
    private Context thisContext;
    protected WebView lWebView;
    protected ActivityDouyinBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_douyin);
        bind();
        thisContext=this;
        initWebView();
        lWebView.requestFocus();
        binding.webviewWrapper.requestFocus();
        lWebView.loadUrl("https://www.douyin.com/?recommend=1");
        ToastUtils.show(this,"已支持遥控器上下可快速切台",Toast.LENGTH_SHORT);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
    }
    private void bind(){
        binding = DataBindingUtil.setContentView(this, R.layout.activity_douyin);
        //binding.setMenuTitleHandler(new BaseWebViewActivity.MenuTitleHandler());
        ViewGroup container = binding.webviewWrapper;
        lWebView = new WebView(this);
        container.addView(lWebView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        // lWebView=binding.webView;
        //focusChange();
    }
    protected void initWebView() {
        WebSettings webSetting = lWebView.getSettings();
        webSetting.setJavaScriptEnabled(true);
        webSetting.setAllowFileAccess(true);
        webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        //webSetting.setNeedInitialFocus(false);
        webSetting.setSupportZoom(false);
        webSetting.setBuiltInZoomControls(false);
        webSetting.setDisplayZoomControls(false);
        webSetting.setUseWideViewPort(true);
        //webSetting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSetting.setLoadWithOverviewMode(true);
        webSetting.setMixedContentMode(WebSettings.LOAD_NORMAL);
        //app cache
        //webSetting.setAppCacheEnabled(true);
        webSetting.setMediaPlaybackRequiresUserGesture(false);
        String userAgent=webSetting.getUserAgentString();
        //"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
        webSetting.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        //webSetting.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
        //normal?
        webSetting.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSetting.setJavaScriptCanOpenWindowsAutomatically(false);
        webSetting.setGeolocationEnabled(false);
        webSetting.setBlockNetworkImage(true);
        lWebView.setWebViewClient(new WebViewClientImpl(getBaseContext(),lWebView,2));
        initWebChromeClient();
        lWebView.setScrollContainer(false);
        lWebView.setVerticalScrollBarEnabled(false);
        lWebView.setHorizontalScrollBarEnabled(false);

        WebView.setWebContentsDebuggingEnabled(true);
        // mWebView.setFocusable(false);
        //mWebView.setFocusableInTouchMode(false);
        //mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //lWebView.addJavascriptInterface(new LiveActivity.JsInterface(),"_api");
    }
    private void initWebChromeClient() {
        lWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                LogUtil.i("WebChromeClient","onShowCustomView");
                binding.fullscreen.addView(view);
                binding.fullscreen.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                LogUtil.i("WebChromeClient","onPermissionRequest "+request.getOrigin());
                LogUtil.i("WebChromeClient",request.getOrigin()+" "+ Arrays.toString(request.getResources()));
                request.deny();
            }
            @Override
            public void onHideCustomView() {
                LogUtil.i("WebChromeClient","onHideCustomView");
                binding.fullscreen.removeAllViews();
                binding.fullscreen.setVisibility(View.GONE);
            }
        });
    }
}