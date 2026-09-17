package com.daoxuan.cctv.impl;

import android.view.View;

import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.Arrays;

import com.daoxuan.cctv.util.LogUtil;

public class WebChromeClientImpl extends WebChromeClient {
    private static String TAG="WebChromeClient";


    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        LogUtil.i(TAG, "onProgressChanged, newProgress:" + newProgress + ", view:" + view);
    }

    @Override
    public boolean onJsAlert(WebView webView, String url, String message, JsResult result) {
        LogUtil.i(TAG,"onJsAlert "+url);

        return true;
    }

    @Override
    public boolean onJsConfirm(WebView webView, String url, String message, JsResult result) {
        LogUtil.i(TAG,"onJsConfirm "+url);

        return true;
    }

    @Override
    public boolean onJsBeforeUnload(WebView webView, String url, String message, JsResult result) {
        LogUtil.i(TAG,"onJsBeforeUnload "+url);

        return true;
    }

    @Override
    public boolean onJsPrompt(WebView webView, String url, String message, String defaultValue, JsPromptResult result) {
        LogUtil.i(TAG,"onJsPrompt");
                /*final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);*/

        return true;
    }

    @Override
    public void onPermissionRequest(PermissionRequest request) {
        LogUtil.i(TAG,"onPermissionRequest "+request.getOrigin());
            LogUtil.i(TAG,request.getOrigin()+" "+ Arrays.toString(request.getResources()));
            request.deny();
            //request.grant(request.getResources());
    }
    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        LogUtil.i(TAG,"onShowCustomView ");
    }
}
