package com.letianpai.robot.control.floating;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.letianpai.robot.control.floating.statusbar.StatusBar;

/**
 * @author liujunbin
 */
public class FloatingViewService extends Service {

    private WindowManager mWindowManager;
    private StatusBar statusBar;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化WindowManager

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 加载悬浮窗口的布局文件
        statusBar = new StatusBar(FloatingViewService.this);

        // 设置悬浮窗口的参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;

        // 将视图添加到WindowManager中
        mWindowManager.addView(statusBar, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 在服务销毁时移除视图
        if (statusBar != null) {
            mWindowManager.removeView(statusBar);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}