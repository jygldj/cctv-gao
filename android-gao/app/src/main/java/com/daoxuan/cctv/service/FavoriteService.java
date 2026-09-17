package com.daoxuan.cctv.service;

import android.content.Context;

import java.util.Date;
import java.util.List;

import com.daoxuan.cctv.dao.AppDatabase;
import com.daoxuan.cctv.dao.Favorite;
import com.daoxuan.cctv.domain.live.Vod;

public class FavoriteService {
    private static FavoriteService instance;
    private AppDatabase db;

    private FavoriteService(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public static synchronized FavoriteService getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteService(context);
        }
        return instance;
    }


    public void addFavorite(Vod vod) {
        Favorite favorite = new Favorite();
        //favorite.setVodKey(vod.getKey());
        favorite.setVodName("❤"+vod.getName());
        favorite.setVodUrl(favUrl(vod.getUrl()));
        favorite.setCreateTime(new Date().getTime());
        db.favoriteDao().insertFavorite(favorite);
    }
    private String favUrl(String url){
        if(url.contains("?")){
            return  url +"&usave=1";
        }
        return url+"?usave=1";
    }
    private String checkFavUrl(String url){
        if(url.contains("usave=1")){
            return url;
        }
        return favUrl(url);
    }


    public void removeFavorite(String vodUrl) {
        vodUrl=checkFavUrl(vodUrl);
        db.favoriteDao().deleteFavoriteByVodUrl(vodUrl);
    }


    public boolean isFavorite(String vodUrl) {
        vodUrl=checkFavUrl(vodUrl);
        return db.favoriteDao().isFavorite(vodUrl) > 0;
    }


    public List<Favorite> getAllFavorites() {
        return db.favoriteDao().getAllFavorites();
    }
}