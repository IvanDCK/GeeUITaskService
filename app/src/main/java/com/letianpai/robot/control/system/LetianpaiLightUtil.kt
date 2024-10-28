package com.letianpai.robot.control.system

import android.content.Context
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException

/**
 * @author liujunbin
 */
object LetianpaiLightUtil {
    fun getLightMode(context: Context): Int {
        // Getting the screen brightness mode
        var mode: Int = 0
        try {
            mode = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }
        return mode
    }

    /**
     * Get the current screen brightness value
     *
     * @param context
     * @return
     */
    fun getCurrentBrightness(context: Context): Int {
        var currentBrightness: Int = 0
        try {
            currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }
        return currentBrightness
    }

    /**
     * Setting the screen brightness mode to manual
     * @param context
     */
    fun setScreenBrightnessToManualMode(context: Context) {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
    }

    /**
     * Set the screen brightness value to
     * @param context
     */
    fun setScreenBrightness(context: Context, value: Int) {
        //  50（The range of values is 0~255）
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value
        )
    }

    /**
     * Set the screen brightness value to
     * @param context
     */
    fun setScreensBrightness(context: Context, value: Int) {
        // 50（The range of values is 0~255）
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value
        )
    }

    /**
     * Set the screen brightness value to
     * @param context
     */
    fun setScreensBrightness1(context: Context, value: Int) {
        // 50（The range of values is 0~255））
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value
        )
    }
}
