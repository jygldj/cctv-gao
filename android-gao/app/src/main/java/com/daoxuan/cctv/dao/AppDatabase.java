package com.daoxuan.cctv.dao;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {History.class, Favorite.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase mAppDatabase;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `favorite` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `vod_key` TEXT, `vod_url` TEXT, `create_time` INTEGER, `vod_name` TEXT)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (mAppDatabase == null) {
            synchronized (AppDatabase.class) {
                if (mAppDatabase == null) {
                    mAppDatabase = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "dbRoomTest.db")
                            .addMigrations(MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                             .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return mAppDatabase;
    }

    public abstract HistoryDao historyDao();
    public abstract FavoriteDao favoriteDao();
}