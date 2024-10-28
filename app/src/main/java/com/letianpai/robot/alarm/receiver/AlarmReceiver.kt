package com.letianpai.robot.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.letianpai.robot.response.app.AppCmdResponser.Companion.getInstance
import com.letianpai.robot.response.ble.BleCmdResponser.Companion.getInstance
import com.letianpai.robot.response.identify.IdentifyCmdResponser.Companion.getInstance
import com.letianpai.robot.response.mi.MiIotCmdResponser.Companion.getInstance
import com.letianpai.robot.response.remote.RemoteCmdResponser.Companion.getInstance
import com.letianpai.robot.response.robotStatus.RobotStatusResponser.Companion.getInstance
import com.letianpai.robot.response.sensor.SensorCmdResponser.Companion.getInstance
import com.letianpai.robot.response.speech.SpeechCmdResponser.Companion.getInstance
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    var calendar_now: Calendar? = null

    override fun onReceive(context: Context, intent: Intent) {
        calendar_now = Calendar.getInstance()
        val hour = calendar_now!!.get(Calendar.HOUR_OF_DAY)
        val minute = calendar_now!!.get(Calendar.MINUTE)
        AlarmCallback.instance.setAlarmTimeout(hour, minute)
    }
}
