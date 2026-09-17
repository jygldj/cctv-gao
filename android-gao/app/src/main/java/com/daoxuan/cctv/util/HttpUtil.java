package com.daoxuan.cctv.util;


import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.daoxuan.cctv.call.DownloadCallback;
import com.daoxuan.cctv.call.DownloadProgressListener;
import com.daoxuan.cctv.dns.HttpDns;

public class HttpUtil {
    private static String TAG = "HttpUtil";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    private static final int DEFAULT_TIMEOUT = 5;
    private static OkHttpClient defaultClient = null;
    private static OkHttpClient noRedirectClient = null;
    private static final Object lockO = new Object();
    static {
        defaultClient();
        noRedirectClient();
    }
/*    public static OkHttpClient defaultClient() {
        synchronized (lockO) {
            if (defaultClient == null) {
                OkHttpClient.Builder builder = new OkHttpClient.Builder()
                        .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .sslSocketFactory(new SSLSocketFactoryCompat(SSLSocketFactoryCompat.trustAllCert), SSLSocketFactoryCompat.trustAllCert);
                defaultClient = builder.build();
            }
            return defaultClient;
        }
    }*/
    public static void defaultClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .sslSocketFactory(new SSLSocketFactoryCompat(SSLSocketFactoryCompat.trustAllCert), SSLSocketFactoryCompat.trustAllCert);
        builder=ignoreSSL(builder);
        defaultClient = builder
                  .dns(new HttpDns())
                  .build();
    }
    public static OkHttpClient.Builder ignoreSSL (OkHttpClient.Builder builder) {
        builder.sslSocketFactory(createSSLSocketFactory())
                .hostnameVerifier((s, sslSession) -> true);
        return builder;
    }

    private static SSLSocketFactory createSSLSocketFactory () {

        SSLSocketFactory sSLSocketFactory = null;

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            sSLSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
            //LOGGER.info(e.getMessage(), e);
        }

        return sSLSocketFactory;
    }

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted (java.security.cert.X509Certificate[] x509Certificates,
                                        String s) throws java.security.cert.CertificateException {

        }

        @Override
        public void checkServerTrusted (java.security.cert.X509Certificate[] x509Certificates,
                                        String s) throws java.security.cert.CertificateException {

        }


        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers () {
            return new X509Certificate[0];
        }
    }
    public static boolean isErrorResponse(String json){
        if(!json.startsWith("{")||json.equals("400")||json.equals("500")){
            return true;
        }
        return false;
    }
    public static void noRedirectClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(true)
                .sslSocketFactory(new SSLSocketFactoryCompat(SSLSocketFactoryCompat.trustAllCert), SSLSocketFactoryCompat.trustAllCert);
        noRedirectClient = builder
                .dns(new HttpDns())
                .build();
    }
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static String postJson(String url,
                            Map<String, String> headerMap, String requestBody) {
        RequestBody body = RequestBody.create(JSON, requestBody);
        Request.Builder builder =new Request.Builder()
                .url(url);
        if(null!=headerMap&&!headerMap.isEmpty()){
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                LogUtil.i(TAG,entry.getKey()+" "+entry.getValue());
                if(entry.getKey().equals("tv-ref")){
                    builder.addHeader("Referer",entry.getValue());
                    continue;
                }
                builder.addHeader(entry.getKey(),entry.getValue());
            }
        }
        Request request = builder
                .post(body)
                .build();
       return responseDo(request);
    }
    public static   void  downloadQ(String url, String filesDirPath, String FILE_NAME, DownloadCallback downloadCallback)  {
        InputStream inputStream =   HttpRequest.get(url).stream();
        File target = new File(filesDirPath, FILE_NAME);
        try {
           FileOutputStream fileOutputStream = new FileOutputStream(target);
            byte[] buffer = new byte[2048];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
                // Log.d(TAG, "read: " + len);
            }
            fileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadCallback.downloaded();
    }
    public static   void  download(String url,String filesDirPath,String FILE_NAME,DownloadCallback downloadCallback){
        Request request = new Request.Builder().url(url).get().build();
        Call call = defaultClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                LogUtil.e(TAG, e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                InputStream inputStream = Objects.requireNonNull(response.body()).byteStream();
                File target = new File(filesDirPath, FILE_NAME);
                FileOutputStream fileOutputStream = new FileOutputStream(target);
                try {
                    byte[] buffer = new byte[2048];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        // Log.d(TAG, "read: " + len);
                    }
                    fileOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                downloadCallback.downloaded();
            }
        });
    }


        public static void downloadByProgress(final String url, File targetFile,final DownloadProgressListener listener){
            OkHttpClient client=defaultClient;
            Request request=new Request.Builder()
                    .url(url)
                    .build();
            Call call=client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    listener.onFailResponse();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                   // File target = new File(filePath);
                            //FileUtil.generateFile(url);
                    InputStream is=response.body().byteStream();   
                    BufferedInputStream bis=new BufferedInputStream(is);
                    byte[] content = new byte[1024];
                    int len=0;                                          
                    long sumReaded = 0L;
                    long contentSize=response.body().contentLength();   
                    FileOutputStream fos=new FileOutputStream(targetFile);    
                    while((len=bis.read(content))!=-1){                 
                        fos.write(content,0,len);                  
                        sumReaded+=len;                                 
                        listener.onDownloadProgress
                                (sumReaded,contentSize,false); 
                    }
                    listener.onDownloadResult(targetFile,true);
                    fos.flush();
                    fos.close();
                    bis.close();
                    is.close();
                }
            });
        }
    public static   InputStream get(String url,
                                   Map<String, String> headerMap){
        Request.Builder builder =new Request.Builder()
                .url(url);
        if(null!=headerMap&&!headerMap.isEmpty()){
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                LogUtil.i(TAG,entry.getKey()+" "+entry.getValue());
                if(entry.getKey().equals("tv-ref")){
                    builder.addHeader("Referer",entry.getValue());
                    continue;
                }
                builder.addHeader(entry.getKey(),entry.getValue());
            }
        }
        Request request = builder
                .get()
                .build();
        Response  response = null;
        try {
            response = defaultClient.newCall(request).execute();
        } catch (IOException e) {
          LogUtil.e(TAG,e.getMessage());
        }
        if(null==response){
            return null;
        }
        return response.body().byteStream();
    }

    public static   String getJson(String url,
                           Map<String, String> headerMap){
        Request.Builder builder =new Request.Builder()
                .url(url);
        if(null!=headerMap&&!headerMap.isEmpty()){
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                LogUtil.i(TAG,entry.getKey()+" "+entry.getValue());
                if(entry.getKey().equals("tv-ref")){
                    builder.addHeader("Referer",entry.getValue());
                    continue;
                }
                builder.addHeader(entry.getKey(),entry.getValue());
            }
        }
        Request request = builder
                .get()
                .build();
        return responseDo(request);
    }

    private static String responseDo(Request request){
        Response response =null;
        try{
            response = defaultClient.newCall(request).execute();
        }catch (IOException e){
            e.printStackTrace();
            LogUtil.e(TAG,"500 error: "+e.getMessage());
            return "500";
        }
        if(!response.isSuccessful()){
            LogUtil.e(TAG,"400 code: "+response.code());
            return "400";
        }
        try{
            return  response.body().string();
        }catch (IOException e){
            e.printStackTrace();
            LogUtil.e(TAG," 500 error response: "+e.getMessage());
            return "500";
        }
    }

}
