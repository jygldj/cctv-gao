package com.daoxuan.cctv.domain;

import java.util.List;
import java.util.Map;

public class ConfigDTO {

    private String api;
    private Map<String,List<String>> x5Url;
    private List<VersionData> datas;
    private ApkInfo apk;
    private Res res;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Res getRes() {
        return res;
    }

    public void setRes(Res res) {
        this.res = res;
    }

    public ApkInfo getApk() {
        return apk;
    }

    public void setApk(ApkInfo apk) {
        this.apk = apk;
    }



    public Map<String, List<String>> getX5Url() {
        return x5Url;
    }

    public void setX5Url(Map<String, List<String>> x5Url) {
        this.x5Url = x5Url;
    }

    public List<VersionData> getDatas() {
        return datas;
    }

    public void setDatas(List<VersionData> datas) {
        this.datas = datas;
    }
}
