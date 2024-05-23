package com.letianpai.robot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // LetianpaiFunctionUtil.openTime(context);
            //开机之后，响应动一动的动作
            // RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_POWER_ON_CHARGING, 1);
        }
    }
}
