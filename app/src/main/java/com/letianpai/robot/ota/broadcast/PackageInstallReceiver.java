package com.letianpai.robot.ota.broadcast;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class PackageInstallReceiver extends BroadcastReceiver {
    private static final String APP_STORE_PACKAGE_NAME = "com.letianpai.robot.appstore";
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取安装的包名
        String packageName = intent.getData().getSchemeSpecificPart();
        Log.e("letianpai_install","packageName:"+ packageName );
        if (APP_STORE_PACKAGE_NAME.equals(packageName)) {
            // 处理APK安装事件
            // 在这里可以执行相应的操作

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3 * 1000);
                        Intent intent = new Intent();
                        ComponentName cn = new ComponentName("com.letianpai.robot.appstore", "com.letianpai.robot.appstore.service.AppStoreService");
                        intent.setComponent(cn);
                        context.startService(intent);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }


    }


}