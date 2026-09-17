package com.daoxuan.cctv.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.daoxuan.cctv.MainActivity;
import com.daoxuan.cctv.util.ValueUtil;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!"android.intent.action.BOOT_COMPLETED".equals(action) &&
            !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            return;
        }
        String autoStart = ValueUtil.getString(context, "autoStart", "0");
        if (!"1".equals(autoStart)) {
            return;
        }
        Intent start = new Intent(context, MainActivity.class);
        start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(start);
    }
}


