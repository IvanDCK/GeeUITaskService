package com.letianpai.robot.taskservice

import android.app.Application
import com.letianpai.robot.components.utils.GeeUILogUtils

class TaskServiceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GeeUILogUtils.initXlog2("taskservice", this)
    }
}
