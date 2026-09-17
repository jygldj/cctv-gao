package com.daoxuan.cctv.dao;

import android.content.Context;

import java.util.Date;

import com.daoxuan.cctv.domain.live.Vod;
import com.daoxuan.cctv.service.UpdateService;
import com.daoxuan.cctv.util.LogUtil;

public class HistoryDaoX {
    private static String TAG="HistoryDaoX";


    public static Vod currentChannel(Context context){
        HistoryDao historyDao = AppDatabase.getInstance(context).historyDao();
        History history =   historyDao.queryOneBySite("tv");
        if(null==history){
            return UpdateService.getByKey("0_0");
        }
        Vod vod= UpdateService.getByUrl(history.url);
        if(null==vod){
            return UpdateService.getByKey("0_0");
        }
        return vod;
    }
    public static void updateChannel(Context context, String url){
        HistoryDao historyDao = AppDatabase.getInstance(context).historyDao();
        History history =   historyDao.queryOneBySite("tv");
        Vod vod = UpdateService.getByUrl(url);
        if(null==vod){
            return;
        }
        if(null==history){
            History historyNew = new History();
            historyNew.url=vod.getUrl();
            historyNew.name=vod.getName();
            historyNew.site="tv";
            historyNew.vodId="0";
            historyNew.createTime=new Date().getTime();
            historyNew.updateTime=new Date().getTime();
            historyDao.insertAll(historyNew);
            return ;
        }
        LogUtil.i(TAG,vod.getName()+vod.getUrl());
        historyDao.updateChannel(history.id,vod.getName(),vod.getUrl(),new Date().getTime());

    }
   /* public  static  void all(Context context,String data){
        HistoryDao historyDao = AppDatabase.getInstance(context).historyDao();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BaseMsg baseMsg = JsonUtil.fromJson(data, BaseMsg.class);
                String msgId=baseMsg.getMsgId();
                List<History> historyList =   historyDao.queryHistory();
                Map<String,String> dataMap = new HashMap<>();
                dataMap.put("key",msgId);
                dataMap.put("value",JsonUtil.toJson(historyList));
                //MainActivity.postMessage("sessionStorage",JsonUtil.toJson(dataMap));
            }
        }).start();
    }*/
}
