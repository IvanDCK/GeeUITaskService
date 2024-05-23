package com.letianpai.robot.taskservice;

import android.app.Application;
import com.letianpai.robot.components.utils.GeeUILogUtils;

public class TaskServiceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GeeUILogUtils.initXlog2("taskservice", this);
    }
}
