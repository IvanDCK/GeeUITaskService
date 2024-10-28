package com.letianpai.robot.notice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.notice.receiver.NoticeReceiver
import java.util.Calendar

/**
 * @author liujunbin
 */
class GeeUINoticeManager private constructor(context: Context) {
    private var alarmManager: AlarmManager? = null

    private var mContext: Context? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        this.mContext = context
        alarmManager = mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
    }

    // 创建闹钟
    fun createNotice(year: Int, month: Int, day: Int, hour: Int, minute: Int, title: String) {
        val calendar_now: Calendar = Calendar.getInstance()
        // 获取当前时间和日期
        calendar_now.setTimeInMillis(System.currentTimeMillis())
        //        calendar_now.set(year,month,day,hour,minute);
        calendar_now.set(Calendar.DAY_OF_MONTH, day)
        calendar_now.set(Calendar.HOUR_OF_DAY, hour)
        calendar_now.set(Calendar.MINUTE, minute)
        calendar_now.set(Calendar.SECOND, 0)
        calendar_now.set(Calendar.MILLISECOND, 0)
        val requestCode: Int = hour * 100 + minute

        val intent: Intent = Intent(mContext, NoticeReceiver::class.java)
        val sender: PendingIntent = PendingIntent.getBroadcast(
            mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager = mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager?

        alarmManager!!.set(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), sender)
        //设置循环
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), 60* 60* 1000 *24,sender);
        RobotConfigManager.getInstance(mContext)!!.setReminderText(requestCode, title)
        RobotConfigManager.getInstance(mContext)!!.commit()
    }

    companion object {
        private var instance: GeeUINoticeManager? = null
        fun getInstance(context: Context): GeeUINoticeManager {
            synchronized(GeeUINoticeManager::class.java) {
                if (instance == null) {
                    instance = GeeUINoticeManager(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}