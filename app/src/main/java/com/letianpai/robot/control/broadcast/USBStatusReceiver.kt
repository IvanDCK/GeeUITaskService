package com.letianpai.robot.control.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.letianpai.robot.components.utils.GeeUILogUtils

/**
 * USB
 *
 * @author liujunbin
 */
class USBStatusReceiver : BroadcastReceiver() {
    private var mContext: Context? = null
    override fun onReceive(context: Context, intent: Intent) {
        this.mContext = context

        val action = intent.action
        if (action == ACTION) {
            val connected = intent.extras!!.getBoolean("connected")
            if (connected) {
                GeeUILogUtils.logi("letianpai_test1111", "connect_true")
            } else {
                GeeUILogUtils.logi("letianpai_test1111", "connect_false")
            }
        }
    }


    companion object {
        const val ACTION: String = "android.hardware.usb.action.USB_STATE"
    }
}
