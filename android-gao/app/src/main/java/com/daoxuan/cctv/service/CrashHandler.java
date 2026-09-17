package com.daoxuan.cctv.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.daoxuan.cctv.MyApplication;
import com.daoxuan.cctv.util.FileUtil;
import com.daoxuan.cctv.util.HttpUtil;
import com.daoxuan.cctv.util.JsonUtil;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.util.Util;
import com.daoxuan.cctv.util.ValueUtil;


public class CrashHandler implements UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    public static final boolean DEBUG = true;

    public static final String FILE_NAME = "crash";
    private boolean mIsHandling = false; 

   // private static final String PATH = Environment.getExternalStorageDirectory().getPath() +
        //    "/Crash/log/";

    private static final String FILE_NAME_SUFFIX = ".trace";

    private static CrashHandler sInstance = new CrashHandler();
    private UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;


    private CrashHandler() {

    }

    public static CrashHandler getInstance() {
        return sInstance;
    }


    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context.getApplicationContext();

    }


    public static void recordNonFatal(Context context, Throwable e) {
        try {
            CrashHandler handler = getInstance();
            if (handler.mContext == null && context != null) {
                handler.mContext = context.getApplicationContext();
            }
            String path = handler.dumpExceptionToSDCard(e);
            ValueUtil.putString(handler.mContext, "errorLog", path);
            ValueUtil.putString(handler.mContext, "errorLogRead", "0");
        } catch (Exception ignore) {
        }
    }



    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (mIsHandling) {
            if (mDefaultCrashHandler != null) {
                mDefaultCrashHandler.uncaughtException(thread, ex);
            }
            return;
        }
        mIsHandling = true;
        try {
            String path= dumpExceptionToSDCard(ex);
            LogUtil.e("PATH",path);
            ValueUtil.putString(mContext,"errorLog",path);
            ValueUtil.putString(mContext,"errorLogRead","0");
           //String error=  dumpExceptionToStr(ex);
           // uploadExceptionToServer(error);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ex.printStackTrace();
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
        }

    }

    private String dumpExceptionToStr(Throwable e){
        long current = System.currentTimeMillis();
        StringWriter stringWriter = new StringWriter();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(current));
        PrintWriter pw = new PrintWriter(new BufferedWriter(stringWriter));
        try{
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            e.printStackTrace(pw);
            pw.close();
        } catch (Exception e1) {
            LogUtil.e(TAG,"dump crash info failed");
        }finally {
            pw.close();
        }
        StringBuffer buffer = stringWriter.getBuffer();
        return buffer.toString();
    }

    private String dumpExceptionToSDCard(Throwable e) throws IOException{
      /*  if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (DEBUG) {
                LogUtil.w(TAG, "sdcard unmounted,skip dump exception");
                return;
            }
        }*/
        File dir = mContext.getFilesDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date(current));
        String path= dir.getPath()+"/" + FILE_NAME + time + FILE_NAME_SUFFIX;
        File file = new File(path);

        try{
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            e.printStackTrace(pw);
            pw.close();
        } catch (Exception e1) {
           LogUtil.e(TAG,"dump crash info failed");
        }
        return path;
    }


    private void dumpPhoneInfo(PrintWriter pw) throws PackageManager.NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(),PackageManager.GET_ACTIVITIES);
        String androidId= MyApplication.androidId;
        String num="32";
        if(Util.is64()){
            num="64";
        }
        int api = Build.VERSION.SDK_INT;
        pw.print("SYS API: ");
        pw.println(androidId+" "+num+ " "+ api);
        pw.print("App Version: ");
        pw.print(pi.versionName);
        pw.print("_");
        pw.println(pi.versionCode);
        pw.print("OS Version: ");
        pw.print(Build.VERSION.RELEASE);
        pw.print("_");
        pw.println(Build.VERSION.SDK_INT);
        pw.print("Vendor: ");
        pw.println(Build.MANUFACTURER);
        pw.print("Model: ");
        pw.println(Build.MODEL);
        pw.print("CPU ABI: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pw.println(JsonUtil.toJson(Build.SUPPORTED_ABIS));
            pw.println(JsonUtil.toJson(Build.SUPPORTED_64_BIT_ABIS));
        }else {
            pw.println(Build.CPU_ABI);
        }

    }


    public static void uploadExceptionToServer(Context context)  {
        Context app = context.getApplicationContext();
        File dir = app.getFilesDir();
        File[] files = dir.listFiles((d, name) -> name != null && name.startsWith(FILE_NAME) && name.endsWith(FILE_NAME_SUFFIX));
        if (files == null || files.length == 0) {
            return;
        }

        new Thread(() -> {
            for (File f : files) {
                String errLog = null;
                try {
                    errLog = FileUtil.getStringFromInputStream(new FileInputStream(f));
                } catch (Exception ignored) {
                    errLog = null;
                }
                if (errLog == null || errLog.isEmpty()) {
                    return;
                }

                try {
                    HttpUtil.postJson("http://api.vonchange.com/daoxuan/error", null, errLog);
                    LogUtil.i("POST", "http://api.vonchange.com/daoxuan/error");
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                } catch (Exception uploadErr) {
                    LogUtil.e("CrashUpload", "upload failed: " + uploadErr.getMessage());
                }
            }

            File[] left = dir.listFiles((d, name) -> name != null && name.startsWith(FILE_NAME) && name.endsWith(FILE_NAME_SUFFIX));
            if (left == null || left.length == 0) {
                ValueUtil.putString(app, "errorLogRead", "1");
                ValueUtil.putString(app, "errorLog", "");
            }
        }).start();
    }
}