package com.letianpai.robot.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.letianpai.robot.alarm.receiver.AlarmReceiver;

import java.util.Calendar;
import java.util.Date;

/**
 * @author liujunbin
 */
public class GeeUIAlarmManager {
    private AlarmManager alarmManager;

    private static GeeUIAlarmManager instance;
    private Context mContext;

    private GeeUIAlarmManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    public static GeeUIAlarmManager getInstance(Context context) {
        synchronized (GeeUIAlarmManager.class) {
            if (instance == null) {
                instance = new GeeUIAlarmManager(context.getApplicationContext());
            }
            return instance;
        }
    }


    // 创建闹钟
    public void createAlarm(int hour, int minute) {
        Calendar calendar_now = Calendar.getInstance();
        // 获取当前时间和日期
        int currentHour = calendar_now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar_now.get(Calendar.MINUTE);

        calendar_now.setTimeInMillis(System.currentTimeMillis());
        calendar_now.set(Calendar.HOUR_OF_DAY, hour);
        calendar_now.set(Calendar.MINUTE, minute);
        calendar_now.set(Calendar.SECOND, 0);
        calendar_now.set(Calendar.MILLISECOND, 0);
        int requestCode = hour * 100 + minute;

        if (currentHour > hour || (currentHour == hour && currentMinute >= minute)) {
            // 如果当前时间已经过了 2:00，就将闹钟设置为第二天的 2:00
            calendar_now.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(mContext, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), sender);
        //设置循环
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), 60* 60* 1000 *24,sender);
    }

    // 创建闹钟
    public void createAlarmNew(int year, int month, int day, int hour, int minute) {
        Calendar calendar_now = Calendar.getInstance();
        // 获取当前时间和日期
        calendar_now.setTimeInMillis(System.currentTimeMillis());
//        calendar_now.set(year,month,day,hour,minute);
        calendar_now.set(Calendar.DAY_OF_MONTH, day);
        calendar_now.set(Calendar.HOUR_OF_DAY, hour);
        calendar_now.set(Calendar.MINUTE, minute);
        calendar_now.set(Calendar.SECOND, 0);
        calendar_now.set(Calendar.MILLISECOND, 0);
        int requestCode = hour * 100 + minute;

        Intent intent = new Intent(mContext, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(
                mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), sender);
        //设置循环
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), 60* 60* 1000 *24,sender);
    }

    public void cancelAlarm(int hour, int minute) {
        int requestCode = hour * 100 + minute;
        Intent intent = new Intent(mContext, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, 0);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

}