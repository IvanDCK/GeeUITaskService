package com.letianpai.robot.control.broadcast.appswitch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * @author liujunbin
 */
class AppSwitchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data!!.schemeSpecificPart
    }
}
