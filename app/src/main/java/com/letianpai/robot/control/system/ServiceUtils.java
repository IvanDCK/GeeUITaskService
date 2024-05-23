package com.letianpai.robot.control.system;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * @author liujunbin
 */
public class ServiceUtils {
    /**
     * 判断服务是否开启
     *
     * @return
     */
    public static boolean isServiceRunning(Context context, String serviceName) {
        if (TextUtils.isEmpty(serviceName)) {
            return false;
        }

        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(200);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i) != null && runningService.get(i).service != null && runningService.get(i).service.getClassName() != null) {
                if (runningService.get(i).service.getClassName().equals(serviceName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
