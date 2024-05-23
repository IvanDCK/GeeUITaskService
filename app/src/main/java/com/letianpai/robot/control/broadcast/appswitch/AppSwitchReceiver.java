package com.letianpai.robot.control.broadcast.appswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author liujunbin
 */
public class AppSwitchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();
    }



}
