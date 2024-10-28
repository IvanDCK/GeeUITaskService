package com.letianpai.robot.response.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.letianpai.robot.components.consts.AppStoreUpdateConsts
import com.letianpai.robot.components.network.nets.AppStoreCmdConsts
import com.letianpai.robot.components.network.nets.GeeUiNetManager
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.callback.RobotCommandWordsCallback
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.ViewModeConsts.Companion.VM_TAKE_PHOTO
import com.letianpai.robot.control.storage.SPUtils
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.response.RobotFuncResponseManager
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.parser.config.AppsShowListModel
import com.renhejia.robot.commandlib.parser.config.UserAppsConfig
import com.renhejia.robot.commandlib.parser.config.UserAppsConfigModel
import com.renhejia.robot.letianpaiservice.ILetianpaiService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.stream.Collectors

/**
 * @author liujunbin
 */
class AppCmdResponser private constructor(private val mContext: Context) {
    private var mTimer: Timer? = null
    private var isTimerCancel = false

    var isAutoSwitchApp: Boolean = true
        //Toggle Robot App
        set(autoSwitchApp) {
            field = autoSwitchApp
            if (autoSwitchApp) {
                //request network
                requestAppsShowConfig()
                if (mTimer != null) {
                    mTimer!!.cancel()
                }
                mTimer = Timer()
                val task: TimerTask = object : TimerTask() {
                    override fun run() {
                        GeeUILogUtils.logi(
                            TAG,
                            "run------appsShowListModel::$appsShowListModel--isTimerCancel::$isTimerCancel"
                        )
                        if (isTimerCancel) return
                        if (appsShowListModel != null) {
                            val appsShowList = appsShowListModel!!.data
                            if (appsShowList != null) {
                                val appShowList =
                                    appsShowList.appShowList
                                if (appShowList != null && appShowList.size > 0) {
                                    val appShow = appShowList[0]
                                    GeeUILogUtils.logi(
                                        TAG,
                                        "run------appShow::$appShow"
                                    )

                                    if (appShow != null) {
                                        val openPath = appShow.openPath
                                        val packageName = appShow.packageName
                                        val appTag = appShow.appTag
                                        GeeUILogUtils.logi(
                                            TAG,
                                            "openPath:$openPath----packageName:$packageName---appTag:$appTag"
                                        )
                                        if (SystemFunctionUtil.isAppInstalled(
                                                mContext,
                                                packageName.toString()
                                            )
                                        ) {
                                            if (packageName == "com.geeui.face") {
                                                RobotFuncResponseManager.Companion.getInstance(
                                                    mContext
                                                ).openRobotMode("")
                                            } else {
                                                //Open the application
                                                RobotModeManager.getInstance(mContext)
                                                    .switchRobotMode(
                                                        ViewModeConsts.VM_STATIC_MODE,
                                                        ViewModeConsts.APP_MODE_OTHER
                                                    )
                                                LetianpaiFunctionUtil.openApp(
                                                    mContext,
                                                    packageName.toString(),
                                                    openPath.toString(),
                                                    appTag
                                                )
                                            }
                                        }
                                        appShowList.remove(appShow)
                                    }
                                } else {
                                    requestAppsShowConfig()
                                }
                            }
                        }
                    }
                }
                isTimerCancel = false
                mTimer!!.schedule(task, 0, (3 * 60 * 1000).toLong()) // 3分钟 = 3 * 60 * 1000 毫秒
            } else {
                if (mTimer != null) {
                    isTimerCancel = true
                    mTimer!!.cancel()
                    mTimer = null
                }
            }
        }

    private var installedApps: MutableList<Map<String, String>>? = null

    @JvmField
    var userAppsConfigModel: UserAppsConfigModel? = null

    //自动切换的APP
    var appsShowListModel: AppsShowListModel? = null

    private val appsConfigCallback: Callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            if (response.body != null) {
                val info = response.body!!.string()
                GeeUILogUtils.logi("------", "info--$info")
                try {
                    userAppsConfigModel = Gson().fromJson(
                        info,
                        UserAppsConfigModel::class.java
                    )
                    //Iterate through the installed apps and then determine if you need to pull up the
                    val iterator = installedApps!!.iterator()
                    while (iterator.hasNext()) {
                        val map = iterator.next()
                        val packageName = map["packageName"]
                        val list = userAppsConfigModel!!.data!!.stream()
                            .filter { item: UserAppsConfig -> item.appPackageName == packageName }
                            .collect(
                                Collectors.toList()
                            )
                        val userAppsConfig = list.stream().findFirst().get()
                        if (userAppsConfig.isRestart == 1 && userAppsConfig.openType == 2) {
                            //Need to judge whether overseas or domestic
                            val pro = SystemUtil.get(SystemUtil.REGION_LANGUAGE, "zh")
                            if (userAppsConfig.appPackageName == "com.geeui.lex" && "en" != pro) {
                                break
                            }
                            if (userAppsConfig.appPackageName == "com.rhj.speech" && "zh" != pro) {
                                break
                            }

                            //Need to restart service
                            val intent = Intent()
                            val cn = ComponentName(
                                userAppsConfig.appPackageName!!,
                                userAppsConfig.openContent!!
                            )
                            intent.setComponent(cn)
                            mContext.startService(intent)
                            //Delete one when you start one.
                            iterator.remove()
                        }
                        GeeUILogUtils.logi(
                            "------",
                            "userAppsConfig:appPackageName:--" + userAppsConfig.appPackageName + "--userAppsConfig:openContent:" + userAppsConfig.openContent
                        )
                    }
                    //重启完成之后，清空
                    installedApps!!.clear()

                    //----
                    // for ( UserAppsConfig config:userAppsConfigModel.getData()) {
                    //     Log.i("------","userAppsConfig:appPackageName:--"+config.appPackageName +"--userAppsConfig:openContent:"+config.openContent);
                    //     Log.i("------","userAppsConfig:appName:--"+config.appName );
                    // }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val appsShowCallback: Callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        @Throws(IOException::class)
        override fun onResponse(call: Call, response: Response) {
            if (response.body != null) {
                val info = response.body!!.string()
                GeeUILogUtils.logi("------", "appsShowCallback info--$info")
                try {
                    appsShowListModel = Gson().fromJson(
                        info,
                        AppsShowListModel::class.java
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    init {
        init()
    }

    private fun init() {
        installedApps = ArrayList()
        val isAutoShow = SPUtils.getInstance(mContext)!!.getDouble("isAutoShow")
        //Waiting for 1, automatic switching
        isAutoSwitchApp = isAutoShow == 1.0
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService?, command: String?, data: String?) {
        GeeUILogUtils.logi(
            TAG,
            "commandDistribute: ============ D ============ command: $command---data:$data"
        )
        if (command == null) {
            return
        }

        when (command) {
            AppStoreCmdConsts.COMMAND_INSTALL_APP_STORE_SUCCESS -> {
                // Request the data of the installed APP, after successful installation, the name of the currently installed package will be sent over.
                //{packageName:'com.letianpai.robot.reminder', displayName:'GeeUIReminder'}
                val jsonObject = Gson().fromJson(
                    data,
                    JsonObject::class.java
                )
                val packageName = jsonObject["packageName"].asString
                val displayName = jsonObject["displayName"].asString
                val tempList = installedApps!!.stream()
                    .filter { item: Map<String, String> -> item["packageName"] == packageName }
                    .collect(
                        Collectors.toList()
                    )
                if (tempList.isEmpty()) {
                    val installedApp: MutableMap<String, String> = HashMap()
                    installedApp["packageName"] = packageName
                    installedApp["displayName"] = displayName
                    installedApps!!.add(installedApp)
                }

                userAppsConfig
            }

            RobotRemoteConsts.COMMAND_TYPE_MOTION -> {}
            RobotRemoteConsts.COMMAND_SET_APP_MODE -> {
                if (data == null) {
                    return
                }
                if (data == RobotRemoteConsts.COMMAND_SHOW_CHARGING) {
                    RobotCommandWordsCallback.instance.showBattery(true)
                } else if (data == RobotRemoteConsts.COMMAND_HIDE_CHARGING) {
                    RobotCommandWordsCallback.instance.showBattery(false)
                }
            }

            AppCmdConsts.COMMAND_TYPE_TAKE_PHOTO -> RobotModeManager.getInstance(mContext)
                .switchRobotMode(VM_TAKE_PHOTO, data!!.toInt())

            RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN -> SystemFunctionUtil.shutdownRobot(mContext)
            RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN_STEERING_ENGINE -> if (!RobotModeManager.getInstance(
                    mContext
                ).isPowerOnChargingMode
            ) {
                LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false)
            }

            RobotRemoteConsts.COMMAND_TYPE_POWER_ON_CHARGING -> responsePowerOnCharging()
            AppStoreUpdateConsts.COMMAND_INSTALL_APP_STORE -> {
                //                updateAppStore(data);
                GeeUILogUtils.logi(
                    "letianpai_install_app_store",
                    "letianpai_install_app_store: " + AppStoreUpdateConsts.COMMAND_INSTALL_APP_STORE + " =============== 1 ============="
                )
                startAppStoreService()
            }

            else -> {}
        }
    }


    private fun startAppStoreService() {
        GeeUILogUtils.logi(
            "letianpai_install_app_store",
            "letianpai_install_app_store: " + AppStoreUpdateConsts.COMMAND_INSTALL_APP_STORE + " =============== 2 ============="
        )
        Thread {
            try {
                Thread.sleep((10 * 1000).toLong())
                val intent = Intent()
                val cn = ComponentName(
                    "com.letianpai.robot.appstore",
                    "com.letianpai.robot.appstore.service.AppStoreService"
                )
                intent.setComponent(cn)
                mContext.startService(intent)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }


    val userAppsConfig: Unit
        //Get user-installed apps
        get() {
            GeeUiNetManager.getUserAppsConfig(
                mContext,
                appsConfigCallback
            )
        }


    //Get the app that needs to be switched automatically
    fun requestAppsShowConfig() {
        GeeUiNetManager.getAppsShowConfig(mContext, appsShowCallback)
    }


    /**
     * Responds to power-on charging action
     */
    private fun responsePowerOnCharging() {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_POWER_ON_CHARGING, 1)
    }

    companion object {
        private const val TAG = "AppCmdResponser"
        private var instance: AppCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): AppCmdResponser {
            synchronized(AppCmdResponser::class.java) {
                if (instance == null) {
                    instance = AppCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
