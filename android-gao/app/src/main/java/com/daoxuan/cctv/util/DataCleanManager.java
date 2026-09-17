package com.daoxuan.cctv.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.math.BigDecimal;


public class DataCleanManager {


    public static void cleanInternalCache(Context context) {

        deleteFilesByDirectory(context.getCacheDir());
    }


    public static void cleanDatabases(Context context) {

        deleteFilesByDirectory(new File("/data/data/"
                + context.getPackageName() + "/databases"));
    }


    public static void cleanSharedPreference(Context context) {

        deleteFilesByDirectory(new File("/data/data/"
                + context.getPackageName() + "/shared_prefs"));
    }


    public static void cleanDatabaseByName(Context context, String dbName) {
        context.deleteDatabase(dbName);
    }


    public static void cleanFiles(Context context) {
        deleteFilesByDirectory(context.getFilesDir());
    }


    public static void cleanExternalCache(Context context) {

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {

            deleteFilesByDirectory(context.getExternalCacheDir());
        }
    }


    public static void cleanCustomCache(String filePath) {

        deleteFilesByDirectory(new File(filePath));
    }


    public static void cleanApplicationData(Context context, String... filepath) {

        cleanInternalCache(context);
        cleanExternalCache(context);
        cleanDatabases(context);
        cleanSharedPreference(context);
        cleanFiles(context);

        if (filepath == null) {

            return;
        }

        for (String filePath : filepath) {

            cleanCustomCache(filePath);
        }
    }


    private static boolean deleteFilesByDirectory(File dir) {

        if (dir != null && dir.isDirectory()) {

            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {

                boolean success = deleteFilesByDirectory(new File(dir,
                        children[i]));

                if (!success) {

                    return false;
                }
            }
        }
        return dir.delete();
    }

    // Context.getExternalCacheDir() -->
    public static long getFolderSize(File file) {

        long size = 0;

        try {

            File[] fileList = file.listFiles();

            for (int i = 0; i < fileList.length; i++) {

                if (fileList[i].isDirectory()) {

                    size = size + getFolderSize(fileList[i]);
                } else {

                    size = size + fileList[i].length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (int) size;
    }


    public static void deleteFolderFile(String filePath, boolean deleteThisPath) {

        if (!TextUtils.isEmpty(filePath)) {

            try {

                File file = new File(filePath);

                if (file.isDirectory()) {

                    File files[] = file.listFiles();

                    for (int i = 0; i < files.length; i++) {

                        deleteFolderFile(files[i].getAbsolutePath(), true);
                    }
                }

                if (deleteThisPath) {

                    if (!file.isDirectory()) {

                        file.delete();
                    } else {

                        if (file.listFiles().length == 0) {

                            file.delete();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static String getCacheSize(Context context) {

        long tCacheSize = getFolderSize(context.getCacheDir()); 

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {

            tCacheSize += getFolderSize(context.getExternalCacheDir()); 
        }

        return getFormatSize(tCacheSize);
    }


    public static long getCacheSizeInt(Context context) {

        long tCacheSize = getFolderSize(context.getCacheDir()); 

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {

            tCacheSize += getFolderSize(context.getExternalCacheDir()); 
        }

        return tCacheSize;
    }


    public static void clearIntExtCache(Context context) {

        deleteFilesByDirectory(context.getCacheDir()); 

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {

            deleteFilesByDirectory(context.getExternalCacheDir()); 
        }
    }


    public static String getFormatSize(double size) {

        double kiloByte = size / 1024;

        if (kiloByte < 1) {

            return size + "Byte";
        }

        double megaByte = kiloByte / 1024;

        if (megaByte < 1) {

            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));

            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;

        if (gigaByte < 1) {

            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));

            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;

        if (teraBytes < 1) {

            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));

            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "GB";
        }

        BigDecimal result4 = new BigDecimal(teraBytes);

        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "TB";
    }
}