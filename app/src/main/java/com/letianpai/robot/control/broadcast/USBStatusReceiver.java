package com.letianpai.robot.control.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.letianpai.robot.components.utils.GeeUILogUtils;


/**
 * USB
 *
 * @author liujunbin
 */
public class USBStatusReceiver extends BroadcastReceiver {
    private Context mContext;
    public final static String ACTION = "android.hardware.usb.action.USB_STATE";


    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;

        String action = intent.getAction();
        if (action.equals(ACTION)) {
            boolean connected = intent.getExtras().getBoolean("connected");
            if (connected) {
                GeeUILogUtils.logi("letianpai_test1111","connect_true");
            } else {
                GeeUILogUtils.logi("letianpai_test1111","connect_false");
            }
        }
    }








}
