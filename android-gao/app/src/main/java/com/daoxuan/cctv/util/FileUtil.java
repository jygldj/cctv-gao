package com.daoxuan.cctv.util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.daoxuan.cctv.service.UpdateService;

public class FileUtil {
    private static final String TAG="FileUtil";

    public static File getTBSFileDir(Context context) {
        //String dirName = "TBSFile";
        //return context.getExternalFilesDir(dirName);
        return context.getFilesDir();
    }
    public  static void   copyFile(File orgFile,File toFile){
        FileOutputStream fos;
        InputStream is;
        try {
            is = new FileInputStream(orgFile);
            fos = new FileOutputStream(toFile);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public  static void   copyFileFromAssert(Context context,String orgFile,String toFile){
        FileOutputStream fos;
        InputStream is;
        try {
            is = context.getAssets().open(orgFile);
            fos = new FileOutputStream(new File(toFile));
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static String readAssert(Context context, String strAssertFileName) {
        AssetManager assetManager = context.getAssets();
        String strResponse = "";
        try {
            InputStream ims = assetManager.open(strAssertFileName);
            strResponse = getStringFromInputStream(ims);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strResponse;
    }
    public static InputStream readAssertIn(Context context, String strAssertFileName) {
        AssetManager assetManager = context.getAssets();
        try {
           return assetManager.open(strAssertFileName);
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage());
        }
        return null;
    }
   public static InputStream readExtIn(Context context,String extFileName) {
       String baseFolder= UpdateService.baseFolder+"/";
       String fullPath=baseFolder+extFileName;
       LogUtil.i(TAG,fullPath);
       InputStream asset = readAssertIn(context, extFileName);
       if (asset == null && !extFileName.startsWith("tv-web/")) {
           asset = readAssertIn(context, "tv-web/" + extFileName);
           LogUtil.i(TAG,"readExt retry with prefix tv-web/ -> "+extFileName);
       }
       if (asset != null) {
           return asset;
       }
       if(new File(fullPath).exists()){
           try {
               return new FileInputStream(fullPath);
           } catch (FileNotFoundException e) {
               LogUtil.e(TAG, e.getMessage());
               //throw new RuntimeException(e);
           }
       }
       String altPath=baseFolder+"tv-web/"+extFileName;
       if(new File(altPath).exists()){
           try {
               LogUtil.i(TAG,"readExt hit filesDir alt -> "+altPath);
               return new FileInputStream(altPath);
           } catch (FileNotFoundException e) {
               LogUtil.e(TAG, e.getMessage());
           }
       }
       return null;
   }
    public static String readExt(Context context,String extFileName) {
        return getStringFromInputStream(readExtIn(context,extFileName));
    }
    public static void  del(String filePath) {
       File file = new File(filePath);
       if(file.exists()){
          boolean delete= file.delete();
          LogUtil.i(TAG,filePath+" del "+delete);
       }
    }

    public static String getStringFromInputStream(InputStream a_is) {
       if(null==a_is){
           return "";
       }
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(a_is));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return sb.toString();
    }
    public static void unzipFile(String zipPath, String outputDirectory,boolean skipFirst)throws IOException {


        File file = new File(outputDirectory);
        if (!file.exists()) {
            file.mkdirs();
        }
        InputStream inputStream = new FileInputStream(zipPath); ;
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        if(null==zipEntry){
            return;
        }
        String firstName=null;
        if(skipFirst){
            int index = zipEntry.getName().indexOf("/");
            if(index>0){
                firstName=zipEntry.getName().substring(0,index+1);
            }else{
                firstName=zipEntry.getName();
                zipEntry=zipInputStream.getNextEntry();
            }
        }
        byte[] buffer = new byte[1024 * 1024];
        int count = 0;
        while (zipEntry != null) {
            String fileName = zipEntry.getName();
            if(skipFirst){
                fileName=fileName.substring(firstName.length());
            }
            if (!zipEntry.isDirectory()) {  
                file = new File(outputDirectory + File.separator + fileName);  

                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                while ((count = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, count);
                }
                fileOutputStream.close();
            }else{
                File folder= new File(outputDirectory+File.separator+fileName);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
            }

            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();

    }

}
