package com.daoxuan.cctv.domain;

public class Res {
    private Integer version;
    private String url;
    private Boolean skipFirst;
    private Boolean update;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getSkipFirst() {
        return skipFirst;
    }

    public void setSkipFirst(Boolean skipFirst) {
        this.skipFirst = skipFirst;
    }

    public Boolean getUpdate() {
        return update;
    }

    public void setUpdate(Boolean update) {
        this.update = update;
    }
}
