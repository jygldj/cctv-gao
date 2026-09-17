package com.daoxuan.cctv.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface FavoriteDao {
    //ORDER BY create_time DESC
    @Query("SELECT * FROM favorite ")
    List<Favorite> getAllFavorites();

    @Query("SELECT * FROM favorite WHERE vod_key = :vodKey LIMIT 1")
    Favorite getFavoriteByVodKey(String vodKey);
    
    @Query("SELECT * FROM favorite WHERE vod_url = :vodUrl LIMIT 1")
    Favorite getFavoriteByVodUrl(String vodUrl);

    @Insert
    void insertFavorite(Favorite favorite);

    @Delete
    void deleteFavorite(Favorite favorite);
    
  /*  @Query("DELETE FROM favorite WHERE vod_key = :vodKey")
    void deleteFavoriteByVodKey(String vodKey);*/
    
    @Query("DELETE FROM favorite WHERE vod_url = :vodUrl")
    void deleteFavoriteByVodUrl(String vodUrl);
    
    @Query("SELECT COUNT(*) FROM favorite WHERE vod_url = :vodUrl")
    int isFavorite(String vodUrl);
}