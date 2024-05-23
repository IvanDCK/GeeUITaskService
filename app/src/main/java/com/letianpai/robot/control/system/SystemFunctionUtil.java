package com.letianpai.robot.control.system;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.renderscript.Allocation.USAGE_SCRIPT;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.WindowManager;

import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback;
import com.renhejia.robot.commandlib.consts.MCUCommandConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author liujunbin
 */
public class SystemFunctionUtil {
    PowerManager.WakeLock wakeLock;

    /**
     * 判断是否安装应用程序
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return appInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断是海外还是国内
     * @return
     */
    public static boolean isChinese(){
        String isChinese =  SystemUtil.get(SystemUtil.REGION_LANGUAGE, "zh");
        return isChinese != null && isChinese.equals("zh");
    }

    /**
     * 关机
     *
     * @param context
     */
    public static void shutdownRobot(Context context) {
        LetianpaiFunctionUtil.updateModeStatusOnServer(context, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME);
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_RESET_MCU, MCUCommandConsts.COMMAND_TYPE_RESET_MCU);
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        Class clazz = pm.getClass();
        try {
            Method shutdown = clazz.getMethod("shutdown", boolean.class, String.class, boolean.class);
            shutdown.invoke(pm, false, "shutdown", false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * 重启
     *
     * @param context
     */
    public static void reboot(Context context) {
        LetianpaiFunctionUtil.updateModeStatusOnServer(context, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME);
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_RESET_MCU, MCUCommandConsts.COMMAND_TYPE_RESET_MCU);
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);

        if (pm != null) {
            pm.reboot(null);
        }
    }


    /**
     * 清理用户数据
     *
     * @param packageName
     * @return
     */
    public static Process clearAppUserData(String packageName) {
        Process p = execRuntimeProcess("pm clear " + packageName);
        if (p == null) {
            GeeUILogUtils.logi("Letianpai", "Clear app data packageName:" + packageName
                    + ", FAILED !");
        } else {
            GeeUILogUtils.logi("Letianpai", "Clear app data packageName:" + packageName
                    + ", SUCCESS !");
        }
        return p;
    }

    /**
     * @param commond
     * @return
     */
    public static Process execRuntimeProcess(String commond) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(commond);
            GeeUILogUtils.logi("Letianpai", "exec Runtime commond:" + commond + ", Process:" + p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return p;
    }

    /**
     * 重启
     *
     * @param context
     */
    public static void screenOff(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        Class clazz = pm.getClass();
        try {
            Method shutdown = clazz.getMethod("reboot", boolean.class, String.class, boolean.class);
            shutdown.invoke(pm, false, "reboot", false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 重启
     *
     * @param context
     */
    public static void screenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        Class clazz = pm.getClass();
        try {
            Method shutdown = clazz.getMethod("reboot", boolean.class, String.class, boolean.class);
            shutdown.invoke(pm, false, "reboot", false);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
          * 唤醒屏幕
          *
          * @param context
          */
    public static void wakeUp(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        GeeUILogUtils.logi("letianpai_sleep_screen","powerManager.isInteractive()2: "+ powerManager.isInteractive());
        if (powerManager.isInteractive()){
            return;
        }
        try {
            powerManager.getClass().getMethod("wakeUp", new Class[]{long.class}).invoke(powerManager, SystemClock.uptimeMillis());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

//    /**
//     * 重启
//     * @param context
//     */
//    public static void goToSleep(Context context) {
//        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
//        Class clazz = pm.getClass();
//        try {
//            Method shutdown = clazz.getMethod("goToSleep", long.class, String.class);
//            long current = System.currentTimeMillis()+ 5000;
//            shutdown.invoke(pm, current, "goToSleep");
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//
//    }

    /**
     * 关闭屏幕 ，其实是使系统休眠
     */
//    public static void goToSleep(Context context) {
//        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        try {
//            powerManager.getClass().getMethod("goToSleep", new Class[]{long.class}).invoke(powerManager, SystemClock.uptimeMillis());
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
//    }

    public static void goToSleep(Context context)  {
        GeeUILogUtils.logi("letianpai_sleep_test_repeat", "=========== 2 =======" );
        PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        try {
            powerManager.getClass()
                    .getMethod("goToSleep",new Class[]{long.class,Integer.TYPE,Integer.TYPE})
                    .invoke(powerManager, SystemClock.uptimeMillis(),0,0);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭屏幕 ，但是系统不休眠
     */
    public static void goToSleep1(Context context) {

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyApp::MyWakelockTag");
        wakeLock.acquire();

    }


//    public static void wakeUp(Context context) {
//        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        try {
//            powerManager.getClass().getMethod("wakeUp", new Class[]{long.class}).invoke(powerManager, SystemClock.uptimeMillis());
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 设置背光亮度
     *
     * @param context
     * @param brightness The brightness value from 0 to 255.
     */
    public static void setBacklightBrightness(Context context, int brightness) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        try {
//            powerManager.getClass().getMethod("setBacklightBrightness", new Class[]{int.class}).invoke(powerManager, brightness);
            powerManager.getClass().getMethod("setBacklightBrightness", new Class[]{Integer.class}).invoke(powerManager, brightness);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置东八区时区
     *
     * @param context
     */
    public static void setTimeZone(Context context) {
//        ((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).setTimeZone("Asia/Tokyo");
//        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTimeZone("Asia/Shanghai");
    }

    /**
     * 设置时区
     *
     * @param context
     */
    public static void set24HourFormat(Context context) {
        android.provider.Settings.System.putString(context.getContentResolver(),
                android.provider.Settings.System.TIME_12_24, "24");
        String timeFormat = android.provider.Settings.System.getString(context.getContentResolver(), android.provider.Settings.System.TIME_12_24);
        GeeUILogUtils.logi("letianpai_time","set24HourFormat: "+ "set24HourFormat: "+ timeFormat);
    }

    public static void set12HourFormat(Context context) {
        android.provider.Settings.System.putString(context.getContentResolver(),
                android.provider.Settings.System.TIME_12_24, "12");
        String timeFormat = android.provider.Settings.System.getString(context.getContentResolver(), android.provider.Settings.System.TIME_12_24);
        GeeUILogUtils.logi("letianpai_time","set24HourFormat: "+ "set12HourFormat: "+ timeFormat);
    }

    public static void set1224HourFormat(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            String timeFormat = android.provider.Settings.System.getString(context.getContentResolver(), android.provider.Settings.System.TIME_12_24);
            if (timeFormat.equals("24")) {
                android.provider.Settings.System.putString(context.getContentResolver(), android.provider.Settings.System.TIME_12_24, "12");
            } else {
                android.provider.Settings.System.putString(context.getContentResolver(), android.provider.Settings.System.TIME_12_24, "24");
            }
            String timeFormat1 = android.provider.Settings.System.getString(context.getContentResolver(), android.provider.Settings.System.TIME_12_24);
        }
    }

    private static void setScreenBrightOFF(Activity context) {
        // 获取当前屏幕亮度值
        try {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            GeeUILogUtils.logi("letianpai_", "currentBrightness: " + currentBrightness);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        // 设置屏幕亮度值
        int newBrightness = 100; // 0 到 255 之间的整数，代表亮度值
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);

        // 应用设置
        WindowManager.LayoutParams layoutParams = context.getWindow().getAttributes();
        layoutParams.screenBrightness = newBrightness / 255.0f; // 将亮度值转换为 0 到 1 之间的浮点数
        context.getWindow().setAttributes(layoutParams);
    }

    private static void setScreenBrightOn(Activity context) {
        // 获取当前屏幕亮度值
        try {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            GeeUILogUtils.logi("letianpai_", "currentBrightness: " + currentBrightness);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        // 设置屏幕亮度值
        int newBrightness = 100; // 0 到 255 之间的整数，代表亮度值
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);

        // 应用设置
        WindowManager.LayoutParams layoutParams = context.getWindow().getAttributes();
        layoutParams.screenBrightness = newBrightness / 255.0f; // 将亮度值转换为 0 到 1 之间的浮点数
        context.getWindow().setAttributes(layoutParams);
    }


    /**
     * @param version1
     * @param version2
     * @return
     * @a
     */
    public static boolean compareVersion(String version1, String version2) {
        // 切割点 "."；
        String[] versionArray1 = version1.split("\\.");
        String[] versionArray2 = version2.split("\\.");
        int idx = 0;
        // 取最小长度值
        int minLength = Math.min(versionArray1.length, versionArray2.length);
        int diff = 0;
        // 先比较长度 再比较字符
        while (idx < minLength && (diff = versionArray1[idx].length() - versionArray2[idx].length()) == 0
                && (diff = versionArray1[idx].compareTo(versionArray2[idx])) == 0) {
            ++idx;
        }
        // 如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
        diff = (diff != 0) ? diff : versionArray1.length - versionArray2.length;
        return diff > 0;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            //如果仅仅是用来判断网络连接
            //则可以使用 cm.getActiveNetworkInfo().isAvailable();
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取蓝牙地址
     *
     * @return
     */
    public static String getBtAddressByReflection() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Field field = null;
        try {
            field = BluetoothAdapter.class.getDeclaredField("mService");
            field.setAccessible(true);
            Object bluetoothManagerService = field.get(bluetoothAdapter);
            if (bluetoothManagerService == null) {
                return null;
            }
            Method method = bluetoothManagerService.getClass().getMethod("getAddress");
            if (method != null) {
                Object obj = method.invoke(bluetoothManagerService);
                if (obj != null) {
                    return obj.toString();
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前wifi名字
     *
     * @param context
     * @return
     */
    public static String getConnectWifiSsid(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getSSID();
    }

    // 5分19帧   7秒19帧
    //bitmap = rsBlur(mContext,bitmap,10,1);

    /**
     * 高斯模糊
     *
     * @param context
     * @param source
     * @param radius
     * @param scale
     * @return
     */
    private static Bitmap rsBlur(Context context, Bitmap source, float radius, float scale) {
        if (source == null) {
            return null;
        }
        int scaleWidth = (int) (source.getWidth() * scale);
        int scaleHeight = (int) (source.getHeight() * scale);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, scaleWidth,
                scaleHeight, false);

        Bitmap inputBitmap = scaledBitmap;
        Log.i("RenderScriptActivity", "size:" + inputBitmap.getWidth() + "," + inputBitmap.getHeight());

        //创建RenderScript
        RenderScript renderScript = RenderScript.create(context);

        //创建Allocation
        Allocation input = Allocation.createFromBitmap(renderScript, inputBitmap, Allocation.MipmapControl.MIPMAP_NONE, USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(renderScript, input.getType());

        //创建ScriptIntrinsic
        ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));

        intrinsicBlur.setInput(input);

        intrinsicBlur.setRadius(radius);

        intrinsicBlur.forEach(output);

        output.copyTo(inputBitmap);

        renderScript.destroy();

        return inputBitmap;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        //声明将要创建的bitmap
        Bitmap bitmap = null;
        //获取图片宽度
        int width = drawable.getIntrinsicWidth();
        //获取图片高度
        int height = drawable.getIntrinsicHeight();
        //图片位深，PixelFormat.OPAQUE代表没有透明度，RGB_565就是没有透明度的位深，否则就用ARGB_8888。详细见下面图片编码知识。
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        //创建一个空的Bitmap
        bitmap = Bitmap.createBitmap(width, height, config);
        //在bitmap上创建一个画布
        Canvas canvas = new Canvas(bitmap);
        //设置画布的范围
        drawable.setBounds(0, 0, width, height);
        //将drawable绘制在canvas上
        drawable.draw(canvas);
        return bitmap;

    }

    public static Bitmap getBitmap(Context context) {
//        Bitmap bitmap = drawableToBitamp(context.getDrawable(R.drawable.test_background));
//        return rsBlur(context,bitmap,10,1);
        return null;
    }
}
