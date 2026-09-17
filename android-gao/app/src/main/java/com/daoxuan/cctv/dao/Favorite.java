package com.daoxuan.cctv.dao;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite")
public class Favorite {
    @PrimaryKey(autoGenerate = true)
    public Integer id;
    
    @ColumnInfo(name = "vod_key")
    public String vodKey;
    
    @ColumnInfo(name = "vod_name")
    public String vodName;
    
    @ColumnInfo(name = "vod_url")
    public String vodUrl;
    
    @ColumnInfo(name = "create_time")
    public Long createTime;

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVodKey() {
        return vodKey;
    }

    public void setVodKey(String vodKey) {
        this.vodKey = vodKey;
    }

    public String getVodName() {
        return vodName;
    }

    public void setVodName(String vodName) {
        this.vodName = vodName;
    }

    public String getVodUrl() {
        return vodUrl;
    }

    public void setVodUrl(String vodUrl) {
        this.vodUrl = vodUrl;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}