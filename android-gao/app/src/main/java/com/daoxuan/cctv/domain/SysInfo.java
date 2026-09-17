package com.daoxuan.cctv.domain;

public class SysInfo {
    private String versionName;
    private Boolean sys64;
    private Boolean x86;
    private Integer versionCode;
    private Boolean haveNew;
    private Boolean x5Ok;
    private String cacheSize;
    private String deviceId;
    private String resVersion;
    private Boolean openOkMenu;

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }



    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Integer versionCode) {
        this.versionCode = versionCode;
    }

    public Boolean getHaveNew() {
        return haveNew;
    }

    public void setHaveNew(Boolean haveNew) {
        this.haveNew = haveNew;
    }

    public Boolean getX5Ok() {
        return x5Ok;
    }

    public void setX5Ok(Boolean x5Ok) {
        this.x5Ok = x5Ok;
    }

    public String getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(String cacheSize) {
        this.cacheSize = cacheSize;
    }

    public Boolean getSys64() {
        return sys64;
    }

    public void setSys64(Boolean sys64) {
        this.sys64 = sys64;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Boolean getX86() {
        return x86;
    }

    public void setX86(Boolean x86) {
        this.x86 = x86;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getResVersion() {
        return resVersion;
    }

    public void setResVersion(String resVersion) {
        this.resVersion = resVersion;
    }

    public Boolean getOpenOkMenu() {
        return openOkMenu;
    }

    public void setOpenOkMenu(Boolean openOkMenu) {
        this.openOkMenu = openOkMenu;
    }
}
