package com.daoxuan.cctv.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import com.daoxuan.cctv.MyApplication;

public class WebService extends NanoHTTPD {
    protected String TAG = "WebService";

    public WebService(int port) {
        super(port);
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }catch (Exception e){
            LogUtil.i(TAG,e.getMessage());
        }
        LogUtil.i(TAG,"Running! Point your browsers to http://localhost:"+port);
    }

 
    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        if(uri.startsWith("/")){
            uri= uri.substring(1);
        }
        if(uri.endsWith(".ico")){
            return newFixedLengthResponse("404!!");
        }
        String mimetype = minetype(uri);
        Map<String, List<String>> stringListMap =  session.getParameters();
        if(uri.endsWith("image")){
            String url= stringListMap.get("url").get(0);
            String ref=stringListMap.get("tv-ref").get(0);
            InputStream data= HttpRequest.get(url).header("Referer",ref).stream();
            return newChunkedResponse(Response.Status.OK, mimetype, data);
        }
        String baseFolder = "tv-web/";
        if (uri.endsWith(".woff2")) {
            return newChunkedResponse(Response.Status.OK, "font/woff2", FileUtil.readExtIn(MyApplication.getAppContext(),baseFolder+uri));
        }
        if(uri.endsWith("ctrl")){
            String url= stringListMap.get("url").get(0);
            //BaseWebViewActivity.mWebView.loadUrl(url);
            WebViewDispatcher.loadUrl(url);

            return newFixedLengthResponse(Response.Status.OK, mimetype, "ok");
        }

        //System.out.println(ip + " [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] >>>　" + uri + "  ===> " + method);
        String data =readData(uri,session);
        return newFixedLengthResponse(Response.Status.OK, mimetype, data);
 
    }
    private String getRequestBody(IHTTPSession session){
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ResponseException e) {
            throw new RuntimeException(e);
        }
        return  files.get("postData");
    }
    private String minetype(String uri){
        String mimetype = "text/html";
        if (uri.endsWith(".html") || uri.endsWith(".htm")) {
            return  "text/html";
        }
        if (uri.endsWith(".js")) {
            return  "text/javascript";
        }
        if (uri.endsWith(".css")) {
            return "text/css";
        }
        if (uri.endsWith("json")) {
            return "application/json";
        }
        if(uri.endsWith("image")){
            return  "image/jpeg";

        }


        return mimetype;
    }
 
 
    private Response handler(IHTTPSession session) {
 
        return newFixedLengthResponse("404!!");
    }

 
    private String readData(String pathname,IHTTPSession session) {
        if(pathname.endsWith("json")){
            String requestUrl = session.getHeaders().get("rel-url");
            Method method = session.getMethod();
            try {
                requestUrl= URLDecoder.decode(requestUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogUtil.e(TAG,e.getMessage());
            }
            LogUtil.i(TAG,requestUrl);
            Map<String, String> headerMap=new HashMap<>();
            //headerMap.remove("rel-url");
            Map<String,String> headerMapInSession = session.getHeaders();
            if(headerMapInSession.containsKey("tv-ref")){
                headerMap.put("Referer",headerMapInSession.get("tv-ref"));
            }
            if(headerMapInSession.containsKey("tv-org")){
                headerMap.put("Origin",headerMapInSession.get("tv-org"));
            }
            if(method.equals(Method.GET)){
                String data= HttpUtil.getJson(requestUrl,headerMap);
                LogUtil.i(TAG,data);
                return data;
            }
            if(method.equals(Method.POST)){
                String requestBody= getRequestBody(session);
                LogUtil.i("requestBody",requestBody);
                String data= HttpUtil.postJson(requestUrl,headerMap,requestBody);
                return data;
            }
        }



        String baseFolder = "tv-web/";
        return FileUtil.readExt(MyApplication.getAppContext(),baseFolder+pathname);
    }
 
}
 
 
 