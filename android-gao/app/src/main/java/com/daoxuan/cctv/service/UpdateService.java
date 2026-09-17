package com.daoxuan.cctv.service;

import android.content.Context;

import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.daoxuan.cctv.MyApplication;
import com.daoxuan.cctv.dao.Favorite;
import com.daoxuan.cctv.domain.live.DataWrapper;
import com.daoxuan.cctv.domain.live.Live;
import com.daoxuan.cctv.domain.live.Vod;
import com.daoxuan.cctv.util.FileUtil;
import com.daoxuan.cctv.util.JsonUtil;

public class UpdateService {

    private static final String TAG = "UpdateService";
    public static String baseFolder;

    /**
     * 只初始化资源根目录。
     * 原来每次启动都会开线程把 assets/tv-web 全量拷贝到私有目录：既拖慢冷启动，
     * 又可能在拷贝还没完成时就已经被读取（首屏偶发空白）。
     * 而 FileUtil.readExtIn 本来就优先读 assets，找不到才回落私有目录，所以这次拷贝是多余的。
     */
    public static void initBaseFolder(Context context) {
        if (context == null) {
            return;
        }
        baseFolder = context.getFilesDir().getPath();
    }

    protected static Map<String, Vod> indexVodMap = new HashMap<>();
    protected static Map<Integer, Integer> tagMaxMap = new HashMap<>();
    protected static Map<String, String> urlKeyMap = new HashMap<>();
    protected static List<Live> newLives = new ArrayList<>();

    public static void initTvData() {
        String json = FileUtil.readExt(MyApplication.getAppContext(), "tv-web/js/cctv/tv.json");
        if (json.trim().isEmpty()) {
            return;
        }
        DataWrapper<List<Live>> data = JsonUtil.fromJson(json, new TypeToken<DataWrapper<List<Live>>>() {
        }.getType());
        List<Live> lives = data.getData();
        indexVodMap = new HashMap<>();
        tagMaxMap = new HashMap<>();
        urlKeyMap = new HashMap<>();
        int i = 0, j;
        for (Live life : lives) {
            j = 0;
            for (Vod vod : life.getVods()) {
                vod.setTagIndex(i);
                vod.setDetailIndex(j);
                String key = i + "_" + j;
                vod.setKey(key);
                indexVodMap.put(key, vod);
                urlKeyMap.put(vod.getUrl(), key);
                j++;
            }
            tagMaxMap.put(i, j - 1);
            i++;
        }
        newLives = lives;
    }


    public static List<Live> getByLivesWithFavorites(Context context) {
        List<Favorite> favorites = FavoriteService.getInstance(context).getAllFavorites();
        List<Live> lives = new ArrayList<>();
        if (!favorites.isEmpty()) {
            int tagIndex = 0;
            Live favoriteLive = new Live();
            favoriteLive.setName("收藏");
            favoriteLive.setTag("favorite");
            favoriteLive.setIndex(tagIndex);
            List<Vod> favoriteVods = new ArrayList<>();
            int index = 0;
            for (Favorite favorite : favorites) {
                Vod vod = new Vod();
                String baseName = favorite.getVodName();
                String displayName = String.format("%d.%s", index + 1, baseName);
                vod.setName(displayName);
                String url = favorite.getVodUrl();
                vod.setUrl(url);
                vod.setTagIndex(tagIndex);
                vod.setDetailIndex(index);
                String key = tagIndex + "_" + index;
                vod.setKey(key);
                indexVodMap.put(key, vod);
                urlKeyMap.put(vod.getUrl(), key);
                index++;
                favoriteVods.add(vod);
            }
            favoriteLive.setVods(favoriteVods);
            lives.add(favoriteLive);
            lives.addAll(newLives);
            tagMaxMap.put(tagIndex, index - 1);
            return lives;
        }
        return newLives;
    }

    public static List<Live> getByLives() {
        return newLives;
    }

    public static Vod getByKey(String key) {
        return indexVodMap.get(key);
    }

    public static Vod getByUrl(String url) {
        String key = urlKeyMap.get(url);
        if (null == key) {
            return null;
        }
        return indexVodMap.get(key);
    }


    public static String liveNext(Integer tagIndexNow, Integer detailIndexNow, String nextType) {
        if (nextType.equals("up")) {
            if (detailIndexNow == 0) {
                return tagIndexNow + "_" + tagMaxMap.get(tagIndexNow);
            }
            return tagIndexNow + "_" + (detailIndexNow - 1);
        }
        if (nextType.equals("down")) {
            if (Objects.equals(detailIndexNow, tagMaxMap.get(tagIndexNow))) {
                return tagIndexNow + "_0";
            }
            return tagIndexNow + "_" + (detailIndexNow + 1);
        }
        if (nextType.equals("left")) {
            if (tagIndexNow == 0) {
                return (tagMaxMap.size() - 1) + "_0";
            }
            return (tagIndexNow - 1) + "_0";
        }
        if (nextType.equals("right")) {
            if (tagIndexNow.equals(tagMaxMap.size() - 1)) {
                return "0_0";
            }
            return (tagIndexNow + 1) + "_0";
        }
        return "0_0";
    }

}
