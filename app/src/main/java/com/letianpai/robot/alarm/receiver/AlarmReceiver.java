package com.letianpai.robot.alarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    Calendar calendar_now;

    @Override
    public void onReceive(Context context, Intent intent) {
        calendar_now = Calendar.getInstance();
        int hour = calendar_now.get(Calendar.HOUR_OF_DAY);
        int minute = calendar_now.get(Calendar.MINUTE);
        AlarmCallback.getInstance().setAlarmTimeout(hour, minute);
    }
}
