package com.letianpai.robot.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.letianpai.robot.alarm.receiver.AlarmReceiver
import java.util.Calendar

/**
 * @author liujunbin
 */
class GeeUIAlarmManager private constructor(context: Context) {
    private var alarmManager: AlarmManager? = null

    private var mContext: Context? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        this.mContext = context
        alarmManager = mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    // 创建闹钟
    fun createAlarm(hour: Int, minute: Int) {
        val calendar_now = Calendar.getInstance()
        // 获取当前时间和日期
        val currentHour = calendar_now[Calendar.HOUR_OF_DAY]
        val currentMinute = calendar_now[Calendar.MINUTE]

        calendar_now.timeInMillis = System.currentTimeMillis()
        calendar_now[Calendar.HOUR_OF_DAY] = hour
        calendar_now[Calendar.MINUTE] = minute
        calendar_now[Calendar.SECOND] = 0
        calendar_now[Calendar.MILLISECOND] = 0
        val requestCode = hour * 100 + minute

        if (currentHour > hour || (currentHour == hour && currentMinute >= minute)) {
            // 如果当前时间已经过了 2:00，就将闹钟设置为第二天的 2:00
            calendar_now.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(mContext, AlarmReceiver::class.java)
        val sender = PendingIntent.getBroadcast(
            mContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager = mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager!![AlarmManager.RTC_WAKEUP, calendar_now.timeInMillis] = sender
        //Setting up the loop
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), 60* 60* 1000 *24,sender);
    }

    // Create a new alarm
    fun createAlarmNew(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val calendar_now = Calendar.getInstance()
        // Get current time and date
        calendar_now.timeInMillis = System.currentTimeMillis()
        //        calendar_now.set(year,month,day,hour,minute);
        calendar_now[Calendar.DAY_OF_MONTH] = day
        calendar_now[Calendar.HOUR_OF_DAY] = hour
        calendar_now[Calendar.MINUTE] = minute
        calendar_now[Calendar.SECOND] = 0
        calendar_now[Calendar.MILLISECOND] = 0
        val requestCode = hour * 100 + minute

        val intent = Intent(mContext, AlarmReceiver::class.java)
        val sender = PendingIntent.getBroadcast(
            mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager = mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager!![AlarmManager.RTC_WAKEUP, calendar_now.timeInMillis] = sender
        //Setting up the loop
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), 60* 60* 1000 *24,sender);
    }

    fun cancelAlarm(hour: Int, minute: Int) {
        val requestCode = hour * 100 + minute
        val intent = Intent(mContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = mContext!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        private var instance: GeeUIAlarmManager? = null
        fun getInstance(context: Context): GeeUIAlarmManager {
            synchronized(GeeUIAlarmManager::class.java) {
                if (instance == null) {
                    instance = GeeUIAlarmManager(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}