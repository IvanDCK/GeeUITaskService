package com.letianpai.robot.notice.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.letianpai.robot.control.storage.RobotConfigManager
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
class NoticeReceiver : BroadcastReceiver() {
    var calendar: Calendar? = null

    override fun onReceive(context: Context, intent: Intent) {
        calendar = Calendar.getInstance()
        val hour: Int = calendar!!.get(Calendar.HOUR_OF_DAY)
        val minute: Int = calendar!!.get(Calendar.MINUTE)
        //        String title = RobotConfigManager.getInstance(context).getNoticeTitle();
        val title: String? =
            RobotConfigManager.getInstance(context)!!.getRemindText(hour * 100 + minute)
        NoticeCallback.instance.setNoticeTimeOut(hour, minute, title)
    }
}
