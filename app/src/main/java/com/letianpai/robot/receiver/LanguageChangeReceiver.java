package com.letianpai.robot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import com.letianpai.robot.response.app.AppCmdResponser;

public class LanguageChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            Configuration newConfig = context.getResources().getConfiguration();
            String newLanguage = newConfig.locale.getLanguage();
            //语言发生改变，重新获取数据
            AppCmdResponser.getInstance(context).getUserAppsConfig();
            Log.d("LanguageChangeReceiver", "New language: " + newLanguage);
        }
    }
}
