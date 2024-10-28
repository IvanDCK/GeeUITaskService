package com.letianpai.robot.control.broadcast.timer

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

/**
 * @author liujunbin
 */
class TimerReceiver : BroadcastReceiver() {
    var calendar: Calendar? = null
    override fun onReceive(context: Context, intent: Intent) {
        calendar = Calendar.getInstance()
        val hour = calendar!!.get(Calendar.HOUR_OF_DAY)
        val min = calendar!!.get(Calendar.MINUTE)
        TimerKeeperCallback.instance.setTimerKeeper(hour, min)
    }
}
