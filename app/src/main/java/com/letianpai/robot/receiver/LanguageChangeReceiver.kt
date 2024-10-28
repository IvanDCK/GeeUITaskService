package com.letianpai.robot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import com.letianpai.robot.response.app.AppCmdResponser

class LanguageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_LOCALE_CHANGED == intent.action) {
            val newConfig: Configuration = context.resources.configuration
            val newLanguage: String = newConfig.locale.language
            //Language change, reacquisition of data
            AppCmdResponser.getInstance(context).userAppsConfig
            Log.d("LanguageChangeReceiver", "New language: $newLanguage")
        }
    }
}
