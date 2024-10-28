package com.letianpai.robot.control.manager

import com.letianpai.robot.components.utils.GeeUILogUtils
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future


class GestureDataThreadExecutor private constructor() : Executor {
    private var currentTask: Future<*>? = null
    private val mExecutorService: ExecutorService = Executors.newSingleThreadExecutor()

    override fun execute(task: Runnable) {
        if (currentTask != null && !currentTask!!.isDone) {
            GeeUILogUtils.logi("GestureDataThreadExecutor", "execute: 取消执行的线程")
            currentTask!!.cancel(true)
        } else {
            GeeUILogUtils.logi("GestureDataThreadExecutor", "execute: 不会取消正在执行的线程")
        }
        currentTask = mExecutorService.submit(task)
    }

    companion object {
        private var gestureDataThreadExecutor: GestureDataThreadExecutor? = null

        val instance: GestureDataThreadExecutor
            get() {
                if (gestureDataThreadExecutor == null) {
                    gestureDataThreadExecutor =
                        GestureDataThreadExecutor()
                }
                return gestureDataThreadExecutor!!
            }
    }
}
