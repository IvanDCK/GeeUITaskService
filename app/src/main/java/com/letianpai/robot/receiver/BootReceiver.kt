package com.letianpai.robot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // LetianpaiFunctionUtil.openTime(context);
            //开机之后，响应动一动的动作
            // RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_POWER_ON_CHARGING, 1);
        }
    }
}
