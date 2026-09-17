package com.daoxuan.cctv.call;

import java.io.File;

public interface DownloadProgressListener {
    void onDownloadProgress(long sumReaded, long content, boolean done);
    void onDownloadResult(File target, boolean done);
    void onFailResponse();
}
