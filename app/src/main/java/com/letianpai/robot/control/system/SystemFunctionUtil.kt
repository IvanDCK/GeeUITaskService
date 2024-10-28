package com.letianpai.robot.control.system

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.SystemClock
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.WindowManager
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback
import com.renhejia.robot.commandlib.consts.MCUCommandConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.math.min

/**
 * @author liujunbin
 */
class SystemFunctionUtil {
    var wakeLock: WakeLock? = null

    companion object {
        /**
         * Determining whether an application is installed
         * @param context
         * @param packageName
         * @return
         */
        fun isAppInstalled(context: Context, packageName: String): Boolean {
            val pm: PackageManager = context.packageManager
            try {
                val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
                return appInfo != null
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                return false
            }
        }

        val isChinese: Boolean
            /**
             * Judging whether overseas or chinese
             * @return
             */
            get() {
                val isChinese: String? =
                    SystemUtil.get(
                        SystemUtil.REGION_LANGUAGE,
                        "zh"
                    )
                return isChinese != null && isChinese == "zh"
            }

        /**
         * finish shooting a film
         *
         * @param context
         */
        fun shutdownRobot(context: Context) {
            LetianpaiFunctionUtil.updateModeStatusOnServer(
                context,
                RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME
            )
            CommandResponseCallback.instance.setLTPCommand(
                MCUCommandConsts.COMMAND_TYPE_RESET_MCU,
                MCUCommandConsts.COMMAND_TYPE_RESET_MCU
            )
            val pm: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val clazz: Class<*> = pm.javaClass
            try {
                val shutdown: Method = clazz.getMethod(
                    "shutdown",
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    Boolean::class.javaPrimitiveType
                )
                shutdown.invoke(pm, false, "shutdown", false)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        /**
         * reopen
         *
         * @param context
         */
        fun reboot(context: Context) {
            LetianpaiFunctionUtil.updateModeStatusOnServer(
                context,
                RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME
            )
            CommandResponseCallback.instance.setLTPCommand(
                MCUCommandConsts.COMMAND_TYPE_RESET_MCU,
                MCUCommandConsts.COMMAND_TYPE_RESET_MCU
            )

            (context.getSystemService(Context.POWER_SERVICE) as PowerManager?)?.reboot(null)
        }


        /**
         * Cleaning up user data
         *
         * @param packageName
         * @return
         */
        fun clearAppUserData(packageName: String): Process? {
            val p: Process? = execRuntimeProcess("pm clear $packageName")
            if (p == null) {
                GeeUILogUtils.logi(
                    "Letianpai", ("Clear app data packageName:" + packageName
                            + ", FAILED !")
                )
            } else {
                GeeUILogUtils.logi(
                    "Letianpai", ("Clear app data packageName:" + packageName
                            + ", SUCCESS !")
                )
            }
            return p
        }

        /**
         * @param commond
         * @return
         */
        fun execRuntimeProcess(commond: String): Process? {
            var p: Process? = null
            try {
                p = Runtime.getRuntime().exec(commond)
                GeeUILogUtils.logi(
                    "Letianpai",
                    "exec Runtime commond:$commond, Process:$p"
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return p
        }

        /**
         * reopen
         *
         * @param context
         */
        fun screenOff(context: Context) {
            val pm: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val clazz: Class<*> = pm.javaClass
            try {
                val shutdown: Method = clazz.getMethod(
                    "reboot",
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    Boolean::class.javaPrimitiveType
                )
                shutdown.invoke(pm, false, "reboot", false)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        /**
         * reopen
         *
         * @param context
         */
        fun screenOn(context: Context) {
            val pm: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val clazz: Class<*> = pm.javaClass
            try {
                val shutdown: Method = clazz.getMethod(
                    "reboot",
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    Boolean::class.javaPrimitiveType
                )
                shutdown.invoke(pm, false, "reboot", false)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        /**
         * wake-up call
         *
         * @param context
         */
        fun wakeUp(context: Context) {
            val powerManager: PowerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            GeeUILogUtils.logi(
                "letianpai_sleep_screen",
                "powerManager.isInteractive()2: " + powerManager.isInteractive
            )
            if (powerManager.isInteractive) {
                return
            }
            try {
                powerManager.javaClass.getMethod(
                    "wakeUp", *arrayOf<Class<*>?>(
                        Long::class.javaPrimitiveType
                    )
                ).invoke(powerManager, SystemClock.uptimeMillis())
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
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
         * Turning off the screen actually puts the system to sleep.
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
        fun goToSleep(context: Context) {
            GeeUILogUtils.logi("letianpai_sleep_test_repeat", "=========== 2 =======")
            val powerManager: PowerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            try {
                powerManager.javaClass
                    .getMethod(
                        "goToSleep",
                        *arrayOf<Class<*>?>(
                            Long::class.javaPrimitiveType,
                            Integer.TYPE,
                            Integer.TYPE
                        )
                    )
                    .invoke(powerManager, SystemClock.uptimeMillis(), 0, 0)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(e)
            }
        }

        /**
         * Turn off the screen, but the system does not hibernate
         */
        fun goToSleep1(context: Context) {
            val powerManager: PowerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock: WakeLock =
                powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyApp::MyWakelockTag")
            wakeLock.acquire()
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
         * Setting Backlight Brightness
         *
         * @param context
         * @param brightness The brightness value from 0 to 255.
         */
        fun setBacklightBrightness(context: Context, brightness: Int) {
            val powerManager: PowerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            try {
//            powerManager.getClass().getMethod("setBacklightBrightness", new Class[]{int.class}).invoke(powerManager, brightness);
                powerManager.javaClass.getMethod(
                    "setBacklightBrightness", *arrayOf<Class<*>>(
                        Int::class.java
                    )
                ).invoke(powerManager, brightness)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }
        }

        /**
         * Setting the time zone for the eastern eight regions
         *
         * @param context
         */
        fun setTimeZone(context: Context?) {
//        ((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).setTimeZone("Asia/Tokyo");
//        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTimeZone("Asia/Shanghai");
        }

        /**
         * Setting the time zone
         *
         * @param context
         */
        fun set24HourFormat(context: Context) {
            Settings.System.putString(
                context.contentResolver,
                Settings.System.TIME_12_24, "24"
            )
            val timeFormat: String =
                Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)
            GeeUILogUtils.logi(
                "letianpai_time",
                "set24HourFormat: set24HourFormat: $timeFormat"
            )
        }

        fun set12HourFormat(context: Context) {
            Settings.System.putString(
                context.contentResolver,
                Settings.System.TIME_12_24, "12"
            )
            val timeFormat: String =
                Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)
            GeeUILogUtils.logi(
                "letianpai_time",
                "set24HourFormat: " + "set12HourFormat: " + timeFormat
            )
        }

        fun set1224HourFormat(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val timeFormat: String = Settings.System.getString(
                    context.contentResolver,
                    Settings.System.TIME_12_24
                )
                if (timeFormat == "24") {
                    Settings.System.putString(
                        context.contentResolver,
                        Settings.System.TIME_12_24,
                        "12"
                    )
                } else {
                    Settings.System.putString(
                        context.contentResolver,
                        Settings.System.TIME_12_24,
                        "24"
                    )
                }
                val timeFormat1: String = Settings.System.getString(
                    context.contentResolver,
                    Settings.System.TIME_12_24
                )
            }
        }

        private fun setScreenBrightOFF(context: Activity) {
            // Get the current screen brightness value
            try {
                val currentBrightness: Int = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                GeeUILogUtils.logi("letianpai_", "currentBrightness: $currentBrightness")
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }

            // Setting the screen brightness value
            val newBrightness: Int = 100 // An integer between 0 and 255, representing the brightness value.
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )

            // 应用设置
            val layoutParams: WindowManager.LayoutParams = context.window.attributes
            layoutParams.screenBrightness = newBrightness / 255.0f // Converts the luminance value to a floating point number between 0 and 1.
            context.window.setAttributes(layoutParams)
        }

        private fun setScreenBrightOn(context: Activity) {
            // Get the current screen brightness value
            try {
                val currentBrightness: Int = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                GeeUILogUtils.logi("letianpai_", "currentBrightness: $currentBrightness")
            } catch (e: SettingNotFoundException) {
                e.printStackTrace()
            }

            // Setting the screen brightness value
            val newBrightness: Int = 100 // An integer between 0 and 255, representing the brightness value.
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                newBrightness
            )

            // 应用设置
            val layoutParams: WindowManager.LayoutParams = context.window.attributes
            layoutParams.screenBrightness = newBrightness / 255.0f // Converts the luminance value to a floating point number between 0 and 1
            context.getWindow().setAttributes(layoutParams)
        }


        /**
         * @param version1
         * @param version2
         * @return
         * @a
         */
        fun compareVersion(version1: String, version2: String): Boolean {
            // Cutting point ‘.’ ;
            val versionArray1: Array<String> =
                version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val versionArray2: Array<String> =
                version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var idx: Int = 0
            // Take the minimum length value
            val minLength: Int =
                min(versionArray1.size.toDouble(), versionArray2.size.toDouble()).toInt()
            var diff: Int = 0
            // Compare lengths before comparing characters.
            while (idx < minLength && ((versionArray1.get(idx).length - versionArray2.get(idx).length).also {
                    diff = it
                }) == 0 && (versionArray1.get(idx).compareTo(versionArray2.get(idx))
                    .also { diff = it }) == 0
            ) {
                ++idx
            }
// If the size has been split, return it directly, if not, compare the bits again, with the subversion being greater;            diff = if ((diff != 0)) diff else versionArray1.size - versionArray2.size
            return diff > 0
        }

        fun isNetworkAvailable(context: Context): Boolean {
            val cm: ConnectivityManager? = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null) {
            } else {
                //If it is only used to determine network connectivity
                // then you can use cm.getActiveNetworkInfo().isAvailable();
                val info: Array<NetworkInfo> = cm.allNetworkInfo
                if (info != null) {
                    for (i in info.indices) {
                        if (info[i].state == NetworkInfo.State.CONNECTED) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        val btAddressByReflection: String?
            /**
             * 获取蓝牙地址
             *
             * @return
             */
            get() {
                val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                var field: Field? = null
                try {
                    field = BluetoothAdapter::class.java.getDeclaredField("mService")
                    field.isAccessible = true
                    val bluetoothManagerService: Any? = field.get(bluetoothAdapter)
                    if (bluetoothManagerService == null) {
                        return null
                    }
                    val method: Method =
                        bluetoothManagerService.javaClass.getMethod("getAddress")
                    if (method != null) {
                        val obj: Any? = method.invoke(bluetoothManagerService)
                        if (obj != null) {
                            return obj.toString()
                        }
                    }
                } catch (e: NoSuchFieldException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
                return null
            }

        /**
         * Get current wifi name
         *
         * @param context
         * @return
         */
        fun getConnectWifiSsid(context: Context): String {
            val wifiManager: WifiManager =
                context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.getConnectionInfo()
            return wifiInfo.getSSID()
        }

        // 5分19帧   7秒19帧
        //bitmap = rsBlur(mContext,bitmap,10,1);
        /**
         * Gaussian blur
         *
         * @param context
         * @param source
         * @param radius
         * @param scale
         * @return
         */
        private fun rsBlur(
            context: Context,
            source: Bitmap?,
            radius: Float,
            scale: Float
        ): Bitmap? {
            if (source == null) {
                return null
            }
            val scaleWidth: Int = (source.getWidth() * scale).toInt()
            val scaleHeight: Int = (source.getHeight() * scale).toInt()
            val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(
                source, scaleWidth,
                scaleHeight, false
            )

            val inputBitmap: Bitmap = scaledBitmap
            Log.i(
                "RenderScriptActivity",
                "size:" + inputBitmap.getWidth() + "," + inputBitmap.getHeight()
            )

            //创建RenderScript
            val renderScript: RenderScript = RenderScript.create(context)

            //创建Allocation
            val input: Allocation = Allocation.createFromBitmap(
                renderScript,
                inputBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            val output: Allocation = Allocation.createTyped(renderScript, input.getType())

            //创建ScriptIntrinsic
            val intrinsicBlur: ScriptIntrinsicBlur =
                ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

            intrinsicBlur.setInput(input)

            intrinsicBlur.setRadius(radius)

            intrinsicBlur.forEach(output)

            output.copyTo(inputBitmap)

            renderScript.destroy()

            return inputBitmap
        }

        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            //Declare the bitmap to be created
            var bitmap: Bitmap? = null
            //Get the width of the image
            val width: Int = drawable.getIntrinsicWidth()
            //Get the height of the image
            val height: Int = drawable.getIntrinsicHeight()
            // Picture bit depth, PixelFormat.OPAQUE stands for no transparency, RGB_565 is the bit depth with no transparency, otherwise use ARGB_8888. see the following picture encoding knowledge for details.
            val config: Bitmap.Config =
                if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            //Create an empty Bitmap
            bitmap = Bitmap.createBitmap(width, height, config)
            //Creating a canvas on a bitmap
            val canvas: Canvas = Canvas(bitmap)
            //Setting the range of the canvas
            drawable.setBounds(0, 0, width, height)
            //Drawing a drawable on a canvas
            drawable.draw(canvas)
            return bitmap
        }

        fun getBitmap(context: Context?): Bitmap? {
//        Bitmap bitmap = drawableToBitamp(context.getDrawable(R.drawable.test_background));
//        return rsBlur(context,bitmap,10,1);
            return null
        }
    }
}
