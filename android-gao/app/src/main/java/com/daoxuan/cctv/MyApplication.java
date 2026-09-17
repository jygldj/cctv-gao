package com.daoxuan.cctv;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.multidex.MultiDex;

import java.util.Random;
import java.util.UUID;

import com.daoxuan.cctv.service.CrashHandler;
import com.daoxuan.cctv.util.LogUtil;


public class MyApplication extends Application  {

    private static Context context;
    private static final String TAG = "MyApplication";
   @Override
   protected void attachBaseContext(Context base) {
       super.attachBaseContext(base);
       MultiDex.install(base);
   }

   public static  String androidId=null;


    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.i(TAG, "onViewInitBegin: ");
        allErrorCatch();
        context = getApplicationContext();
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if(null==androidId){
            LogUtil.i(TAG, "androidId: getUUID");
            androidId=getUUID();
        }
        CrashHandler.getInstance().init(this);
        CrashHandler.uploadExceptionToServer(this);
        try {
            System.setProperty("persist.sys.media.use-mediaDrm", "false");
        } catch (Exception e) {
            LogUtil.e("use-mediaDrm:"+e.getMessage());
        }
    }
    private String randomStr(int length){
        StringBuilder result = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomInt = random.nextInt(26) + 97;
            result.append((char) randomInt);
        }
        return result.toString();
    }
    private void allErrorCatch(){
        final Thread.UncaughtExceptionHandler systemDefault = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                if (throwable != null && throwable.getMessage() != null &&
                        (throwable instanceof NullPointerException) &&
                        (throwable.getStackTrace() != null && throwable.getStackTrace().length > 0 &&
                                containsSurfaceTextureInStackTrace(throwable.getStackTrace()))) {

                    LogUtil.e("Application", "捕获到 SurfaceTexture 相关异常: " + throwable.getMessage());

                    CrashHandler.recordNonFatal(getApplicationContext(), throwable);
                    return;
                }

                if (systemDefault != null) {
                    systemDefault.uncaughtException(thread, throwable);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }

            private boolean containsSurfaceTextureInStackTrace(StackTraceElement[] stackTrace) {
                for (StackTraceElement element : stackTrace) {
                    if (element.getClassName().contains("SurfaceTexture") ||
                            element.getMethodName().contains("SurfaceTexture")) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
    public static Context getAppContext() {
        return context;
    }
    public String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }

    public String getString(String s, String defValue) {
        return isEmpty(s) ? defValue : s;
    }

    public boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static Context getContext() {
        return context;
    }
    public static String getUUID() {
        String serial = null;
        String m_szDevIDShort = "随机两位数" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; 
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                serial = "默认值";
            } else {
                serial = Build.SERIAL;
            }
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            serial = "默认值"; 
        }
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }


}