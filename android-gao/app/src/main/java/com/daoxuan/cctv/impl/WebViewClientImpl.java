package com.daoxuan.cctv.impl;

import android.content.Context;
import android.graphics.Bitmap;

import android.net.http.SslError;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.daoxuan.cctv.MyApplication;
import com.daoxuan.cctv.util.AppVersionUtils;
import com.daoxuan.cctv.util.ConstantMy;
import com.daoxuan.cctv.util.FileUtil;
import com.daoxuan.cctv.util.HttpUtil;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.util.TplUtil;
import com.daoxuan.cctv.util.Util;

public class WebViewClientImpl extends WebViewClient {

    /**
     * 页面加载状态回调：用于把「加载遮罩」和「失败提示」交给界面处理。
     */
    public interface PageLoadCallback {
        void onStarted(String url);

        void onFinished(String url);

        void onError(int code, String description, String url);
    }

    private static PageLoadCallback sPageLoadCallback;

    public static void setPageLoadCallback(PageLoadCallback callback) {
        sPageLoadCallback = callback;
    }

    private static void notifyStarted(String url) {
        if (sPageLoadCallback != null) {
            sPageLoadCallback.onStarted(url);
        }
    }

    private static void notifyFinished(String url) {
        if (sPageLoadCallback != null) {
            sPageLoadCallback.onFinished(url);
        }
    }

    private static void notifyError(int code, String description, String url) {
        if (sPageLoadCallback != null) {
            sPageLoadCallback.onError(code, description, url);
        }
    }

    private  static  String TAG="WebViewClient";
    private Context context;
    private WebView mWebView;

    /**
     * 注入脚本的内存缓存。
     *
     * 这些脚本在每次页面加载时都会通过 onPageStarted / onPageFinished 注入，
     * 原来每次都走 FileUtil.readExt 从 assets 重新读一遍（assets 是压缩的，
     * 每次 open 都要解压 + 逐行拼串 + 打日志），而且是在 UI 线程上做。
     * 换台越频繁、这套 I/O 越浪费，缓存后只读一次。
     */
    private static String sEndJs;
    private static String sLoadDetailTvJs;
    private static String sLoadDetailVideoJs;
    private static String sCctvFullscreenJs;
    private static String sPageFsBtnJs;
    private static String sPageToastKillJs;

    private static String assetText(String name) {
        try {
            String text = FileUtil.readExt(MyApplication.getAppContext(), "tv-web/" + name);
            return text == null ? "" : text;
        } catch (Exception e) {
            LogUtil.e(TAG, "read asset failed: " + name + " " + e.getMessage());
            return "";
        }
    }

    private static  String lastUrl=null;
    private static  String rootUrl=null;
    private static  String currentUrl=null;
    private static  WebView sWebView=null;
    private int type;
    public WebViewClientImpl(Context context,WebView mWebView,int type){
        this.context=context;
        this.mWebView=mWebView;
        sWebView=mWebView;
        this.type=type;
    }
    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        view.clearCache(false);
        view.clearHistory();
        return true;
    }

    @Override
    public void onReceivedSslError(WebView webView,
                                   SslErrorHandler handler,
                                   SslError error) {
        LogUtil.i(TAG,"onReceivedSslError");
        handler.proceed();
    }
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        notifyStarted(url);
        currentUrl=url;
        try {
            if (type == 1 && !url.contains("tv.cctv.com")) {
                String earlyJs = getFileContent(url);
                if (null != earlyJs && !earlyJs.isEmpty()) {
                    view.evaluateJavascript(earlyJs, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                        }
                    });
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "early inject error: " + e.getMessage());
        }
        String baseFolder = "tv-web/";
        if(url.contains("tv-web")){
            if(url.endsWith("index.html")){
                rootUrl=url;
            }
            lastUrl=url;
        }
    }


    private String getFileContent(String url){
        String baseFolder = "tv-web/";
        if(url.contains(baseFolder)){
            return null;
        }
        if (sEndJs == null) { sEndJs = assetText("js/end.js"); }
        String detail;
        if(type==1){
            if (sLoadDetailTvJs == null) { sLoadDetailTvJs = assetText("js/load_detail_tv.js"); }
            detail = sLoadDetailTvJs;
        } else {
            if (sLoadDetailVideoJs == null) { sLoadDetailVideoJs = assetText("js/load_detail_video.js"); }
            detail = sLoadDetailVideoJs;
        }
        return sEndJs + detail;
    }
    @Override
    public void onPageFinished(WebView view, String url) {
        notifyFinished(url);
        if (url.contains("tv.cctv.com")) {
            injectCctvFullscreenPipeline(view);
            return;
        }
        /* CCTV 直播入口走的是央视频（yangshipin.cn）：它不需要脱壳，但播放器自己的
           「全力加载中…」气泡要压掉。这里只注「气泡压制器」pageToastKill，不注脱壳脚本，
           也绝不影响上面 tv.cctv.com 的 4 个入口（它们 v4.5.16 已验证正常，不应再背扫描开销）。 */
        if (url.contains("yangshipin.cn")) {
            String toastKill = pageToastKillJs();
            injectToastKiller(view, toastKill);
            final String tk2 = toastKill;
            view.postDelayed(new Runnable() {
                @Override
                public void run() { injectToastKiller(view, tk2); }
            }, 350);
        }
        if (mWebView.getProgress() == 100) {
            String fileContent =getFileContent(url);
            if(null==fileContent){
                return;
            }
            view.evaluateJavascript(fileContent, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                }
            });
        }
    }

    /**
     * 央视外页脱壳的主链路：主方案与兜底都立即注入。
     *
     * 演进：原实现要串行熬过 onPageFinished + progress==100 + 5000ms 三段等待才上兜底；
     * 上一版把兜底压到 800ms，但 cctvFullscreen 不只是「全屏兜底」——它同时是
     * 「专辑页 → 播放页」自动跳转的执行者（tryEnterPlayer）。等 800ms 注入，
     * 等于每次点节目都白等 0.8 秒才有人去点「立即观看」。因此改为立即注入。
     * 脚本内自带 __DXTV_FS_INJECTED__ / fsMarked 幂等保护，重复注入无副作用。
     */
    private void injectCctvFullscreenPipeline(final WebView view) {
        final String fsBtn = pageFsBtnJs();
        final String cctvFs = getCctvFullscreenJs();
        if (fsBtn != null && !fsBtn.trim().isEmpty()) {
            view.evaluateJavascript(fsBtn, null);
        }
        injectCctvFullscreen(view, cctvFs);
        // 350ms 补一次，防止首次注入时页面 body 尚未就绪（幂等，重复注入无副作用）
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                injectCctvFullscreen(view, cctvFs);
            }
        }, 350);
    }

    /** 无条件注入气泡压制器。脚本自带 __DXTV_TOASTKILL_INJECTED__ 幂等保护，重复注入无副作用。 */
    private void injectToastKiller(WebView view, String js) {
        if (js == null || js.trim().isEmpty()) {
            LogUtil.e(TAG, "pageToastKill.js load failed or empty");
            return;
        }
        try {
            view.evaluateJavascript(js, null);
        } catch (Exception e) {
            LogUtil.e(TAG, "inject pageToastKill error: " + e.getMessage());
        }
    }

    private void injectCctvFullscreen(final WebView view, final String cctvFs) {
        if (cctvFs == null || cctvFs.trim().isEmpty()) { return; }
        view.evaluateJavascript(
                "(function(){return !!(window.__DXTV_FS_INJECTED__||window.__DXTV_FS_DONE__);})()",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        if (s != null && s.contains("true")) { return; }
                        view.evaluateJavascript(cctvFs, null);
                    }
                });
    }

    private String getCctvFullscreenJs(){
        if (sCctvFullscreenJs == null) {
            sCctvFullscreenJs = assetText("js/cctvFullscreen.js");
            if (sCctvFullscreenJs.isEmpty()) {
                LogUtil.e(TAG, "cctvFullscreen.js load failed or empty");
            }
        }
        return sCctvFullscreenJs;
    }

    private String pageFsBtnJs() {
        if (sPageFsBtnJs == null) { sPageFsBtnJs = assetText("js/pageFsBtn.js"); }
        return sPageFsBtnJs;
    }

    private String pageToastKillJs() {
        if (sPageToastKillJs == null) { sPageToastKillJs = assetText("js/pageToastKill.js"); }
        return sPageToastKillJs;
    }

    @Override
    public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl) {
        LogUtil.e(TAG, "onReceivedError: " + errorCode
                + ", description: " + description
                + ", url: " + failingUrl);
        notifyError(errorCode, description, failingUrl);
    }

    private Map<String,String> toHeader(Map<String,String> orgHeader){
        Map<String,String> headerMap = new HashMap<>();
        if(orgHeader.containsKey("Referer")){
            headerMap.put("Referer",orgHeader.get("Referer"));
        }
        if(orgHeader.containsKey("Origin")){
            headerMap.put("Origin",orgHeader.get("Origin"));
        }
        if(orgHeader.containsKey("User-Agent")){
            headerMap.put("User-Agent",orgHeader.get("User-Agent"));
        }
        if(orgHeader.containsKey("Host")){
            headerMap.put("Host",orgHeader.get("Host"));
        }
        return headerMap;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest webResourceRequest) {
        String accept=  webResourceRequest.getRequestHeaders().get("Accept");
        String url=webResourceRequest.getUrl().toString();
        String orgUrl=url;
        String method = webResourceRequest.getMethod();
        if(type==1){
            if(orgUrl.startsWith("https://tlive.fengshows.com/live/")||orgUrl.startsWith("https://hkmolive.fengshows.com/live/")){
                String realUrl= "https://qctv.fengshows.cn"+orgUrl.substring(orgUrl.indexOf("/live"));
                Map<String,String> headerMap = new HashMap<>();
                InputStream inputStream = HttpUtil.get(realUrl,new HashMap<>());
                if(null==inputStream){
                    return super.shouldInterceptRequest(webView, webResourceRequest);
                }
                WebResourceResponse resp=new WebResourceResponse("video/x-flv",
                        ConstantMy.UTF8, inputStream);
                headerMap.put("access-control-allow-origin","*");
                resp.setResponseHeaders(headerMap);
                return resp;
            }

            if(orgUrl.startsWith("https://gdtv-api.gdtv.cn/api/tv/v2/tvChannel")){
                Map<String,String> orgHeadMap = webResourceRequest.getRequestHeaders();
                if (!"GET".equalsIgnoreCase(method)) {
                    return super.shouldInterceptRequest(webView, webResourceRequest);
                }
                Map<String,String> headerMap = new HashMap<>(orgHeadMap);
                headerMap.remove("x-requested-with");
                String json = HttpUtil.getJson(orgUrl,headerMap);
                WebResourceResponse resp=new WebResourceResponse("application/json;charset=UTF-8",
                        ConstantMy.UTF8, new ByteArrayInputStream(json.getBytes(Charset.defaultCharset())));
                headerMap.put("access-control-allow-origin","*");
                resp.setResponseHeaders(headerMap);
                return resp;
            }
            if(url.contains(".m3u8")&&currentUrl!=null&&currentUrl.contains("u-link=1")){
               String js= MessageFormat.format(
                        "sessionStorage.setItem(\"{0}\",\"{1}\");sessionStorage.setItem(\"{2}\",\"{3}\");",
                        "u-m3u8",url,"u-loc",currentUrl);
                Util.evalOnUi(webView,js);
            }
        }
        if(null!=accept&&accept.startsWith("image/")&&!imageLoad(url)){
            return new WebResourceResponse(null,
                    null, null);
        }
        int index= url.indexOf("tv-web");
        if(index<0){
            if(webResourceRequest.getMethod().equals("GET")&&url.startsWith("https://mesh.if.iqiyi")){
                if(url.startsWith("https://mesh.if.iqiyi.com/tvg/v2/lw/base_info")){
                    Util.evalOnUi(webView,Util.sessionStorageWithTime("iqiyiXj",url));
                }
            }
            return super.shouldInterceptRequest(webView, webResourceRequest);
        }
        if(url.endsWith("tvImg=1")){
            if(index>0) {
                String fileName = url.substring(index,url.indexOf("?"));
                return new WebResourceResponse("image/jpeg",
                        ConstantMy.UTF8, FileUtil.readExtIn(MyApplication.getAppContext(),fileName));
            }
        }
        int indexWen=url.indexOf("?");
        if(indexWen>0){
            url=url.substring(0,indexWen);
        }

        if(url.endsWith("js")){
            if(index>0) {
                String fileName = url.substring(index);
                if(fileName.endsWith("basex.js")){
                    return new WebResourceResponse("text/html",
                            ConstantMy.UTF8, new ByteArrayInputStream(baseJs(fileName).getBytes(Charset.defaultCharset())));
                }
                return new WebResourceResponse("text/javascript",
                        ConstantMy.UTF8, FileUtil.readExtIn( MyApplication.getAppContext(),fileName));
            }
        }
        if(url.endsWith("css")){
            if(index>0) {
                String fileName = url.substring(index);
                return new WebResourceResponse("text/css",
                        ConstantMy.UTF8, FileUtil.readExtIn(MyApplication.getAppContext(),fileName));
            }
        }
        if(url.endsWith(".html")){
            if(index>0){
                String fileName=url.substring(index);
                String html = FileUtil.readExt(MyApplication.getAppContext(),fileName);
                html= html.replace("base.js","basex.js");
                return new WebResourceResponse("text/html",
                        ConstantMy.UTF8, new ByteArrayInputStream(html.getBytes(Charset.defaultCharset())));
            }
        }
        if(url.endsWith(".woff2")){
            if(index>0){
                String fileName = url.substring(index);
                return new WebResourceResponse("font/woff2",
                        ConstantMy.UTF8, FileUtil.readExtIn(MyApplication.getAppContext(),fileName));
            }
        }
        return super.shouldInterceptRequest(webView, webResourceRequest);

    }




    private String baseJs(String fileName){
       String baseStr= FileUtil.readExt(MyApplication.getAppContext(),fileName);
        Map<String, Object> data = new HashMap<>();
        data.put("version", AppVersionUtils.getVersionCode());
       return TplUtil.tpl(baseStr,data);
    }


   private  boolean notLoadUrl(String url){

       if(url.contains("https://cmts.iqiyi.com/bulle")){
           return  true;
       }
      if(url.contains("https://puui.qpic.cn/iwan_cloud")){
          return  true;
      }
       if(url.contains("https://msg.qy.net/")){
           return  true;
       }
      return  false;
   }
    private boolean  imageLoad(String url){
        if(url.contains("tvImg")){
            return true;
        }
        if(url.contains("cctvpic.com")){
            return true;
        }
        if(url.contains("default")){
            return true;
        }
        if(url.contains("open.weixin.qq.com/connect/qrcode")){
            String code=Util.loginQr(url,"微信");
            Util.evalOnUi(mWebView, code);
            return true;
        }
        if(url.contains("ptlogin2.qq.com/ssl/ptqrshow")){
            String code=Util.loginQr(url,"手机端qq");
            Util.evalOnUi(mWebView, code);
            return true;
        }
        if(url.startsWith("https://img.alicdn.com/imgextra/")&&url.endsWith("xcode.png")){
            String code=Util.loginQr(url,"youkuQr");
            Util.evalOnUi(mWebView, code);
            return true;
        }
        return false;
    }

    public  static Boolean currentUrlIsHome(){
        if(null==currentUrl){
            return  false;
        }
        if(currentUrl.contains("tv-web")){
            return true;
        }
        return false;
    }
    public static String  backUrl(){
        if(null==currentUrl){
            return null;
        }
        if(null!=sWebView && sWebView.canGoBack()){
            sWebView.goBack();
            return "$$GOBACK$$";
        }
        if(currentUrl.contains("tv-web")){
            if(currentUrl.endsWith("index.html")){
                return null;
            }
            return rootUrl;
        }
        return lastUrl;
    }

}