package com.letianpai.robot.notice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.letianpai.robot.alarm.receiver.AlarmCallback;
import com.letianpai.robot.control.storage.RobotConfigManager;

import java.util.Calendar;

/**
 * @author liujunbin
 */
public class NoticeReceiver extends BroadcastReceiver {

    Calendar calendar;

    @Override
    public void onReceive(Context context, Intent intent) {
        calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
//        String title = RobotConfigManager.getInstance(context).getNoticeTitle();
        String title = RobotConfigManager.getInstance(context).getRemindText(hour *100 + minute);
        NoticeCallback.getInstance().setNoticeTimeOut(hour,minute,title);
    }
}
