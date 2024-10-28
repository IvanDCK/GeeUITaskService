package com.letianpai.robot.control.floating

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import com.letianpai.robot.control.floating.statusbar.StatusBar

/**
 * @author liujunbin
 */
class FloatingViewService : Service() {
    private var mWindowManager: WindowManager? = null
    private var statusBar: StatusBar? = null

    override fun onCreate() {
        super.onCreate()

        // Initialise WindowManager
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager?

        // Load the layout file for the hover window
        statusBar = StatusBar(this@FloatingViewService)

        // Setting the parameters of the hover window
        val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  //                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            //                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            //                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START

        // Adding Views to WindowManager
        mWindowManager!!.addView(statusBar, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Removing Views on Service Destruction
        if (statusBar != null) {
            mWindowManager!!.removeView(statusBar)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}