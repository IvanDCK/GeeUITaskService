package com.letianpai.robot.ota.broadcast

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log


class PackageInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 获取安装的包名
        val packageName: String = intent.data!!.schemeSpecificPart
        Log.e("letianpai_install", "packageName:" + packageName)
        if (APP_STORE_PACKAGE_NAME == packageName) {
            // 处理APK安装事件
            // 在这里可以执行相应的操作

            Thread(object : Runnable {
                override fun run() {
                    try {
                        Thread.sleep((3 * 1000).toLong())
                        val intent: Intent = Intent()
                        val cn: ComponentName = ComponentName(
                            "com.letianpai.robot.appstore",
                            "com.letianpai.robot.appstore.service.AppStoreService"
                        )
                        intent.setComponent(cn)
                        context.startService(intent)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }).start()
        }
    }


    companion object {
        private const val APP_STORE_PACKAGE_NAME: String = "com.letianpai.robot.appstore"
    }
}