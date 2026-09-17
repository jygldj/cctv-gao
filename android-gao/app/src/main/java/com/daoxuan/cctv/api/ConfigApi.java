package com.daoxuan.cctv.api;

import android.os.Build;

import java.text.MessageFormat;
import java.util.HashMap;

import com.daoxuan.cctv.MyApplication;
import com.daoxuan.cctv.call.ConfigCallback;
import com.daoxuan.cctv.domain.ConfigDTO;
import com.daoxuan.cctv.util.AppVersionUtils;
import com.daoxuan.cctv.util.HttpUtil;
import com.daoxuan.cctv.util.JsonUtil;
import com.daoxuan.cctv.util.LogUtil;
import com.daoxuan.cctv.util.Util;

public class ConfigApi {
    public static final String  apiHost="http://api.vonchange.com";
    private static final  String updateUrl=apiHost+"/daoxuan/config/update.json";
    public static ConfigDTO configDTO=null;
    private static  Long lastTime=System.currentTimeMillis();
    public static void  syncGetConfig(ConfigCallback configCallback){
        new Thread(()->{
            ConfigDTO configDTO1 = getConfig();
            configCallback.getConfig(configDTO1);
        }).start();
    }
    public static ConfigDTO getConfig(){
        if(null!=configDTO&&System.currentTimeMillis()-lastTime<1000*60*60*24){
            return configDTO;
        }
        lastTime=System.currentTimeMillis();
        String json;
        try {
             String androidId= MyApplication.androidId;
             String num="32";
             if(Util.is64()){
                 num="64";
             }
             int api = Build.VERSION.SDK_INT;
            String remark=  Build.MANUFACTURER+"_"+Build.MODEL+"_"+Build.VERSION.RELEASE+"_"+
                    AppVersionUtils.getVersionCode();
            String paramStr=   MessageFormat.format("?id={0}&num={1}&api={2}&remark={3}",androidId,num,api,remark);
            String reqUrl =updateUrl+paramStr;
            LogUtil.i("getConfig","reqUrl "+reqUrl);
             json = HttpUtil.getJson(reqUrl,new HashMap<>());
        }catch (Exception e){
            return  null;
        }
        LogUtil.i("getConfig","getConfig "+json);
        if(HttpUtil.isErrorResponse(json)){
            return null;
        }
        ConfigDTO result= JsonUtil.fromJson(json,ConfigDTO.class);
        configDTO=result;
        return result;
    }

}
