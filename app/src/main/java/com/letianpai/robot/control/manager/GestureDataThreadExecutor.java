package com.letianpai.robot.control.manager;



import androidx.annotation.NonNull;
import com.letianpai.robot.components.utils.GeeUILogUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GestureDataThreadExecutor implements Executor {
    Future<?> currentTask = null;
    private static GestureDataThreadExecutor gestureDataThreadExecutor;

    public static GestureDataThreadExecutor getInstance() {
        if (gestureDataThreadExecutor == null) {
            gestureDataThreadExecutor = new GestureDataThreadExecutor();
        }
        return gestureDataThreadExecutor;
    }

    private final ExecutorService mExecutorService;

    private GestureDataThreadExecutor() {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void execute(@NonNull Runnable task) {
        if (currentTask != null && !currentTask.isDone()) {
            GeeUILogUtils.logi("GestureDataThreadExecutor", "execute: 取消执行的线程");
            currentTask.cancel(true);
        } else {
            GeeUILogUtils.logi("GestureDataThreadExecutor", "execute: 不会取消正在执行的线程");
        }
        currentTask = mExecutorService.submit(task);
    }
}
