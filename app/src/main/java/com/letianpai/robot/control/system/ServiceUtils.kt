package com.letianpai.robot.control.system

import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils

/**
 * @author liujunbin
 */
object ServiceUtils {
    /**
     * Determine if the service is on
     *
     * @return
     */
    fun isServiceRunning(context: Context, serviceName: String): Boolean {
        if (TextUtils.isEmpty(serviceName)) {
            return false
        }

        val myManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningService: ArrayList<ActivityManager.RunningServiceInfo?> =
            myManager.getRunningServices(200) as ArrayList<ActivityManager.RunningServiceInfo?>
        for (i in runningService.indices) {
            if (runningService[i] != null && runningService[i]!!.service != null) {
                if (runningService[i]!!.service.className == serviceName) {
                    return true
                }
            }
        }
        return false
    }
}
