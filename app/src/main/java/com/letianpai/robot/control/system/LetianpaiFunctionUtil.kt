package com.letianpai.robot.control.system

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.ActivityManager.RunningTaskInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.RecoverySystem
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.letianpai.robot.components.network.nets.GeeUiNetManager
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.callback.ControlSteeringEngineCallback
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.callback.RobotCommandWordsCallback
import com.letianpai.robot.control.callback.TemperatureUpdateCallback
import com.letianpai.robot.control.floating.FloatingViewService
import com.letianpai.robot.control.manager.L81AppListManager
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.callback.ModeChangeCallback
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.response.RobotFuncResponseManager.Companion.stopRobot
import com.letianpai.robot.response.app.AppCmdResponser
import com.letianpai.robot.response.robotStatus.RobotStatusResponser
import com.letianpai.robot.taskservice.utils.LTPConfigConsts
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.PackageConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TAKE_PHOTO
import com.renhejia.robot.commandlib.consts.SpeechConst
import com.renhejia.robot.commandlib.parser.config.UserAppsConfig
import com.renhejia.robot.commandlib.parser.config.UserAppsConfigModel
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.letianpaiservice.ILetianpaiService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.lang.reflect.Method
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * @author liujunbin
 */
object LetianpaiFunctionUtil {
    private val TAG: String = "LetianpaiFunctionUtil"
    private const val SPLIT: String = "____"

    //记录当前module，如果相同就不用切换
    var currentPackageName: String = ""

    var packageNames: MutableList<String> = ArrayList(4)

    fun showFloatingView(context: Context) {
        val intent: Intent = Intent(context, FloatingViewService::class.java)
        context.startService(intent)
    }


    /**
     * 打开天气
     *
     * @param context
     */
    fun openWeather(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.WEATHER_PACKAGE_NAME,
            PackageConsts.ACTIVITY_PACKAGE_NAME,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WEATHER
        )
    }

    /**
     * 打开自动充电
     *
     * @param context
     */
    fun openAutoCharging(context: Context): Boolean {
        if (SystemFunctionUtil.isAppInstalled(
                context,
                PackageConsts.PACKAGE_NAME_AUTO_CHARGING
            )
        ) {
            controlSteeringEngine(context, true, true)
            val intent: Intent = Intent()
            intent.setComponent(
                ComponentName(
                    PackageConsts.PACKAGE_NAME_AUTO_CHARGING,
                    PackageConsts.CLASS_NAME_AUTO_CHARGING
                )
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
            RobotCommandWordsCallback.instance.showBattery(false)
            ModeChangeCallback.instance
                .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
            return true
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText()
            return false
        }
    }

    fun startAlarmService(context: Context, data: String?) {
        val intent: Intent = Intent()
        //        ComponentName cn = new ComponentName("com.letianpai.robot.alarm", "com.letianpai.robot.alarm.service.AlarmService");
        val cn: ComponentName = ComponentName(
            "com.letianpai.robot.alarmnotice",
            "com.letianpai.robot.alarmnotice.service.AlarmService"
        )
        intent.setComponent(cn)
        intent.putExtra(SpeechConst.EXTRA_ALARM, data)
        context.startService(intent)
    }

    fun startAlarmService(context: Context, command: String?, data: String?) {
        val intent: Intent = Intent()
        //        ComponentName cn = new ComponentName("com.letianpai.robot.alarm", "com.letianpai.robot.alarm.service.AlarmService");
        val cn: ComponentName = ComponentName(
            "com.letianpai.robot.alarmnotice",
            "com.letianpai.robot.alarmnotice.service.AlarmService"
        )
        intent.setComponent(cn)
        if ((!TextUtils.isEmpty(command)) && (!TextUtils.isEmpty(data))) {
            intent.putExtra(
                SpeechConst.EXTRA_SPEECH_ALARM,
                command + SpeechConst.COMMAND_SPEECH_SPLIT + data
            )
        }
        context.startService(intent)
    }

    fun startAppStoreService(context: Context) {
        Thread(object : Runnable {
            override fun run() {
                try {
                    val intent: Intent = Intent()
                    val cn: ComponentName = ComponentName(
                        "com.letianpai.robot.appstore",
                        "com.letianpai.robot.appstore.service.AppStoreService"
                    )
                    intent.setComponent(cn)
                    context.startService(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }).start()
    }

    /**
     * 打开新闻
     *
     * @param context
     */
    fun openNews(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_NEWS,
            PackageConsts.ACTIVITY_NAME_NEWS,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_NEWS
        )
    }

    /**
     * 打开跑马灯
     *
     * @param context
     */
    fun openLamp(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_LAMP,
            PackageConsts.PACKAGE_NAME_LAMP_ACTIVITY,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_LAMP
        )
    }


    /**
     * 打开自定义桌面
     *
     * @param context
     */
    fun openCustom(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_CUSTOM,
            PackageConsts.CLASS_NAME_CUSTOM,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_CUSTOM,
            true
        )
    }


    /**
     * 打开米家绑定app
     *
     * @param context
     */
    fun openBindMijia(context: Context): Boolean {
        return openApp(context, "com.geeui.miiot", "com.geeui.miiot.MiIoTActivity", "")
    }

    /**
     * 打开纪念日
     *
     * @param context
     */
    fun openCommemoration(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_COMMEMORATION,
            PackageConsts.ACTIVITY_NAME_COMMEMORATION,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_COMMEMORATION
        )
    }

    /**
     * 打开股票
     *
     * @param context
     */
    fun openStock(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.STOCK_PACKAGE_NAME,
            PackageConsts.PACKAGE_NAME_STOCK_ACTIVITY,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_STOCK
        )
    }

    /**
     * 打开单词
     *
     * @param context
     */
    fun openWord(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_WORDS,
            PackageConsts.PACKAGE_NAME_WORDS_ACTIVITY,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WORD
        )
    }

    /**
     * 打开粉丝
     *
     * @param context
     */
    fun openFans(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_FANS,
            PackageConsts.ACTIVITY_NAME_FANS,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_FANS
        )
    }

    /**
     * 打开表情
     *
     * @param context
     */
    fun openExpressionApp(context: Context): Boolean {
        return openApp(
            context,
            "com.letianpai.robot.expression",
            "com.letianpai.robot.expression.ui.activity.MainActivity",
            "com.letianpai.robot.expression"
        )
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    fun openEventCountdown(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_COUNT_DOWN,
            PackageConsts.ACTIVITY_NAME_COUNT_DOWN,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_EVENT
        )
    }

    fun openUniversalApp(context: Context, name: String, iLetianpaiService: ILetianpaiService) {
        GeeUILogUtils.logi("----" + TAG, "openUniversalApp::name:::" + name)

        if (!TextUtils.isEmpty(name) && name.contains(SPLIT)) {
            val appInfo: Array<String> =
                name.split(SPLIT.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (appInfo != null && appInfo.size >= 2) {
                openApp(context, appInfo.get(0), appInfo.get(1), appInfo.get(0))
            }
        } else {
            //判断如果这个包名不在我们的APP中，第三方安装应用就本地打开
            if (name.contains(".") && !L81AppListManager.getInstance(context)
                    .isInThePackageList(name)
            ) {
                try {
                    val intent: Intent? =
                        context.getPackageManager().getLaunchIntentForPackage(name)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        killProcessOfPackageName(context, name)
                    } else {
                        GeeUILogUtils.logi("----" + TAG, "openUniversalApp::无法打开该应用")
                    }
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    GeeUILogUtils.logi("----", Log.getStackTraceString(e))
                }
                return
            }

            val userAppsConfigModel: UserAppsConfigModel? =
                AppCmdResponser.getInstance(context).userAppsConfigModel
            GeeUILogUtils.logi(
                "----" + TAG,
                "openUniversalApp:userAppsConfigModel.getData().isEmpty():" + userAppsConfigModel!!.data!!.isEmpty()
            )

            if (userAppsConfigModel != null && !userAppsConfigModel.data!!.isEmpty()) {
                val list: List<UserAppsConfig>
                if (name.contains(".")) { //包名过滤
                    list = userAppsConfigModel.data!!.stream().filter { item: UserAppsConfig ->
                        item.appPackageName!!.lowercase(
                            Locale.getDefault()
                        ) == name.lowercase(Locale.getDefault())
                    }.collect(
                        Collectors.toList()
                    )
                } else {
                    list = userAppsConfigModel.data!!.stream().filter { item: UserAppsConfig ->
                        item.appName!!.lowercase(
                            Locale.getDefault()
                        ) == name.lowercase(Locale.getDefault())
                    }.collect(
                        Collectors.toList()
                    )
                }
                if (!list.isEmpty()) {
                    val userAppsConfig: UserAppsConfig = list.stream().findFirst().get()
                    GeeUILogUtils.logi(
                        "----$TAG",
                        "open others app.appName::" + userAppsConfig.appName + "--appPackageName::" + userAppsConfig.appPackageName + "--openContent::" + userAppsConfig.openContent
                    )

                    if (SystemFunctionUtil.isAppInstalled(
                            context,
                            userAppsConfig.appPackageName!!
                        )
                    ) {
                        if (userAppsConfig.appPackageName == "com.letianpai.robot.expression") {
                            RobotModeManager.getInstance(context).switchRobotMode(
                                ViewModeConsts.VM_STATIC_MODE,
                                ViewModeConsts.APP_MODE_EXPRESSION
                            )
                        } else {
                            RobotModeManager.getInstance(context).switchRobotMode(
                                ViewModeConsts.VM_STATIC_MODE,
                                ViewModeConsts.APP_MODE_OTHER
                            )
                        }
                        stopRobot(iLetianpaiService)
                        var moduleName: String? = ""
                        if (userAppsConfig.isShowReport == 1) {
                            moduleName = userAppsConfig.appPackageName
                        }
                        openApp(
                            context,
                            userAppsConfig.appPackageName!!,
                            userAppsConfig.openContent!!, moduleName
                        )
                        return
                    }
                } else {
                    AppCmdResponser.getInstance(context).userAppsConfig
                }
                RobotModeManager.getInstance(context).ttsUninstallAppText()
            } else {
                AppCmdResponser.getInstance(context).userAppsConfig
            }
        }
    }


    fun openTakePhoto(context: Context): Boolean {
        if (SystemFunctionUtil.isAppInstalled(
                context,
                PackageConsts.TAKEPHOTO_PACKAGE_NAME
            )
        ) {
            openRobotMode(context, COMMAND_VALUE_CHANGE_MODE_TAKE_PHOTO, "h0182")
            controlSteeringEngine(context, true, true)
            val intent: Intent = Intent()
            intent.setComponent(
                ComponentName(
                    PackageConsts.TAKEPHOTO_PACKAGE_NAME,
                    PackageConsts.TAKEPHOTO_PACKAGE_CLASS_NAME
                )
            )
            context.startService(intent)
            return true
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText()
            return false
        }
    }

    fun resetOtherAppStatus(context: Context) {
        changeToStand(context)
    }

    @JvmOverloads
    fun openApp(
        context: Context,
        packageName: String,
        activityName: String,
        moduleName: String?,
        isShowBattery: Boolean = true
    ): Boolean {
        if (SystemFunctionUtil.isAppInstalled(context, packageName)) {
            changeToStand(context)
            val intent: Intent = Intent()
            intent.setComponent(ComponentName(packageName, activityName))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            //If the current app is not equal to the last one, or if it's a Mija, open it.
            if (currentPackageName != packageName || activityName.contains("MiIoTActivity")) {
                try {
                    context.startActivity(intent)
                    // Determine if there is an app that needs to be killed
                    killProcessOfPackageName(context, packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                //Get whether the foreground application package name is packageName, if not, open the
                GeeUILogUtils.logi(
                    "letianpai_test",
                    "getTopPackageName: " + getTopPackageName(context) + "---packageName::" + packageName
                )
                if (getTopPackageName(context) != packageName) {
                    try {
                        context.startActivity(intent)
                        //Determine if there is an app that needs to be killed
                        killProcessOfPackageName(context, packageName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            // Record the current module
            currentPackageName = packageName

            RobotCommandWordsCallback.instance.showBattery(isShowBattery)
            if (!TextUtils.isEmpty(moduleName)) {
                //Reply to a letter
                updateModeStatusOnServer(context, moduleName!!)
                //Record the current open APP, the purpose is to report the order to maintain the
                val modeList: MutableList<String?> =
                    RobotModeManager.getInstance(context).modeList
                if (modeList.contains(moduleName)) {
                    modeList.remove(moduleName)
                }
                modeList.add(moduleName)
            }
            ModeChangeCallback.instance
                .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
            // ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_IDENT_PROGRESS, 1);
            return true
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText()
            return false
        }
    }

    //Kill the app.
    fun killProcessOfPackageName(context: Context, packageName: String) {
        if (packageNames.contains(packageName)) {
            packageNames.remove(packageName)
        }
        //App Centre does not add
        if (packageName != "com.letianpai.robot.desktop" && packageName != "com.letianpai.robot.downloader" && packageName != "com.letianpai.robot.appstore") {
            packageNames.add(packageName)
        }

        //If the second app is not in our own app list, kill the
        for (i in 0 until packageNames.size - 1) {
            val packageN: String = packageNames.get(i)
            GeeUILogUtils.logi("-----", "--for-- packageName::$packageN")
            if (!L81AppListManager.getInstance(context).isInThePackageList(packageN)) {
                killProcessOfPackageName2(context, packageN)
            }
        }

        GeeUILogUtils.logi("-----", "packageName::$packageName")
        GeeUILogUtils.logi("-----", "packageNames.size()::" + packageNames.size)
        if (packageNames.size >= 3) {
            val needKillPackageName: String = packageNames.get(0)
            killProcessOfPackageName2(context, needKillPackageName)
        }
        //test Iterate through the array to see how many
        // Log.d("---++++++++---", packageNames.size()+"");
        //
        // for (int i = 0; i < packageNames.size();i++){
        //     Log.d("---++++++++---", packageNames.get(i));
        // }
    }

    private fun killProcessOfPackageName2(context: Context, currentPackageName: String) {
        val mActivityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var method: Method? = null
        try {
            method = Class.forName("android.app.ActivityManager").getMethod(
                "forceStopPackage",
                String::class.java
            )
            method.invoke(mActivityManager, currentPackageName)
            //Remove the 0th
            packageNames.remove(currentPackageName)
            GeeUILogUtils.logi("---++++++++---", "kill process：：$currentPackageName")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateModeStatusOnServer(context: Context?, modeName: String) {
        if (modeName.isEmpty()) return
        GeeUiNetManager.moduleChange(context, modeName, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body == null) {
                    return
                }
                val resData: String = response.body!!.string()
                var jsonObject: JSONObject? = null
                try {
                    jsonObject = JSONObject(resData)
                    val code: Int = jsonObject.getInt("code")
                    if (code == 0) {
                        val time: Long = System.currentTimeMillis()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
    }


    fun resetRobot(context: Context) {
        GeeUILogUtils.logi("--$TAG", "---resetRobot---- ")
        GeeUiNetManager.robotReset(context, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                GeeUILogUtils.logi("--$TAG", "----resetRobot0000_onFailure----")
                e.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body == null) {
                    return
                }
                val resData: String = response.body!!.string()
                GeeUILogUtils.logi("--$TAG", "resetRobot0000_resData : $resData")
                var jsonObject: JSONObject? = null
                try {
                    jsonObject = JSONObject(resData)
                    val code: Int = jsonObject.getInt("code")
                    if (code == 0) {
                        //Servo unloading
                        changeToStand(context)
                        restoreRobot(context)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        })
    }

    /**
     * "
     * Activate robot mode
     *
     * @param context
     * @param mode    activation mode
     * @param face    Face Tags
     */
    fun openRobotFace(context: Context, mode: String, face: String?) {
        GeeUILogUtils.logi("---$TAG", "----openRobotFace----mode::$mode----face::$mode")
        controlSteeringEngine(context, true, true)
        val intent: Intent = Intent()
        intent.setComponent(ComponentName("com.geeui.face", "com.geeui.face.MainActivity"))
        intent.putExtra("face", face)
        intent.putExtra("mode", mode)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //        intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP );
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
        RobotCommandWordsCallback.instance.showBattery(false)
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_KILL_SPEECH_SERVICE, 1)
    }

    fun startGeeUIOtaService(context: Context) {
        val intent = Intent()
        val cn = ComponentName(
            "com.letianpai.otaservice",
            "com.letianpai.otaservice.ota.GeeUpdateService"
        )
        intent.setComponent(cn)
        context.startService(intent)
    }

    /**
     * Activate robot mode
     *
     * @param context
     * @param mode    activation mode
     * @param face    face Tags
     */
    fun openRobotMode(context: Context, mode: String, face: String?) {
        if (!isRobotOnTheTop(context)) {
            openRobotFace(context, mode, face)
        } else {
            if (mode == RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT) {
                ModeChangeCallback.instance
                    .setModeChange(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1)
            } else if (mode == RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_MONITOR) {
                ModeChangeCallback.instance
                    .setModeChange(ViewModeConsts.VM_TRTC_MONITOR, 1)
            } else if (mode == RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_TRANSFORM) {
                ModeChangeCallback.instance
                    .setModeChange(ViewModeConsts.VM_TRTC_TRANSFORM, 1)
            } else if (mode == RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO) {
                ModeChangeCallback.instance
                    .setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1)
            }
        }
    }

    const val START_FROM_START_APP: String = "startApp"
    const val START_FROM_SLEEP: String = "sleep"
    const val START_FROM: String = "from"
    const val START_FROM_DEEP_SLEEP: String = "deep_sleep"
    const val START_FROM_NO_NETWORK: String = "no_network"


    /**
     * Start the countdown
     *
     * @param context
     */
    fun openTime(context: Context) {
        openTimeView(context, START_FROM_START_APP)
    }


    /**
     * @param context
     */
    fun openMessage(context: Context): Boolean {
        return openApp(
            context,
            PackageConsts.PACKAGE_NAME_MESSAGE,
            PackageConsts.ACTIVITY_NAME_MESSAGE,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_MESSAGE
        )
    }


    /**
     * Start the countdown
     *
     * @param context
     */
    fun openTimeViewForSleep(context: Context) {
        openTimeView(context, START_FROM_SLEEP)
    }

    /**
     * Start the countdown
     *
     * @param context
     */
    fun openTimeViewForDeepSleep(context: Context) {
        openTimeView(context, START_FROM_DEEP_SLEEP)
    }

    /**
     * Start the countdown
     *
     * @param context
     */
    fun openTimeView(context: Context, from: String) {
        GeeUILogUtils.logi(TAG, "openTimeView---from::$from")
        //Reset this value, otherwise there will be problems switching other apps
        currentPackageName = ""
        changeToStand(context)
        var packageN: String = ""
        val intent: Intent = Intent()
        if (from == START_FROM_START_APP) {
            packageN = "com.letianpai.robot.time"
            RobotCommandWordsCallback.instance.showBattery(true)
            intent.setComponent(
                ComponentName(
                    packageN,
                    "com.letianpai.robot.time.ui.activity.MainActivity"
                )
            )
        } else {
            packageN = "com.renhejia.robot.launcher"
            RobotCommandWordsCallback.instance.showBattery(false)
            intent.setComponent(
                ComponentName(
                    packageN,
                    "com.renhejia.robot.launcher.main.activity.LeTianPaiMainActivity"
                )
            )
        }
        intent.putExtra(START_FROM, from)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
        killProcessOfPackageName(context, packageN)

        updateModeStatusOnServer(context, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME)
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
    }

    fun getTopActivity(context: Context) {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks: List<RunningTaskInfo>? = am.getRunningTasks(1)
        if (runningTasks != null && runningTasks.size > 0) {
            val taskInfo: RunningTaskInfo = runningTasks.get(0)
            val componentName: ComponentName? = taskInfo.topActivity
            GeeUILogUtils.logi(
                "letianpai_test",
                "componentName.packageName: " + componentName!!.packageName
            )
            GeeUILogUtils.logi(
                "letianpai_test",
                "componecomponentName.getClassName(): " + componentName.getClassName()
            )

            //            if (componentName.packageName.equals("com.example.myapp") && componentName.getClassName().equals("com.example.myapp.MyActivity")) {
//                // MyActivity is the top activity in the task
//            } else {
//                // MyActivity is not the top activity in the task
//            }
        }
    }

    fun getTopPackageName(context: Context): String {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks: List<RunningTaskInfo>? = am.getRunningTasks(1)
        if (!runningTasks.isNullOrEmpty()) {
            val taskInfo: RunningTaskInfo = runningTasks[0]
            val componentName: ComponentName? = taskInfo.topActivity
            GeeUILogUtils.logi(
                "letianpai_test",
                "getTopPackageName: " + componentName!!.packageName
            )
            return componentName.packageName
        }
        return ""
    }

    /**
     * Getting to the top Activity
     *
     * @param context
     * @return
     */
    fun getTopActivityName(context: Context): String? {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks: List<RunningTaskInfo>? = am.getRunningTasks(1)
        if (!runningTasks.isNullOrEmpty()) {
            val taskInfo: RunningTaskInfo = runningTasks[0]
            val componentName: ComponentName? = taskInfo.topActivity
            if (componentName != null) {
                return componentName.className
            }
        }
        return null
    }

    /**
     * 获取顶部 Activity
     *
     * @param context
     * @return
     */
    fun getTopAppPackageName(context: Context): String? {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks: List<RunningTaskInfo>? = am.getRunningTasks(1)
        if (!runningTasks.isNullOrEmpty()) {
            val taskInfo: RunningTaskInfo = runningTasks.get(0)
            val componentName: ComponentName? = taskInfo.topActivity
            if (componentName != null) {
                return componentName.packageName
            }
        }
        return null
    }

    fun getForegroundPackageName(context: Context): String? {
        val activityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses: List<RunningAppProcessInfo>? =
            activityManager.runningAppProcesses
        if (!runningAppProcesses.isNullOrEmpty()) {
            val currentAppProcess: RunningAppProcessInfo = runningAppProcesses[1]
            return currentAppProcess.processName
        }
        return null
    }

    /**
     * @param context
     * @return
     */
    fun isRobotOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.ROBOT_CLASS_NAME) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isWifiConnectorOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_WIFI_CONNECTOR) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isAlarmOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.ALARM_CLASS_NAME) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isNewAlarmOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.ALARM_CLASS_NAME_NEW) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isFactoryOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.FACTORY_MODE_CLASS_NAME) {
            return true
        } else if (activityName != null && activityName == PackageConsts.FACTORY_CALI_MODE_CLASS_NAME) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isOtaOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_OTA) {
            return true
        } else {
            return false
        }
    }

    fun isSpeechOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.SPEECH_CLASS_NAME) {
            return true
        } else {
            return false
        }
    }

    fun isLexOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.LEX_CLASS_NAME) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isOtherRobotAppOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && (activityName != PackageConsts.ROBOT_CLASS_NAME) && (activityName != PackageConsts.LAUNCHER_CLASS_NAME)) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    fun isTargetAppRunning(context: Context, packageName: String): Boolean {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val list: List<RunningTaskInfo> = am.getRunningTasks(100)
        for (info: RunningTaskInfo in list) {
            if (info.topActivity!!.packageName == packageName || info.baseActivity!!.packageName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * @param context
     * @return
     */
    fun isTargetAppTopRunning(context: Context, packageName: String): Boolean {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val list: List<RunningTaskInfo> = am.getRunningTasks(100)
        for (info: RunningTaskInfo in list) {
            if (info.topActivity!!.packageName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * @param context
     * @return
     */
    fun isAlarmRunning(context: Context): Boolean {
//        return isTargetAppRunning(context, PackageConsts.ALARM_PACKAGE_NAME);
        return isTargetAppTopRunning(context, PackageConsts.ALARM_PACKAGE_NAME)
    }

    /**
     * @param context
     * @return
     */
    fun isVideoCallOnTheTop(context: Context): Boolean {
        return isTargetAppTopRunning(context, PackageConsts.PACKAGE_NAME_VIDEO_CALL)
    }

    /**
     * @param context
     * @return
     */
    fun isVideoCallRunning(context: Context): Boolean {
        return isTargetAppRunning(context, PackageConsts.PACKAGE_NAME_VIDEO_CALL)
    }

    /**
     * @param context
     * @return
     */
    fun isVideoCallServiceRunning(context: Context): Boolean {
        return ServiceUtils.isServiceRunning(context, PackageConsts.PACKAGE_NAME_VIDEO_CALL_SERVICE)
    }

    /**
     * @param context
     * @return
     */
    fun isAlarmServiceRunning(context: Context): Boolean {
        return ServiceUtils.isServiceRunning(context, PackageConsts.SERVICE_NAME_ALARM)
    }

    /**
     * @param context
     * @return
     */
    fun isRemindServiceRunning(context: Context): Boolean {
        return ServiceUtils.isServiceRunning(context, PackageConsts.SERVICE_NAME_REMINDER)
    }

    /**
     * @param context
     * @return
     */
    fun isAppStoreServiceRunning(context: Context): Boolean {
        return ServiceUtils.isServiceRunning(context, PackageConsts.SERVICE_NAME_APP_STORE)
    }


    /**
     * @param context
     * @return
     */
    fun isAutoAppOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && (activityName == PackageConsts.CLASS_NAME_AUTO_CHARGING)) {
            return true
        } else {
            return false
        }
    }

    /**
     * @param context
     * @return
     */
    // public static boolean isDesktopAppOnTheTop(Context context) {
    //     String activityName = getTopActivityName(context);
    //     if (activityName != null && (activityName.equals(PackageConsts.ACTIVITY_NAME_DESKTOP))) {
    //         return true;
    //     } else {
    //         return false;
    //     }
    //
    // }
    /**
     * @param context
     * @return
     */
    fun isLauncherOrRobotOnTheTop(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if (activityName != null && activityName == PackageConsts.ROBOT_CLASS_NAME) {
            return true
        } else if (activityName != null && activityName == PackageConsts.LAUNCHER_CLASS_NAME) {
            return true
        } else {
            return false
        }
    }


    /**
     * @param context
     * @return
     */
    fun isRobotRunning(context: Context): Boolean {
        return isAppRunning(context, PackageConsts.ROBOT_CLASS_NAME)
    }

    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks: List<RunningTaskInfo>? = activityManager.getRunningTasks(Int.MAX_VALUE)
        if (runningTasks != null) {
            for (taskInfo: RunningTaskInfo in runningTasks) {
                val componentName: ComponentName? = taskInfo.topActivity

                if (componentName!!.packageName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    fun isActivityRunning(context: Context, activityName: String): Boolean {
        val activityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks: List<RunningTaskInfo>? = activityManager.getRunningTasks(Int.MAX_VALUE)

        if (runningTasks != null) {
            for (taskInfo: RunningTaskInfo in runningTasks) {
                val componentName: ComponentName? = taskInfo.topActivity
                val componentName1: ComponentName? = taskInfo.baseActivity
                if (componentName != null && !TextUtils.isEmpty(componentName.getClassName()) && (componentName.getClassName() == activityName)) {
                    return true
                }
                if (componentName1 != null && !TextUtils.isEmpty(componentName1.getClassName()) && (componentName1.getClassName() == activityName)) {
                    return true
                }
            }
        }
        return false
    }


    fun isSpeechRunning(context: Context): Boolean {
        return isActivityRunning(context, PackageConsts.SPEECH_CLASS_NAME)
    }

    fun isLexRunning(context: Context): Boolean {
        return isActivityRunning(context, PackageConsts.LEX_CLASS_NAME)
    }

    /**
     * @param context
     * @return
     */
    fun isRobotAppRunning(context: Context): Boolean {
        return isActivityRunning(context, PackageConsts.ROBOT_CLASS_NAME)
    }

    /**
     * 开启ChatGPT
     *
     * @param context
     */
    fun openChatGpt(context: Context) {
        val intent: Intent = Intent()
        intent.setComponent(ComponentName("com.rhj.chatgpt", "com.rhj.chatgpt.MainActivity"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    fun openCountdown(context: Context, time: String?) {
        GeeUILogUtils.logi(
            "letianpai_control",
            "controlSteeringEngine_========================== 12 =========================="
        )
        controlSteeringEngine(context, false, false)
        setTimer(context, LTPConfigConsts.VALUE_ALARM, time)
        RobotCommandWordsCallback.instance.showBattery(true)
    }

    fun startMIotService(iLetianpaiService: ILetianpaiService, context: Context) {
        RobotModeManager.getInstance(context).switchRobotMode(
            ViewModeConsts.VM_STATIC_MODE,
            ViewModeConsts.APP_MODE_MIJIA
        )
        stopRobot(iLetianpaiService)
    }

    /**
     * 切换舵机开关
     *
     * @param context
     */
    fun controlSteeringEngine(context: Context, footSwitch: Boolean, sensorSwitch: Boolean) {
        GeeUILogUtils.logi(TAG, "舵机卸力---" + footSwitch + " ====" + sensorSwitch)
        if (isAutoAppOnTheTop(context)) {
            return
        }
        ControlSteeringEngineCallback.instance
            .setControlSteeringEngine(footSwitch, sensorSwitch)
    }

    /**
     * 回正并关闭舵机
     *
     * @param context
     */
    fun changeToStand(context: Context) {
        GestureCallback.instance
            .setGestures(GestureCenter.resetStandGesture(), RGestureConsts.GESTURE_COMMAND_DELAY)
        val timer: Timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                controlSteeringEngine(context, false, false)
            }
        }, 400)
    }

    /**
     * 回正并关闭舵机
     *
     * @param context
     */
    fun changeToStand(context: Context, hasPower: Boolean) {
        GestureCallback.instance
            .setGestures(GestureCenter.resetStandGesture(), RGestureConsts.GESTURE_COMMAND_DELAY)
        val timer: Timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (hasPower) {
                    controlSteeringEngine(context, true, true)
                } else {
                    controlSteeringEngine(context, false, false)
                }
            }
        }, 400)
    }

    /**
     * @param context
     */
    fun openDevelopSettings(context: Context) {
        val intent: Intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 开发者选项是否开启
     *
     * @return true 开启
     */
    fun isOpenDevelopmentSetting(activity: Context): Boolean {
        val enableAdb: Boolean = Settings.Secure.getInt(
            activity.getContentResolver(),
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) != 0
        return enableAdb
    }

    /**
     * usb调试是否开启
     *
     * @return true 开启
     */
    fun isUSBDebugSetting(activity: Context): Boolean {
        val enableAdb: Boolean = Settings.Secure.getInt(
            activity.getContentResolver(),
            Settings.Global.ADB_ENABLED,
            0
        ) != 0
        return enableAdb
    }


    /**
     * 开启闹钟
     *
     * @param context
     */
    fun openClock(context: Context, time: String?) {
//        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(3, 0).toString());
//        //TODO 给MCU发消息，打开悬崖，悬空上爆棚
//        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(5, 0).toString());
        controlSteeringEngine(context, false, false)
        setTimer(context, LTPConfigConsts.VALUE_CLOCK, time)
        RobotCommandWordsCallback.instance.showBattery(true)
    }

    /**
     * 开启闹钟
     *
     * @param context
     */
    fun openNotices(context: Context, time: String?, title: String?) {
        controlSteeringEngine(context, false, false)
        setTimer(context, LTPConfigConsts.VALUE_NOTICE, time)
        RobotCommandWordsCallback.instance.showBattery(true)
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    fun setTimer(context: Context, type: String?, time: String?): Boolean {
        if (SystemFunctionUtil.isAppInstalled(context, "com.letianpai.robot.alarm")) {
            SystemFunctionUtil.wakeUp(context)
            RobotCommandWordsCallback.instance.showBattery(true)
            val intent: Intent = Intent()
            intent.setComponent(
                ComponentName(
                    "com.letianpai.robot.alarm",
                    "com.letianpai.robot.alarm.MainActivity"
                )
            )
            intent.putExtra(LTPConfigConsts.COUNT_DOWN_KEY, time)
            intent.putExtra(LTPConfigConsts.CLOCK_ALARM_TYPE, type)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
            return true
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText()
            return false
        }
    }


    //
    //    public static void restoreRobot(Context context) {
    //        Intent intent = new Intent("android.intent.action.FACTORY_RESET");
    //        intent.putExtra("android.intent.extra.REASON", "Factory reset from app");
    //        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true);
    //        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
    //        intent.setPackage("android");
    //        context.startActivity(intent);
    //    }
    fun restoreRobot(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                RecoverySystem.rebootWipeUserData(context.applicationContext)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                val packageName: String = context.applicationContext.packageName
                Runtime.getRuntime().exec(arrayOf("su", "-c", "pm clear $packageName")).waitFor()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    val cpuThermal: Float
        get() {
            val result: MutableList<String> =
                ArrayList()
            var br: BufferedReader? = null
            var temp: Float = 0f

            try {
                val dir: File = File("/sys/class/thermal/")

                val files: Array<File>? = dir.listFiles { file ->
                    Pattern.matches("thermal_zone[0-9]+", file.name)
                }
                val SIZE: Int = files!!.size
                val line: String?
                for (i in 0 until SIZE) {
                    br =
                        BufferedReader(FileReader("/sys/class/thermal/thermal_zone$i/temp"))
                    line = br.readLine()
                    if (line != null) {
                        val temperature: Long = line.toLong()
                        if (temperature < 0) {
                            temp = -1f
                            return temp
                        } else {
                            temp = (temperature / 1000.0).toFloat()
                            return temp
                        }
                    }
                    break
                }
                return temp
            } catch (e: FileNotFoundException) {
                result.add(e.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (br != null) {
                    try {
                        br.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return temp
        }

    fun takePhoto(context: Context) {
        RobotModeManager.getInstance(context)
            .switchRobotMode(ViewModeConsts.VM_TAKE_PHOTO, 1)
    }

    fun getCompareTime(time: String): Int {
//        LogUtils.logd("LetianpaiFunctionUtil", "takePhoto: ");

        val times: Array<String> =
            time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (times == null || times.size < 2) {
            return 0
        } else {
            return (times.get(0).toInt()) * 100 + times.get(1).toInt()
        }
    }

    fun getCompareTime(hour: Int, minute: Int): Int {
        return hour * 100 + minute
    }

    fun isTimeInSleepRange(context: Context?, hour: Int, minute: Int): Boolean {
        val startTime: Int = RobotConfigManager.getInstance(context)!!.sleepStartTime
        val endTime: Int = RobotConfigManager.getInstance(context)!!.sleepEndTime
        val currentTime: Int = getCompareTime(hour, minute)
        if (startTime == 0 && endTime == 0) {
            return false
        }

        if (startTime > endTime) {
            if (((currentTime >= startTime) && (currentTime <= 2400)) || ((currentTime > 0) && (currentTime <= endTime))) {
                return true
            }
        } else if (startTime < endTime) {
            if ((currentTime >= startTime) && (currentTime <= endTime)) {
                return true
            }
        }
        return false
    }

    /**
     * @param context
     * @param hour
     * @param minute
     */
    fun updateRobotSleepStatus(context: Context, hour: Int, minute: Int) {
//        if (ChargingUpdateCallback.getInstance().isCharging()){
//            return;
//        }

        val isInRange: Boolean = isTimeInSleepRange(context, hour, minute)
        val isSleepModeOn: Boolean =
            RobotConfigManager.getInstance(context)!!.closeScreenModeSwitch
        val currentMode: Int = RobotModeManager.getInstance(context).robotMode
        GeeUILogUtils.logi("letianpai_sleep_test", "isInRange: " + isInRange)
        GeeUILogUtils.logi("letianpai_sleep_test", "isSleepModeOn: " + isSleepModeOn)
        GeeUILogUtils.logi("letianpai_sleep_test", "currentMode: " + currentMode)
        //        if (isInRange && isSleepModeOn ){
//            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE,1);
//        }else if ((!isInRange || !isSleepModeOn) && (currentMode == ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE) ){
//            SystemFunctionUtil.wakeUp(context);
//            RobotModeManager.getInstance(context).switchToPreviousPlayMode();
//        }
        if (isInRange && isSleepModeOn) {
            GeeUILogUtils.logi(
                "letianpai_sleep",
                "currentMode ===== updateRobotSleepStatus ========1 : $currentMode"
            )
            RobotModeManager.getInstance(context)
                .switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE, 1)
        } else if ((!isInRange || !isSleepModeOn) && (currentMode == ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE)) {
            GeeUILogUtils.logi(
                "letianpai_sleep",
                "currentMode ===== updateRobotSleepStatus ========2 : $currentMode"
            )
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "open view ========================== 3 =========================="
            )

            SystemFunctionUtil.wakeUp(context)
            RobotModeManager.getInstance(context)
                .switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE, 0)
        }
    }

    fun startBleService(context: Context) {
        val intent: Intent = Intent()
        val cn: ComponentName = ComponentName("com.rhj.speech", "com.rhj.audio.service.BleService")
        intent.setComponent(cn)
        context.startService(intent)
    }

    fun stopBleService(context: Context) {
        val intent: Intent = Intent()
        val cn: ComponentName = ComponentName("com.rhj.speech", "com.rhj.audio.service.BleService")
        intent.setComponent(cn)
        context.stopService(intent)
    }

    fun responseCharging(context: Context) {
        //If the device is unbound
        if (RobotModeManager.getInstance(context)
                .robotMode == ViewModeConsts.VM_UNBIND_DEVICE
        ) {
            RobotCommandWordsCallback.instance.showBattery(true)
            return
        }

        // long currentTime  = System.currentTimeMillis();
        // long inter = currentTime - LetianpaiFunctionUtil.getExitLongTrtcTime();
        // Log.i("LetianpaiFunctionUtil::responseCharging", " --- inter: "+ inter +
        //         "---LetianpaiFunctionUtil.getExitLongTrtcTime():"+ LetianpaiFunctionUtil.getExitLongTrtcTime() +
        //         "---LetianpaiFunctionUtil.isVideoCallServiceRunning(context):"+ LetianpaiFunctionUtil.isVideoCallServiceRunning(context)
        // );
        //Remote real-time monitoring filters in here.
        if ((RobotModeManager.getInstance(context).robotTrtcStatus != -1)) {
            return
        }

        // else if (LetianpaiFunctionUtil.isVideoCallServiceRunning(context) && (inter>= 0  && inter<= 50 )){
        //     Log.i("LetianpaiFunctionUtil::responseCharging", " =================================== letianpai_sleep_test ==== 3.3 ===================================  ");
        //     LetianpaiFunctionUtil.setExitLongTrtcTime(-1);
        // }
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_KILL_ROBOT_PROGRESS, 1)
        GeeUILogUtils.logi(
            "LetianpaiFunctionUtil::responseCharging",
            "=========== 0001.1 ========== "
        )
        if (RobotModeManager.getInstance(context)
                .isRobotDeepSleepMode && TemperatureUpdateCallback.instance
                .isInHighTemperature
        ) {
            GeeUILogUtils.logi(
                "LetianpaiFunctionUtil::responseCharging",
                "=========== 0002 ========== "
            )
            return
        }

        GeeUILogUtils.logi(
            "LetianpaiFunctionUtil::responseCharging",
            "=========== 0002.1 ========== "
        )
        if (RobotStatusResponser.getInstance(context).isNoNeedResponseMode) {
            GeeUILogUtils.logi(
                "LetianpaiFunctionUtil::responseCharging",
                "=========== 0003 ========== "
            )
            return
        }
        GeeUILogUtils.logi(
            "LetianpaiFunctionUtil::responseCharging",
            "=========== 0003.1 ========== "
        )
        if (RobotModeManager.getInstance(context).isSleepMode
            && RobotModeManager.getInstance(context).isPreviousModeIsRobotMode
            && RobotConfigManager.getInstance(context)!!
                .sleepTimeStatusModeSwitch == ViewModeConsts.ROBOT_STATUS_SLEEP_ZZZ
        ) {
            GeeUILogUtils.logi(
                "LetianpaiFunctionUtil::responseCharging",
                "=========== 0005.1 ========== "
            )
            openTimeViewForSleep(context)
            return
        }
        if (RobotModeManager.getInstance(context).isSleepMode) {
            GeeUILogUtils.logi(
                "LetianpaiFunctionUtil::responseCharging",
                "=========== 0005 ========== "
            )
            return
        }

        GeeUILogUtils.logi(
            "LetianpaiFunctionUtil::responseCharging",
            "=========== 0006.2 ==========RobotModeManager.getInstance(context).getRobotMode():  " + RobotModeManager.getInstance(
                context
            ).robotMode
        )
        GeeUILogUtils.logi(
            "LetianpaiFunctionUtil::responseCharging",
            "=========== 0006.3 ==========RobotModeManager.getInstance(context).getRobotModeStatus():  " + RobotModeManager.getInstance(
                context
            ).robotModeStatus
        )
        // if ((!RobotModeManager.getInstance(context).isRobotMode())
        //         && (!RobotModeManager.getInstance(context).isRobotWakeupMode())) {
        //     Log.e("letianpai_sleep_test", "=========== 0007 ========== ");
        //     return;
        // }
        val robotMode: Int = RobotModeManager.getInstance(context).robotMode
        val robotModeStatus: Int =
            RobotModeManager.getInstance(context).robotModeStatus
        if (robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT && robotModeStatus == 1) {
            return
        } else if (robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE && robotModeStatus == 0) {
            //Exited wake-up call.
            if (RobotModeManager.getInstance(context).isPreviousModeIsAppMode) {
                RobotCommandWordsCallback.instance.showBattery(true)
                RobotModeManager.getInstance(context).switchToPreviousAppMode()
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0007.11 ========== ")
                return
            }
        } else if (robotMode == ViewModeConsts.VM_STATIC_MODE) {
            RobotCommandWordsCallback.instance.showBattery(true)
            RobotModeManager.getInstance(context).switchToPreviousAppMode()
            return
        } else {
            if (RobotModeManager.getInstance(context).isPreviousModeIsAppMode) {
                RobotCommandWordsCallback.instance.showBattery(true)
                RobotModeManager.getInstance(context).switchToPreviousAppMode()
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0007.12========== ")
                return
            }
        }

        //Turn off the screen in sleep mode
        if (RobotConfigManager.getInstance(context)!!
                .sleepTimeStatusModeSwitch == ViewModeConsts.ROBOT_STATUS_CLOSE_SCREEN
        ) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0010 ========== ")
            RobotModeManager.getInstance(context).setRobotModeBeforeChargingOn(true)
            RobotModeManager.getInstance(context)
                .switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 1)
            //Snoring in sleep mode
        } else if (RobotConfigManager.getInstance(context)!!
                .sleepTimeStatusModeSwitch == ViewModeConsts.ROBOT_STATUS_SLEEP_ZZZ
        ) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0011 ========== ")
            RobotModeManager.getInstance(context)
                .switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1)
        } else {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0012 ========== ")
            if (RobotModeManager.getInstance(context)
                    .robotMode != ViewModeConsts.VM_STATIC_MODE
            ) {
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0013 ========== ")
                RobotModeManager.getInstance(context).switchToPreviousAppMode()
            }
        }
    }

    fun responseDisCharging(context: Context) {
        GeeUILogUtils.logi(
            "letianpai_sleep_test",
            "=========== responseDisCharging 0001 ========== "
        )
        if (RobotModeManager.getInstance(context)
                .isRobotDeepSleepMode && TemperatureUpdateCallback.instance
                .isInHighTemperature
        ) {
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "=========== responseDisCharging 0002 ========== "
            )
            return
        }
        GeeUILogUtils.logi(
            "letianpai_sleep_test",
            "=========== responseDisCharging 0002.1 ========== "
        )
        if (RobotStatusResponser.getInstance(context).isNoNeedResponseMode) {
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "=========== responseDisCharging 0003 ========== "
            )
            return
        }
        GeeUILogUtils.logi(
            "letianpai_sleep_test",
            "=========== responseDisCharging 0003.1 ========== "
        )
        if (RobotModeManager.getInstance(context).isAppMode) {
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "=========== responseDisCharging 0004 ========== "
            )
            return
        }
        GeeUILogUtils.logi(
            "letianpai_sleep_test",
            "=========== responseDisCharging 0004.1 ========== "
        )

        //        if ((!RobotModeManager.getInstance(context).isRobotMode()) && (!RobotModeManager.getInstance(context).isRobotWakeupMode())){
//            Log.e("letianpai_sleep_test", "=========== responseDisCharging 0004.1 ========== ");
//            return;
//        }
        GeeUILogUtils.logi(
            "letianpai_sleep_test",
            "=========== responseDisCharging 0008 ========== "
        )

        //        if (RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_FUNCTION_MODE) {
//            Log.e("letianpai_test000", "=========== 0009 ========== ");
//            RobotModeManager.getInstance(context).setRobotModeBeforeChargingOn(true);
//            RobotModeManager.getInstance(context).switchToPreviousAppMode();
//        } else
        if (RobotConfigManager.getInstance(context)!!
                .sleepTimeStatusModeSwitch == ViewModeConsts.ROBOT_STATUS_CLOSE_SCREEN
        ) {
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "=========== responseDisCharging 0010 ========== "
            )
            RobotModeManager.getInstance(context).setRobotModeBeforeChargingOn(true)
            RobotModeManager.getInstance(context)
                .switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 1)
        } else if (RobotConfigManager.getInstance(context)!!
                .sleepTimeStatusModeSwitch == ViewModeConsts.ROBOT_STATUS_SLEEP_ZZZ
        ) {
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "=========== responseDisCharging 0011 ========== "
            )
            RobotModeManager.getInstance(context)
                .switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1)
        } else {
            GeeUILogUtils.logi(
                "letianpai_sleep_test",
                "=========== responseDisCharging 0012 ========== "
            )
            if (RobotModeManager.getInstance(context)
                    .robotMode != ViewModeConsts.VM_STATIC_MODE
            ) {
                GeeUILogUtils.logi(
                    "letianpai_sleep_test",
                    "=========== responseDisCharging 0013 ========== "
                )
                RobotModeManager.getInstance(context).switchToPreviousAppMode()
            }
        }
    }

    fun hideStatusBar() {
        RobotCommandWordsCallback.instance.showBattery(false)
    }

    fun closeAppByPackageName(packageName: String?, context: Context) {
        // ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        // activityManager.killBackgroundProcesses(packageName);

        val mActivityManager: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            val method: Method = Class.forName("android.app.ActivityManager").getMethod(
                "forceStopPackage",
                String::class.java
            )
            method.invoke(mActivityManager, packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAutoAppCanBeLaunched(context: Context): Boolean {
        val activityName: String? = getTopActivityName(context)
        if ((activityName != null && activityName == PackageConsts.ACTIVITY_NAME_NEWS)
            || (activityName != null && activityName == PackageConsts.LAUNCHER_CLASS_NAME)
            || (activityName != null && activityName == PackageConsts.PACKAGE_NAME_WORDS_ACTIVITY)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_COMMEMORATION)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_PACKAGE_NAME)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_COUNT_DOWN)
            || (activityName != null && activityName == PackageConsts.PACKAGE_NAME_STOCK_ACTIVITY)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_FANS)
            || (activityName != null && activityName == PackageConsts.CLASS_NAME_CUSTOM)
            || (activityName != null && activityName == PackageConsts.PACKAGE_NAME_LAMP_ACTIVITY)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_REMINDER)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_SPECTRUM)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_POMO)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_MEDITATION)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_MESSAGE)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_TIME)
            || (activityName != null && activityName == PackageConsts.ROBOT_CLASS_NAME)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_EXPRESSION)
            || (activityName != null && activityName == PackageConsts.ACTIVITY_NAME_APHORISMS)
        ) {
            return true
        } else {
            return false
        }
    }
}
