package com.letianpai.robot.notice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.letianpai.robot.alarm.receiver.AlarmReceiver;
import com.letianpai.robot.control.storage.RobotConfigManager;
import com.letianpai.robot.notice.receiver.NoticeReceiver;

import java.util.Calendar;

/**
 * @author liujunbin
 */
public class GeeUINoticeManager {
    private AlarmManager alarmManager;

    private static GeeUINoticeManager instance;
    private Context mContext;

    private GeeUINoticeManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    public static GeeUINoticeManager getInstance(Context context) {
        synchronized (GeeUINoticeManager.class) {
            if (instance == null) {
                instance = new GeeUINoticeManager(context.getApplicationContext());
            }
            return instance;
        }
    }


    // 创建闹钟
    public void createNotice (int year,int month,int day, int hour, int minute, String title ) {
        Calendar calendar_now = Calendar.getInstance();
        // 获取当前时间和日期
        calendar_now.setTimeInMillis(System.currentTimeMillis());
//        calendar_now.set(year,month,day,hour,minute);
        calendar_now.set(Calendar.DAY_OF_MONTH, day);
        calendar_now.set(Calendar.HOUR_OF_DAY, hour);
        calendar_now.set(Calendar.MINUTE, minute);
        calendar_now.set(Calendar.SECOND, 0);
        calendar_now.set(Calendar.MILLISECOND, 0);
        int requestCode = hour *100 + minute;

        Intent intent = new Intent(mContext, NoticeReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(
                mContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), sender);
        //设置循环
//        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar_now.getTimeInMillis(), 60* 60* 1000 *24,sender);
        RobotConfigManager.getInstance(mContext).setReminderText(requestCode,title);
        RobotConfigManager.getInstance(mContext).commit();

    }

}