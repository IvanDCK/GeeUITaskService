package com.letianpai.robot.control.system;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

/**
 * @author liujunbin
 */
public class LetianpaiLightUtil {

    public static int getLightMode(Context context) {
        // 获取屏幕亮度模式
        int mode = 0;
        try {
            mode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return mode;
    }

    /**
     * 获取当前屏幕亮度值
     *
     * @param context
     * @return
     */
    public static int getCurrentBrightness(Context context) {
        int currentBrightness = 0;
        try {
            currentBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return currentBrightness;
    }

    /**
     * 设置屏幕亮度模式为手动模式
     * @param context
     */
    public static void setScreenBrightnessToManualMode(Context context) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    /**
     * 设置屏幕亮度值为
     * @param context
     */
    public static void setScreenBrightness(Context context,int value) {
        //  50（取值范围为 0~255）
        Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value);
    }

    /**
     * 设置屏幕亮度值为
     * @param context
     */
    public static void setScreensBrightness(Context context,int value) {
        //  50（取值范围为 0~255）
        Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value);
    }

    /**
     * 设置屏幕亮度值为
     * @param context
     */
    public static void setScreensBrightness1(Context context,int value) {
        //  50（取值范围为 0~255）
        Settings.System.putInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,value);
    }

}
