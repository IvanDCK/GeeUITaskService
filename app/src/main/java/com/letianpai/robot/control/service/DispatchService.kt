package com.letianpai.robot.control.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.letianpai.robot.alarm.receiver.AlarmCallback
import com.letianpai.robot.alarm.receiver.AlarmCallback.AlarmTimeListener
import com.letianpai.robot.components.network.nets.GeeUIStatusUploader
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.components.parser.base.BaseMessageInfo
import com.letianpai.robot.components.parser.recharge.ReChargeInfo
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.broadcast.LauncherBroadcastReceiverManager
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.broadcast.timer.TimerKeeperCallback
import com.letianpai.robot.control.broadcast.timer.TimerKeeperCallback.TimerKeeperUpdateListener
import com.letianpai.robot.control.callback.ControlSteeringEngineCallback
import com.letianpai.robot.control.callback.ControlSteeringEngineCallback.ControlSteeringEngineListener
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.callback.GestureCallback.GestureCompleteListener
import com.letianpai.robot.control.callback.GestureCallback.GestureResponseListener
import com.letianpai.robot.control.callback.RobotCommandWordsCallback
import com.letianpai.robot.control.callback.TemperatureUpdateCallback
import com.letianpai.robot.control.callback.TemperatureUpdateCallback.TemperatureUpdateListener
import com.letianpai.robot.control.consts.AudioServiceConst
import com.letianpai.robot.control.consts.RobotConsts
import com.letianpai.robot.control.manager.CommandResponseManager
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.manager.SleepModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.callback.ModeChangeCallback
import com.letianpai.robot.control.mode.callback.ModeChangeCallback.ModeChangeListener
import com.letianpai.robot.control.nets.GeeUiNetManager
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.control.system.LetianpaiLightUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.letianpaiservice.LtpAppCmdCallback
import com.letianpai.robot.letianpaiservice.LtpBleCallback
import com.letianpai.robot.letianpaiservice.LtpIdentifyCmdCallback
import com.letianpai.robot.letianpaiservice.LtpLongConnectCallback
import com.letianpai.robot.letianpaiservice.LtpMiCmdCallback
import com.letianpai.robot.letianpaiservice.LtpRobotStatusCallback
import com.letianpai.robot.letianpaiservice.LtpSensorResponseCallback
import com.letianpai.robot.letianpaiservice.LtpSpeechCallback
import com.letianpai.robot.notice.receiver.NoticeCallback
import com.letianpai.robot.notice.receiver.NoticeCallback.NoticeTimeListener
import com.letianpai.robot.ota.broadcast.PackageInstallReceiver
import com.letianpai.robot.response.app.AppCmdResponser
import com.letianpai.robot.response.ble.BleCmdResponser
import com.letianpai.robot.response.identify.IdentifyCmdResponser
import com.letianpai.robot.response.mi.MiIotCmdResponser
import com.letianpai.robot.response.remote.RemoteCmdResponser
import com.letianpai.robot.response.robotStatus.RobotStatusResponser
import com.letianpai.robot.response.sensor.SensorCmdResponser
import com.letianpai.robot.response.speech.SpeechCmdResponser
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback.LTPCommandResponseListener
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback.LTPIdentifyCommandResponseListener
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback.LTPRobotStatusCmdResponseListener
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback.ExpressionChangeListener
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.commandlib.consts.MCUCommandConsts
import com.renhejia.robot.commandlib.consts.PackageConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_SET_APP_MODE
import com.renhejia.robot.commandlib.parser.config.RobotConfig
import com.renhejia.robot.commandlib.parser.deviceinfo.DeviceInfo
import com.renhejia.robot.commandlib.parser.power.PowerMotion
import com.renhejia.robot.commandlib.parser.time.ServerTime
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.gesturefactory.parser.GestureData
import com.renhejia.robot.letianpaiservice.ILetianpaiService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Random
import kotlin.math.abs

/**
 * @author liujunbin
 */
class DispatchService : Service() {
    private var iLetianpaiService: ILetianpaiService? = null
    private var isConnectService: Boolean = false
    private var mHandler: GestureHandler? = null
    private var mTaskHandler: TaskHandler? = null
    private var mControlSteeringEngineHandler: ControlSteeringEngineHandler? = null

    private var robotStatus: Int = 0
    private var previousRobotStatus: Int = 0
    private var mHour: Int = -1
    private var mMinute: Int = 0
    private var isLastTempHigh: Boolean = false
    private var allConfigCallback: Callback? = null
    private var reChargeConfigCallback: Callback? = null

    private var powerStatus: Int = POWER_OFF_IDLE
    private var hadPlayLowPowerAnimation: Boolean = false
    private var countDownTimer: CountDownTimer? = null
    private var currentBatteryPercent: Int = 0
    private var isCharging: Boolean = false
    private var installReceiver: PackageInstallReceiver? = null
    private var uploadEnterAutoCharging: Long = 0
    private var uploadExitAutoCharging: Long = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        bindLPTService()
        init()
    }

    private fun init() {
        mHandler = GestureHandler(this@DispatchService)
        mTaskHandler = TaskHandler(this@DispatchService)
        mControlSteeringEngineHandler = ControlSteeringEngineHandler(this@DispatchService)
        addTimeKeepLister()
        LauncherBroadcastReceiverManager.getInstance(this@DispatchService)
        LetianpaiLightUtil.setScreenBrightness(this@DispatchService, 10)
        startStatusBar()
        registerAppStoreReceiver()
        addCommandListener()
        addGestureListeners()
        addModeChangeListeners()
        addTemperatureListeners()
        initCallBack()
        addChargingCallback()
        addNetworkChangeListeners()
        initCountDownTimer()
        addControlSteeringEngineListeners()
        //Get Hardware Code
        encodeInfo
        reChargeConfig
    }

    override fun onDestroy() {
        super.onDestroy()
        GeeUILogUtils.logi(TAG, "--DispatchService onDestroy")
        unregisterAppStoreReceiver()
        val connectivityManager: ConnectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback!!)

        if (iLetianpaiService != null) {
            try {
                iLetianpaiService!!.unregisterLCCallback(ltpLongConnectCallback)
                iLetianpaiService!!.unregisterSpeechCallback(ltpSpeechCallback)
                iLetianpaiService!!.unregisterAppCmdCallback(ltpAppCmdCallback)
                iLetianpaiService!!.unregisterSensorResponseCallback(ltpSensorResponseCallback)
                iLetianpaiService!!.unregisterRobotStatusCallback(ltpRobotStatusCallback)
                iLetianpaiService!!.unregisterMiCmdResponseCallback(ltpMiCmdCallback)
                iLetianpaiService!!.unregisterIdentifyCmdCallback(identifyCmdCallback)
                iLetianpaiService!!.unregisterBleCmdCallback(ltpBleCallback)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            isConnectService = false
        }
        unbindService(serviceConnection)
    }

    private fun requestData() {
        encodeInfo
        userAppsConfig
        getRobotStatus()
        reChargeConfig
    }

    private fun addControlSteeringEngineListeners() {
        ControlSteeringEngineCallback.instance
            .setControlSteeringEngineListener(object : ControlSteeringEngineListener {
                override fun onControlSteeringEngine(footSwitch: Boolean, sensorSwitch: Boolean) {
                    switchPower(footSwitch)
                    switchSensor(sensorSwitch)
                }
            })
    }

    private fun registerAppStoreReceiver() {
        installReceiver = PackageInstallReceiver()
        val installIntentFilter = IntentFilter()
        installIntentFilter.addAction(PackageInstaller.ACTION_SESSION_COMMITTED)
        installIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        installIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        installIntentFilter.addDataScheme("package")
        registerReceiver(installReceiver, installIntentFilter)
    }

    private fun unregisterAppStoreReceiver() {
        if (installReceiver != null) {
            unregisterReceiver(installReceiver)
        }
    }

    /**
     * todo: This code view should not be used, delete at a later stage
     */
    private fun updateTime() {
        val current: Long = System.currentTimeMillis()
        val lastUpdateTime: Long =
            RobotConfigManager.getInstance(this@DispatchService)!!.updateTime
        val results: Long = current - lastUpdateTime

        if (results < UPDATE_INTERNAL_TIME) {
            return
        }

        GeeUiNetManager.getTimeStamp(this@DispatchService, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body != null) {
                    var serverTime: ServerTime? = null
                    var info: String? = ""
                    if (response.body != null) {
                        info = response.body!!.string()
                    }
                    try {
                        if (info != null) {
                            serverTime = Gson().fromJson(
                                info,
                                ServerTime::class.java
                            )
                            if (serverTime?.data != null && serverTime.data!!.timestamp != 0L) {
                                updateRobotTime(serverTime)
                            }
                        }
                    } catch (e: Exception) {
                        XLog.i(Log.getStackTraceString(e))
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun updateRobotTime(serverTime: ServerTime) {
        val current: Long = System.currentTimeMillis()
        RobotConfigManager.getInstance(this@DispatchService)!!.updateTime = (current)
        RobotConfigManager.getInstance(this@DispatchService)!!.commit()
        val result: Long = current - serverTime.data!!.timestamp * 1000
        if (abs(result.toDouble()) > 60 * 1000) {
            changeTime(serverTime.data!!.timestamp * 1000)
        }
    }

    private fun changeTime(time: Long) {
        val status: Boolean = SystemClock.setCurrentTimeMillis(time * 1000)
    }

    private fun initCallBack() {
        allConfigCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body != null) {
                    var robotConfig: RobotConfig? = null
                    var info: String? = ""
                    if (response.body != null) {
                        info = response.body!!.string()
                    }
                    try {
                        if (info != null) {
                            robotConfig = Gson().fromJson(
                                info,
                                RobotConfig::class.java
                            )
                            if (robotConfig?.data != null && robotConfig.data!!.device_sound_config != null) {
                                val volume: Int =
                                    robotConfig.data!!.device_sound_config!!.volume_size
                                SleepModeManager.getInstance(this@DispatchService)
                                    .setRobotVolume(volume)
                            }
                        }
                    } catch (e: Exception) {
                        XLog.i(Log.getStackTraceString(e))
                        e.printStackTrace()
                    }
                }
            }
        }
        reChargeConfigCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body != null) {
                    var reChargeInfo: ReChargeInfo? = null
                    var info: String? = ""
                    if (response.body != null) {
                        info = response.body!!.string()
                    }
                    try {
                        if (info != null) {
                            GeeUILogUtils.logi(
                                "letianpai_recharging",
                                "---reChargeConfigCallback_info: $info"
                            )
                            reChargeInfo = Gson().fromJson(
                                info,
                                ReChargeInfo::class.java
                            )
                            GeeUILogUtils.logi(
                                "letianpai_recharging",
                                "---reChargeInfo.data.getAutomatic_recharge_val() : " + reChargeInfo.data
                                    .getAutomatic_recharge_val()
                            )
                            GeeUILogUtils.logi(
                                "letianpai_recharging",
                                "---reChargeInfo.data.automatic_recharge_val: " + reChargeInfo.data
                                    .automatic_recharge_val
                            )
                            if (reChargeInfo != null && reChargeInfo.data != null && reChargeInfo.data
                                    .getAutomatic_recharge_val() != 0
                            ) {
                                RobotConfigManager.getInstance(this@DispatchService)!!
                                    .setAutomaticRechargeSwitch(
                                        reChargeInfo.data.automatic_recharge_val
                                    )
                                RobotConfigManager.getInstance(this@DispatchService)!!
                                    .automaticRechargeVal = (
                                        reChargeInfo.data.automatic_recharge_val
                                    )
                                RobotConfigManager.getInstance(this@DispatchService)!!
                                    .commit()
                            }
                        }
                    } catch (e: Exception) {
                        XLog.i(Log.getStackTraceString(e))
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getRobotStatus() {
        com.letianpai.robot.components.network.nets.GeeUiNetManager.getAllConfig(
            this@DispatchService,
            allConfigCallback
        )
    }

    private val reChargeConfig: Unit
        get() {
            Thread {
                com.letianpai.robot.components.network.nets.GeeUiNetManager.getReChargeConfig(
                    this@DispatchService,
                    SystemUtil.isInChinese(),
                    reChargeConfigCallback
                )
            }.start()
        }

    private val userAppsConfig: Unit
        get() {
            AppCmdResponser.getInstance(this@DispatchService).userAppsConfig
        }

    private val encodeInfo: Unit
        get() {
            if (SystemUtil.hasHardCode()) {
                return
            }
            GeeUiNetManager.getDeviceInfo(this@DispatchService, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.body != null) {
                        var deviceInfo: DeviceInfo? = null
                        var info: String? = ""
                        if (response.body != null) {
                            info = response.body!!.string()
                            GeeUILogUtils.logi("letianpai_getEncodeInfo", "---info: " + info)
                        }

                        try {
                            if (info != null) {
                                deviceInfo = Gson().fromJson(
                                    info,
                                    DeviceInfo::class.java
                                )
                                if (deviceInfo?.data != null && !TextUtils.isEmpty(
                                        deviceInfo.data!!.client_id
                                    ) && !TextUtils.isEmpty(
                                        deviceInfo.data!!.hard_code
                                    ) && !TextUtils.isEmpty(
                                        deviceInfo.data!!.sn
                                    )
                                ) {
                                    SystemUtil.setHardCode(
                                        deviceInfo.data!!.hard_code
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    }
                }
            })
        }

    private var networkCallback: NetworkCallback? = null

    private fun addNetworkChangeListeners() {
        val connectivityManager: ConnectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Perform the appropriate action when the network is available
                requestData()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    private fun addTemperatureListeners() {
        TemperatureUpdateCallback.instance
            .registerTemperatureUpdateListener(object : TemperatureUpdateListener {
                override fun onTemperatureUpdate(temp: Float) {
                    if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                        return
                    }
                    if (temp > TemperatureUpdateCallback.HIGH_TEMP) {
                        if (RobotModeManager.getInstance(this@DispatchService)
                                .isRobotDeepSleepMode
                        ) {
//                        updateRobotStatus(temp);
                            GeeUIStatusUploader.getInstance(this@DispatchService)
                                .uploadRobotStatus()
                            isLastTempHigh = true
                            return
                        }
                        // 进入睡眠模式
                        if (RobotModeManager.getInstance(this@DispatchService)
                                .isCommonRobotMode ||
                            LetianpaiFunctionUtil.isVideoCallRunning(this@DispatchService) ||
                            LetianpaiFunctionUtil.isVideoCallServiceRunning(this@DispatchService)
                        ) {
                            if (LetianpaiFunctionUtil.isVideoCallRunning(this@DispatchService) || LetianpaiFunctionUtil.isVideoCallServiceRunning(
                                    this@DispatchService
                                )
                            ) {
                                CommandResponseCallback.instance.setIdentifyCmd(
                                    AppCmdConsts.COMMAND_STOP_APP,
                                    AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL
                                )
                            }
                            try {
                                iLetianpaiService!!.setAppCmd(
                                    RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                    PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT
                                )
                                //High Temp Mode, turn off speech [Resolved: bug where dancing doesn't end in high temp]
                                closeSpeechAudio()
                            } catch (e: RemoteException) {
                                e.printStackTrace()
                            }
                            RobotModeManager.getInstance(this@DispatchService)
                                .switchRobotMode(
                                    ViewModeConsts.VM_DEEP_SLEEP_MODE,
                                    ViewModeConsts.VIEW_MODE_IN
                                )
                        }
                    } else if (temp <= TemperatureUpdateCallback.TARGET_TEMP) {
                        if (isLastTempHigh) {
                            isLastTempHigh = false
                        }
                        //                    updateRobotStatus(temp);
                        GeeUIStatusUploader.getInstance(this@DispatchService).uploadRobotStatus()
                        if (RobotModeManager.getInstance(this@DispatchService)
                                .isRobotDeepSleepMode
                        ) {
                            RobotModeManager.getInstance(this@DispatchService)
                                .switchRobotMode(
                                    ViewModeConsts.VM_DEEP_SLEEP_MODE,
                                    ViewModeConsts.VIEW_MODE_OUT
                                )
                        }
                    }
                }
            })
    }

    private fun stopAudioService() {
        try {
            iLetianpaiService!!.setRobotStatusCmd(
                AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAudioService() {
        try {
            iLetianpaiService!!.setRobotStatusCmd(
                AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startStatusBar() {
        LetianpaiFunctionUtil.showFloatingView(this@DispatchService)
    }

    private fun bindLPTService() {
        connectService()
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            GeeUILogUtils.logi(TAG, "---Optimist ControlService Completion of the AIDLService service")
            iLetianpaiService = ILetianpaiService.Stub.asInterface(service)
            RobotModeManager.getInstance(this@DispatchService)
                .setiLetianpaiService(iLetianpaiService)
            try {
                iLetianpaiService!!.registerLCCallback(ltpLongConnectCallback)
                iLetianpaiService!!.registerSpeechCallback(ltpSpeechCallback)
                iLetianpaiService!!.registerAppCmdCallback(ltpAppCmdCallback)
                iLetianpaiService!!.registerSensorResponseCallback(ltpSensorResponseCallback)
                iLetianpaiService!!.registerRobotStatusCallback(ltpRobotStatusCallback)
                iLetianpaiService!!.registerMiCmdResponseCallback(ltpMiCmdCallback)
                iLetianpaiService!!.registerIdentifyCmdCallback(identifyCmdCallback)
                iLetianpaiService!!.registerBleCmdCallback(ltpBleCallback)

                //              iLetianpaiService.registerExpressionCallback(expressionCallback);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            isConnectService = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            GeeUILogUtils.logi(TAG, "---Lotte Pie ControlService cannot bind to the AIDLService service of the aidlserver.")
            isConnectService = false
        }
    }

    //Link Server
    private fun connectService() {
        val intent = Intent()
        intent.setPackage("com.renhejia.robot.letianpaiservice")
        intent.setAction("android.intent.action.LETIANPAI")
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private val ltpLongConnectCallback: LtpLongConnectCallback.Stub =
        object : LtpLongConnectCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onLongConnectCommand(command: String, data: String) {
                GeeUILogUtils.logi(
                    "onLongConnectCommand",
                    "---command: $command /data: $data"
                )
                GeeUILogUtils.logi(
                    "onLongConnectCommand",
                    "---onLongConnectCommand: " + "  Current robot mode：" + RobotModeManager.getInstance(
                        this@DispatchService
                    ).robotMode
                )
                if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                    return
                }
                responseRobotCommand(
                    RobotConsts.ROBOT_COMMAND_TYPE_REMOTE,
                    iLetianpaiService!!, command, data, false
                )
            }
        }

    private val ltpSpeechCallback: LtpSpeechCallback.Stub = object : LtpSpeechCallback.Stub() {
        @Throws(RemoteException::class)
        override fun onSpeechCommandReceived(command: String, data: String) {
            if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                return
            }
            responseRobotCommand(
                RobotConsts.ROBOT_COMMAND_TYPE_SPEECH,
                iLetianpaiService!!, command, data, false
            )
        }
    }

    private val ltpAppCmdCallback: LtpAppCmdCallback.Stub = object : LtpAppCmdCallback.Stub() {
        @Throws(RemoteException::class)
        override fun onAppCommandReceived(command: String, data: String) {
            if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                return
            }
            responseRobotCommand(
                RobotConsts.ROBOT_COMMAND_TYPE_APP,
                iLetianpaiService!!, command, data, false
            )
        }
    }

    private val identifyCmdCallback: LtpIdentifyCmdCallback.Stub =
        object : LtpIdentifyCmdCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onIdentifyCommandReceived(command: String, data: String) {
                responseRobotCommand(
                    RobotConsts.ROBOT_COMMAND_TYPE_IDENTIFY,
                    iLetianpaiService!!, command, data, false
                )
            }
        }

    private val ltpMiCmdCallback: LtpMiCmdCallback.Stub = object : LtpMiCmdCallback.Stub() {
        @Throws(RemoteException::class)
        override fun onMiCommandReceived(command: String, data: String) {
            responseRobotCommand(
                RobotConsts.ROBOT_COMMAND_TYPE_MI_IOT,
                iLetianpaiService!!, command, data, false
            )
        }
    }

    private val ltpRobotStatusCallback: LtpRobotStatusCallback.Stub =
        object : LtpRobotStatusCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onRobotStatusChanged(command: String, data: String) {
                responseRobotCommand(
                    RobotConsts.ROBOT_COMMAND_TYPE_ROBOT_STATUS,
                    iLetianpaiService!!, command, data, false
                )
            }
        }

    private val ltpBleCallback: LtpBleCallback.Stub = object : LtpBleCallback.Stub() {
        @Throws(RemoteException::class)
        override fun onBleCmdReceived(command: String, data: String, isNeedResponse: Boolean) {
            responseRobotCommand(
                RobotConsts.ROBOT_COMMAND_TYPE_BLE,
                iLetianpaiService!!, command, data, isNeedResponse
            )
        }
    }

    private val ltpSensorResponseCallback: LtpSensorResponseCallback.Stub =
        object : LtpSensorResponseCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onSensorResponse(command: String, data: String) {
                GeeUILogUtils.logi("letianpai_sensor", "---command: " + command + "data: " + data)
                if (RobotModeManager.getInstance(this@DispatchService)
                        .robotMode == ViewModeConsts.VM_UNBIND_DEVICE
                ) {
                    return
                }
                // TODO Set to sensor trigger mode
                responseRobotCommand(
                    RobotConsts.ROBOT_COMMAND_TYPE_SENSOR,
                    iLetianpaiService!!, command, data, false
                )
            }
        }

    /**
     * @param commandType
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun responseRobotCommand(
        commandType: Int,
        iLetianpaiService: ILetianpaiService,
        command: String,
        data: String,
        isNeedResponse: Boolean
    ) {
//        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this)) {
//            Log.e("letianpai_tap", "responseRobotCommand === 1");
//            stopAudioService();
//            return;
//        }
        GeeUILogUtils.logi(
            TAG,
            "---responseRobotCommand:  commandType: $commandType---command::$command---data:$data"
        )

        val noNeedRes: Boolean =
            RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode
        GeeUILogUtils.logi(
            TAG,
            "---responseRobotCommand_data: ============ C ============noNeedRes: $noNeedRes"
        )
        if (noNeedRes) {
            if (command == COMMAND_SET_APP_MODE) {
                //Needs to be cleared.
            } else if (command == AppCmdConsts.COMMAND_TYPE_SET_ROBOT_MODE &&
                (data == AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_OUT
                        || data == AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_IN
                        || data == AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_OUT
                        || data == AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_IN)
            ) {
            } else {
                return
            }
        }

        if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_SENSOR) {
            GeeUILogUtils.logi(
                "letianpai_remind_notice",
                "---responseRobotCommand_data: ============ ROBOT_COMMAND_TYPE_SENSOR============"
            )
            responseRobotSensorCommand(commandType, iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_REMOTE) {
            //如果触发悬崖不执行
            GeeUILogUtils.logi(
                TAG,
                "---responseRobotCommand_data: isInCliffMode: " + RobotModeManager.getInstance(
                    this@DispatchService
                ).isInCliffMode
            )
            if (RobotModeManager.getInstance(this@DispatchService).isInCliffMode) {
                return
            }

            GeeUILogUtils.logi(
                "letianpai_remind_notice",
                "---responseRobotCommand_data: ============ I ============"
            )
            responseRobotRemoteCommand(commandType, iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_SPEECH) {
            GeeUILogUtils.logi(
                "letianpai_remind_notice",
                "---responseRobotCommand_data: ============ J ============"
            )
            responseRobotSpeechCommand(commandType, iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_APP) {
            AppCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_MI_IOT) {
            MiIotCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_ROBOT_STATUS) {
            RobotStatusResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_IDENTIFY) {
            IdentifyCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_BLE) {
            BleCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data, isNeedResponse)
        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_AUTO) {
        } else {
            //RemoteCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
        }

        //        SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService,command, data);
    }

    private fun addChargingCallback() {
        ChargingUpdateCallback.instance.registerChargingStatusUpdateListener(object :
            ChargingUpdateCallback.ChargingUpdateListener {
            override fun onChargingUpdateReceived(changingStatus: Boolean, percent: Int) {
                currentBatteryPercent = percent
                isCharging = changingStatus
                if (!changingStatus
                    && (percent < RobotConfigManager.getInstance(this@DispatchService)!!
                        .automaticRechargeVal)
                    && RobotConfigManager.getInstance(this@DispatchService)!!
                        .automaticRechargeSwitch
                    && (LetianpaiFunctionUtil.isAutoAppCanBeLaunched(this@DispatchService)) // TODO This is written as local logic first, and then updated to use the server-side logic later when the server-side interface is provided.
                    && percent >= ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD && !LetianpaiFunctionUtil.isAutoAppOnTheTop(
                        this@DispatchService
                    )
                ) {
                    //Initiate automatic recharge
                    RobotModeManager.getInstance(this@DispatchService).switchRobotMode(
                        ViewModeConsts.VM_AUTO_CHARGING, 1
                    )
                }

                if (!changingStatus && percent < ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD) {
                    if (powerStatus == POWER_OFF_IDLE) {
                        countDownTimer!!.start()
                        powerStatus = POWER_OFF_ING
                    } else if (powerStatus == POWER_OFF_ING) {
                    } else if (powerStatus == POWER_OFF_FINISH) {
                        SystemFunctionUtil.shutdownRobot(this@DispatchService)
                    }
                } else if (changingStatus) {
                    powerStatus = POWER_OFF_IDLE
                    countDownTimer!!.cancel()
                    hadPlayLowPowerAnimation = false
                } else if ((percent < ChargingUpdateCallback.LOW_BATTERY_NOTICE) && RobotModeManager.getInstance(
                        this@DispatchService
                    ).isRobotMode()
                ) {
                    //如果自动回充的话，不显示下面的提示
                    if (LetianpaiFunctionUtil.isAutoAppOnTheTop(this@DispatchService)) return

                    if (!hadPlayLowPowerAnimation) {
                        hadPlayLowPowerAnimation = true
                        ModeChangeCallback.instance
                            .setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1)
                        GestureCallback.instance.setGestures(
                            GestureCenter.lowBatteryNoticeGesture,
                            RGestureConsts.GESTURE_ROBOT_LOW_BATTERY_NOTICE
                        )
                    }
                }
            }

            override fun onChargingUpdateReceived(
                changingStatus: Boolean,
                percent: Int,
                chargePlug: Int
            ) {
            }
        })
    }

    private fun initCountDownTimer() {
        countDownTimer = object : CountDownTimer((10 * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                powerStatus = POWER_OFF_FINISH
                if (!isCharging && currentBatteryPercent <= ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD) {
                    SystemFunctionUtil.shutdownRobot(this@DispatchService)
                }
            }
        }
    }


    /**
     * @param commandType
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun responseRobotSensorCommand(
        commandType: Int,
        iLetianpaiService: ILetianpaiService,
        command: String,
        data: String
    ) {
        val robotActivate: Boolean = SystemUtil.getRobotActivateStatus()
        val isAppMode: Boolean =
            RobotModeManager.getInstance(this@DispatchService).isAppMode
        val isAlarm: Boolean = LetianpaiFunctionUtil.isAlarmOnTheTop(this@DispatchService)
        val isVideoCall: Boolean = LetianpaiFunctionUtil.isVideoCallOnTheTop(this@DispatchService)
        val isVideoCallService: Boolean = LetianpaiFunctionUtil.isVideoCallServiceRunning(
            this@DispatchService
        )
        GeeUILogUtils.logi(
            TAG,
            "---responseRobotSensorCommand::robotActivate:$robotActivate--isAppMode::$isAppMode--isAlarm:$isAlarm--isVideoCall::$isVideoCall--isVideoCallService::$isVideoCallService"
        )
        GeeUILogUtils.logi(
            TAG,
            "---responseRobotSensorCommand::command:$command--data::$data"
        )

        // if (!(robotActivate || (isAppMode && !isAlarm) || (isAppMode && isVideoCall) || (isAppMode && isVideoCallService))){
        //     return;
        // }
        if (!SystemUtil.getRobotActivateStatus()
            || ((RobotModeManager.getInstance(this@DispatchService).isAppMode))
            && (!LetianpaiFunctionUtil.isAlarmOnTheTop(this@DispatchService))
            && (!LetianpaiFunctionUtil.isVideoCallOnTheTop(this@DispatchService))
            && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(this@DispatchService))
        ) {
            return
        }
        // Log.e("letianpai_tap", "command: " + command);
        // Log.e("letianpai_tap", "data: " + data);
        if (ChargingUpdateCallback.instance
                .isCharging && (command != RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA)
        ) {
            return
        }

        val robotMode: Int =
            RobotModeManager.getInstance(this@DispatchService).robotMode
        val robotWakeupMode: Boolean =
            RobotModeManager.getInstance(this@DispatchService).isRobotWakeupMode
        val isRobotOnTheTop: Boolean = LetianpaiFunctionUtil.isRobotOnTheTop(this@DispatchService)

        if (LetianpaiFunctionUtil.isRobotOnTheTop(this@DispatchService) && !robotWakeupMode) {
            if (RobotModeManager.getInstance(this@DispatchService)
                    .isRobotDeepSleepMode
            ) {
                if (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA) {
                    SensorCmdResponser.getInstance(this@DispatchService)
                        .commandDistribute(iLetianpaiService, command, data)
                }
            } else {
                SensorCmdResponser.getInstance(this@DispatchService)
                    .commandDistribute(iLetianpaiService, command, data)
            }
        } else if (LetianpaiFunctionUtil.isSpeechOnTheTop(this@DispatchService) || LetianpaiFunctionUtil.isLexOnTheTop(
                this@DispatchService
            )
        ) {
            SensorCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (LetianpaiFunctionUtil.isSpeechRunning(this@DispatchService) || LetianpaiFunctionUtil.isLexRunning(
                this@DispatchService
            )
        ) {
            SensorCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (RobotModeManager.getInstance(this@DispatchService)
                .isRestMode && (!ChargingUpdateCallback.instance.isCharging)
        ) {
            SensorCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else if (RobotModeManager.getInstance(this@DispatchService)
                .isCloseScreenMode && (!ChargingUpdateCallback.instance
                .isCharging)
        ) {
            SensorCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        } else {
            if (LetianpaiFunctionUtil.isAlarmRunning(this@DispatchService) && (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA)) {
                SensorCmdResponser.getInstance(this@DispatchService)
                    .commandDistribute(iLetianpaiService, command, data)
            } else if ((LetianpaiFunctionUtil.isSpeechOnTheTop(this@DispatchService) || LetianpaiFunctionUtil.isLexOnTheTop(
                    this@DispatchService
                )) && ((command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA) || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_DOUBLE_TAP_DATA))
            ) {
                SensorCmdResponser.getInstance(this@DispatchService)
                    .commandDistribute(iLetianpaiService, command, data)
            } else if ((command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_BACKEND)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_FORWARD)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_LEFT)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_RIGHT)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_START_DATA)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_STOP_DATA)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_START_DATA)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_STOP_DATA)
                || (command == RobotRemoteConsts.COMMAND_TYPE_CONTROL_TOF)
            ) {
                SensorCmdResponser.getInstance(this@DispatchService)
                    .commandDistribute(iLetianpaiService, command, data)
            }
        }
    }

    /**
     * @param commandType
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun responseRobotSpeechCommand(
        commandType: Int,
        iLetianpaiService: ILetianpaiService,
        command: String,
        data: String
    ) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return
        }
        this.previousRobotStatus = this.robotStatus
        this.robotStatus = commandType
        SpeechCmdResponser.getInstance(this@DispatchService)
            .commandDistribute(iLetianpaiService, command, data)
    }

    /**
     * @param commandType
     * @param command
     * @param data
     */
    private fun responseRobotRemoteCommand(
        commandType: Int,
        iLetianpaiService: ILetianpaiService,
        command: String,
        data: String
    ) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return
        }
        this.previousRobotStatus = this.robotStatus
        this.robotStatus = commandType

        //Responds only to shutdown and reboot in high temperature mode
        if (TemperatureUpdateCallback.instance.isInHighTemperature) {
            GeeUILogUtils.logi(
                "letianpai_test",
                "---TemperatureUpdateCallback.getInstance().isInHighTemperature()1: " + TemperatureUpdateCallback.instance
                    .isInHighTemperature
            )
            if ((!TextUtils.isEmpty(command)) && (command == RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN || command == RobotRemoteConsts.COMMAND_TYPE_REBOOT)) {
                RemoteCmdResponser.getInstance(this@DispatchService)
                    .commandDistribute(iLetianpaiService, command, data)
            } else if ((!TextUtils.isEmpty(command)) && (command == MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC
                        || command == MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_MONITOR
                        || command == MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_TRANSFORM)
            ) {
                RemoteCmdResponser.getInstance(this@DispatchService)
                    .commandDistribute(iLetianpaiService, command, data)
            } else {
//                updateRobotStatus(TemperatureUpdateCallback.getInstance().getTemp());
                GeeUIStatusUploader.getInstance(this@DispatchService).uploadRobotStatus()
            }
        } else {
            RemoteCmdResponser.getInstance(this@DispatchService)
                .commandDistribute(iLetianpaiService, command, data)
        }
    }

    /**
     * Restore to last robot state
     */
    fun responseRobotStatus() {
        this.robotStatus = this.previousRobotStatus
        this.previousRobotStatus = 0
    }

    fun showGesture(gesture: String?) {
        val message = Message()
        message.what = SHOW_GESTURE_STR
        message.obj = gesture
        mHandler!!.sendMessage(message)
    }

    fun showGesture(gesture: String?, gId: Int) {
        val message: Message = Message()
        message.what = SHOW_GESTURES_WITH_ID
        message.obj = gesture
        message.arg1 = gId
        mHandler!!.sendMessage(message)
    }

    fun showGestures(list: ArrayList<GestureData>, taskId: Int) {
        val message = Message()
        message.what = SHOW_GESTURES_STR
        message.obj = list
        message.arg1 = taskId
        mHandler!!.sendMessage(message)
    }

    fun showGesture(gestureData: GestureData?) {
        val message = Message()
        message.what = SHOW_GESTURE_STR_OBJECT
        message.obj = gestureData
        mHandler!!.sendMessage(message)
    }


    private inner class GestureHandler(context: Context) : Handler() {
        private val context: WeakReference<Context> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == SHOW_GESTURE_STR) {
                if (msg.obj != null) {
                    CommandResponseManager.getInstance(this@DispatchService)
                        .responseGestures(
                            (msg.obj as String?),
                            iLetianpaiService!!
                        )
                }
            } else if (msg.what == SHOW_GESTURES_STR) {
                if (msg.obj != null) {
                    CommandResponseManager.getInstance(this@DispatchService)
                        .responseGestures(
                            (msg.obj as ArrayList<GestureData>), msg.arg1,
                            iLetianpaiService!!
                        )
                }
            } else if (msg.what == SHOW_GESTURE_STR_OBJECT) {
                if (msg.obj != null) {
                    CommandResponseManager.getInstance(this@DispatchService)
                        .responseGesture(
                            (msg.obj as GestureData?),
                            iLetianpaiService!!
                        )
                }
            } else if (msg.what == SHOW_GESTURES_WITH_ID) {
                if (msg.obj != null && msg.arg1 != 0) {
                    CommandResponseManager.getInstance(this@DispatchService)
                        .responseGestures(
                            (msg.obj as String), msg.arg1,
                            iLetianpaiService!!
                        )
                }
            }
        }
    }


    fun switchPower(powerStatus: Boolean) {
        val message: Message = Message()
        message.what = FOOT_POWER
        message.obj = powerStatus
        mControlSteeringEngineHandler!!.sendMessage(message)
    }

    fun switchSensor(sensorStatus: Boolean) {
        val message: Message = Message()
        message.what = FOOT_SENSOR
        message.obj = sensorStatus
        mControlSteeringEngineHandler!!.sendMessageDelayed(message, 50)
    }


    private inner class ControlSteeringEngineHandler(context: Context) : Handler() {
        private val context: WeakReference<Context> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val status: Boolean = (msg.obj) as Boolean
            if (msg.what == FOOT_POWER) {
                if (status) {
                    CommandResponseCallback.instance.setLTPCommand(
                        MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,
                        PowerMotion(3, 1).toString()
                    )
                } else {
                    CommandResponseCallback.instance.setLTPCommand(
                        MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,
                        PowerMotion(3, 0).toString()
                    )
                }
            } else if (msg.what == FOOT_SENSOR) {
                if (status) {
                    CommandResponseCallback.instance.setLTPCommand(
                        MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,
                        PowerMotion(5, 1).toString()
                    )
                } else {
                    CommandResponseCallback.instance.setLTPCommand(
                        MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,
                        PowerMotion(5, 0).toString()
                    )
                }
            }
        }
    }

    fun closeSpeechAudio() {
        val message = Message()
        message.arg1 = CLOSE_SPEECH_AUDIO
        mTaskHandler!!.sendMessageDelayed(message, 100)
    }

    fun closeSpeechAudioAndListening(gestureData: GestureData?) {
        val message = Message()
        message.arg1 = CLOSE_SPEECH_AUDIO_AND_LISTENING
        mTaskHandler!!.sendMessage(message)
    }

    private inner class TaskHandler(context: Context) : Handler() {
        private val context: WeakReference<Context> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.arg1 == CLOSE_SPEECH_AUDIO) {
                if (msg.arg1 != 0) {
                    closeSpeechAudio()
                }
            } else if (msg.arg1 == CLOSE_SPEECH_AUDIO_AND_LISTENING) {
                if (msg.arg1 != 0) {
                    CommandResponseManager.getInstance(this@DispatchService)
                        .responseGestures(
                            (msg.obj as ArrayList<GestureData>), msg.arg1,
                            iLetianpaiService!!
                        )
                }
            }
        }
    }

    private fun addCommandListener() {
        CommandResponseCallback.instance.setLTPCommandResponseListener(object :
            LTPCommandResponseListener {
            override fun onLTPCommandReceived(command: String?, data: String?) {
                try {
                    iLetianpaiService!!.setMcuCommand(command, data)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        })

        CommandResponseCallback.instance.setLTPRobotStatusCmdResponseListener(object :
            LTPRobotStatusCmdResponseListener {
            override fun onRobotStatusCmdResponse(command: String?, data: String?) {
                try {
                    iLetianpaiService!!.setRobotStatusCmd(command, data)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        })

        CommandResponseCallback.instance.setLTPIdentifyCommandResponseListener(object :
            LTPIdentifyCommandResponseListener {
            override fun onIdentifyCmdResponse(command: String?, data: String?) {
                try {
                    iLetianpaiService!!.setIdentifyCmd(
                        AppCmdConsts.COMMAND_STOP_APP,
                        AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL
                    )
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        })


        GestureCallback.instance
            .setGestureListener(object : GestureResponseListener {
                override fun onGestureReceived(gesture: String?) {
                    GeeUILogUtils.logi(
                        "RemoteCmdResponser",
                        "---startPrecipice: Suspended stance start === 2"
                    )
                    showGesture(gesture)
                }

                override fun onGestureReceived(gesture: String, gestureId: Int) {
                    GeeUILogUtils.logi(
                        "letianpai_task",
                        "---gesture: " + gesture + "gestureId: " + gestureId
                    )
                    showGesture(gesture, gestureId)
                }

                override fun onGesturesReceived(list: ArrayList<GestureData>, taskId: Int) {
                    GeeUILogUtils.logi(
                        "letianpai_task",
                        "---gesture_list: " + list.toString() + "taskId: : " + taskId
                    )
                    showGestures(list, taskId)
                }

                override fun onGesturesReceived(gestureData: GestureData?) {
                    GeeUILogUtils.logi(
                        "RemoteCmdResponser",
                        "---startPrecipice: Suspended stance start === 5"
                    )
                    showGesture(gestureData)
                }
            })

        ExpressionChangeCallback.instance.setExpressionChangeListener(object :
            ExpressionChangeListener {
            override fun onExpressionChanged(expression: String?) {
                GeeUILogUtils.logi("test_fukun", "---expression: $expression")
                try {
                    iLetianpaiService!!.setExpression(
                        RobotRemoteConsts.COMMAND_TYPE_FACE,
                        expression
                    )
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun onMainImageShow() {
            }

            override fun onDisplayViewShow(viewName: String?) {
                try {
                    iLetianpaiService!!.setAppCmd(
                        RobotRemoteConsts.COMMAND_TYPE_SHOW_TIME,
                        RobotRemoteConsts.COMMAND_TYPE_SHOW_TIME
                    )
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            override fun onChatGptView() {
            }

            override fun onRemoteControlViewShow() {
            }

            override fun onShowImage() {
            }

            override fun onShowText(text: String?) {
            }

            override fun onShowBlack(isShow: Boolean) {
            }

            override fun onShutdown() {
            }

            override fun onCountDownShow(time: String?) {
            }
        })

        AlarmCallback.instance.registerAlarmTimeListener(object : AlarmTimeListener {
            override fun onAlarmTimeOut(hour: Int, minute: Int) {
                if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                    return
                }
                if (LetianpaiFunctionUtil.isVideoCallRunning(this@DispatchService) || LetianpaiFunctionUtil.isVideoCallServiceRunning(
                        this@DispatchService
                    )
                ) {
                    return
                }
                GeeUILogUtils.logi("letianpai_timer", "---updateAlarm: " + hour + "_" + minute)
                //                RobotModeManager.getInstance(DispatchService.this).setAlarming(true);
                val mHour: String
                val mMinute: String
                mHour = if (hour < 10) {
                    "0$hour"
                } else {
                    "" + hour
                }

                mMinute = if (minute < 10) {
                    "0$minute"
                } else {
                    "" + minute
                }
                LetianpaiFunctionUtil.openClock(this@DispatchService, "$mHour:$mMinute")
            }
        })

        NoticeCallback.instance
            .registerNoticeTimeListener(object : NoticeTimeListener {
                override fun onNoticeTimeOut(hour: Int, minute: Int, title: String?) {
                    GeeUILogUtils.logi("letianpai_notice", "---hour: $hour")
                    GeeUILogUtils.logi("letianpai_notice", "---minute: $minute")
                    GeeUILogUtils.logi("letianpai_notice", "---title: $title")
                    if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                        return
                    }
                    if (LetianpaiFunctionUtil.isVideoCallRunning(this@DispatchService) || LetianpaiFunctionUtil.isVideoCallServiceRunning(
                            this@DispatchService
                        )
                    ) {
                        return
                    }
                    val mHour: String
                    val mMinute: String
                    mHour = if (hour < 10) {
                        "0$hour"
                    } else {
                        "" + hour
                    }

                    mMinute = if (minute < 10) {
                        "0$minute"
                    } else {
                        "" + minute
                    }
                    LetianpaiFunctionUtil.openNotices(
                        this@DispatchService,
                        "$mHour:$mMinute",
                        title
                    )
                }
            })
    }

    private fun addGestureListeners() {
        GestureCallback.instance
            .setGestureCompleteListener(object : GestureCompleteListener {
                override fun onGestureCompleted(gesture: String, taskId: Int) {
                    GeeUILogUtils.logi(
                        "onGestureCompleted_00",
                        "---gesture010--$gesture--taskId: $taskId"
                    )
                    val robotMode: Int =
                        RobotModeManager.getInstance(this@DispatchService).robotMode
                    GeeUILogUtils.logi("onGestureCompleted_11", "---robotMode: $robotMode")
                    if (robotMode == ViewModeConsts.VM_STATIC_MODE) {
                        return
                    }
                    GeeUILogUtils.logi(
                        "onGestureCompleted",
                        "---gesture--$gesture--taskId: $taskId"
                    )
                    GeeUILogUtils.logi(
                        "RemoteCmdResponser",
                        "---startPrecipice: Suspended stance start ======= 99"
                    )
                    when (taskId) {
                        RGestureConsts.GESTURE_ID_TIME_KEEPER -> if ((!LetianpaiFunctionUtil.isVideoCallRunning(
                                this@DispatchService
                            )) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(
                                this@DispatchService
                            ))
                        ) {
                            RobotModeManager.getInstance(this@DispatchService)
                                .switchToPreviousPlayMode()
                        }

                        RGestureConsts.GESTURE_ID_DANGLING_START -> {
                            GeeUILogUtils.logi(
                                "letianpai_test_control999",
                                "---RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_START_DATA ======== 1 end ========"
                            )
                            GestureCallback.instance.setGestures(
                                GestureCenter.danglingGestureData(),
                                RGestureConsts.GESTURE_ID_DANGLING_START
                            )
                        }

                        RGestureConsts.GESTURE_ID_DANGLING_END -> {
                            GeeUILogUtils.logi(
                                "letianpai_test_control999",
                                "---RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_STOP_DATA ======== 2 end ========"
                            )
                            backToPreviousPlayMode()
                        }

                        RGestureConsts.GESTURE_ID_CLIFF_FORWARD, RGestureConsts.GESTURE_ID_CLIFF_BACKEND, RGestureConsts.GESTURE_ID_CLIFF_LEFT, RGestureConsts.GESTURE_ID_CLIFF_RIGHT -> {
                            RobotModeManager.getInstance(this@DispatchService)
                                .isInCliffMode = false
                            GeeUILogUtils.logi("DispatchService", "--HANDLE--CLIFF---END--")
                            backToPreviousPlayMode()
                        }

                        RGestureConsts.GESTURE_ID_TAP, RGestureConsts.GESTURE_ID_FALLDOWN_END, RGestureConsts.GESTURE_ID_DOUBLE_TAP, RGestureConsts.GESTURE_ID_LONG_PRESS -> backToPreviousPlayMode0()
                        RGestureConsts.GESTURE_ID_Tof -> {
                            //切换回机器人模式
                            RobotModeManager.getInstance(this@DispatchService)
                                .isInTofMode = false
                            GeeUILogUtils.logi(
                                TAG, "---Obstacle avoidance complete.:" + RobotModeManager.getInstance(
                                    this@DispatchService
                                ).isInTofMode
                            )

                            // responseRobotStatus();
                            // if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                            //     RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
                            // }
                            backToPreviousPlayMode()
                        }

                        RGestureConsts.GESTURE_ID_FALLDOWN_START -> GestureCallback.instance
                            .setGestures(
                                GestureCenter.danglingGestureData(),
                                RGestureConsts.GESTURE_ID_FALLDOWN_START
                            )

                        RGestureConsts.GESTURE_ID_24_HOUR -> if ((!LetianpaiFunctionUtil.isVideoCallRunning(
                                this@DispatchService
                            )) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(
                                this@DispatchService
                            ))
                        ) {
                            RobotModeManager.getInstance(this@DispatchService)
                                .switchToPreviousPlayMode()
                        }

                        RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY -> RobotModeManager.getInstance(
                            this@DispatchService
                        ).switchToPreviousPlayMode()

                        RGestureConsts.GESTURE_COMMAND_GO_TO_SLEEP -> LetianpaiFunctionUtil.openTimeViewForSleep(
                            this@DispatchService
                        )

                        RGestureConsts.GESTURE_COMMAND_HIBERNATION -> if (RobotModeManager.getInstance(
                                this@DispatchService
                            ).isRobotDeepSleepMode
                            || RobotModeManager.getInstance(this@DispatchService)
                                .isSleepMode
                        ) {
                            GestureCallback.instance.setGestures(
                                GestureCenter.goToHibernationGesture1(),
                                RGestureConsts.GESTURE_COMMAND_HIBERNATION
                            )
                        }

                        RGestureConsts.GESTURE_MI_IOT -> if ((!LetianpaiFunctionUtil.isVideoCallRunning(
                                this@DispatchService
                            )) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(
                                this@DispatchService
                            ))
                        ) {
                            RobotModeManager.getInstance(this@DispatchService)
                                .switchToPreviousPlayMode()
                        }

                        RGestureConsts.GESTURE_POWER_ON_CHARGING -> {
                            RobotCommandWordsCallback.instance.showBattery(true)
                            val robotModeManager: RobotModeManager =
                                RobotModeManager.getInstance(
                                    this@DispatchService
                                )
                            if (robotModeManager.robotTrtcStatus == -1) {
                                robotModeManager.switchToPreviousAppMode()
                            }
                        }

                        RGestureConsts.GESTURE_ROBOT_LOW_BATTERY_NOTICE -> {
                            RobotCommandWordsCallback.instance.showBattery(true)
                            if ((!LetianpaiFunctionUtil.isVideoCallRunning(this@DispatchService)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(
                                    this@DispatchService
                                ))
                            ) {
                                RobotModeManager.getInstance(this@DispatchService)
                                    .switchToPreviousAppMode()
                            }
                        }

                        RGestureConsts.GESTURE_ID_REMIND_WATER, RGestureConsts.GESTURE_ID_REMIND_SED, RGestureConsts.GESTURE_ID_REMIND_SITE, RGestureConsts.GESTURE_ID_REMIND_KEEP -> if ((!LetianpaiFunctionUtil.isVideoCallRunning(
                                this@DispatchService
                            )) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(
                                this@DispatchService
                            ))
                        ) {
                            RobotModeManager.getInstance(this@DispatchService)
                                .switchToPreviousAppMode()
                        }

                        else -> {}
                    }
                }
            })
    }

    private fun backToPreviousPlayMode0() {
        //切换回机器人模式
        responseRobotStatus()
        val currentMode: Int =
            RobotModeManager.getInstance(this).robotMode //VM_DEMOSTRATE_MODE
        if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(this@DispatchService))
            || (LetianpaiFunctionUtil.isVideoCallServiceRunning(this@DispatchService))
            || (LetianpaiFunctionUtil.isAutoAppOnTheTop(this@DispatchService))
        ) {
            return
        }
        if (currentMode == ViewModeConsts.VM_DEMOSTRATE_MODE) {
            return
        }
        RobotModeManager.getInstance(this@DispatchService).switchToPreviousPlayMode()
    }

    private fun backToPreviousPlayMode() {
        RobotModeManager.getInstance(this).isInCliffMode = false
        responseRobotStatus()
        val currentMode: Int =
            RobotModeManager.getInstance(this).robotMode //VM_DEMOSTRATE_MODE
        GeeUILogUtils.logi(TAG, "---backToPreviousPlayMode currentMode:$currentMode")

        //         if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this))
//                 || (LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))){
//             return;
//         }
        if ((LetianpaiFunctionUtil.isAutoAppOnTheTop(this@DispatchService))) {
            return
        }

        if (currentMode == ViewModeConsts.VM_DEMOSTRATE_MODE) {
            try {
                iLetianpaiService!!.setExpression(RobotRemoteConsts.COMMAND_TYPE_FACE, "h0059")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        if (RobotModeManager.getInstance(this@DispatchService)
                .robotTrtcStatus != -1
        ) {
            return
        }
        RobotModeManager.getInstance(this@DispatchService).switchToPreviousPlayMode()
    }


    private fun addTimeKeepLister() {
        TimerKeeperCallback.instance
            .registerTimerKeeperUpdateListener(object : TimerKeeperUpdateListener {
                override fun onTimerKeeperUpdateReceived(hour: Int, minute: Int) {
                    updateTime()
                    uploadAutoChargingStatus()
                    checkService()
                    if (RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode) {
                        return
                    }
                    if (!SystemUtil.getRobotActivateStatus()) {
                        return
                    }
                    if ((!LetianpaiFunctionUtil.isVideoCallOnTheTop(this@DispatchService)) || (!LetianpaiFunctionUtil.isVideoCallServiceRunning(
                            this@DispatchService
                        ))
                    ) {
                        timeKeeperGesture(hour, minute)
                    }
                    checkRobotTemp()
                    if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(this@DispatchService)) || (LetianpaiFunctionUtil.isVideoCallServiceRunning(
                            this@DispatchService
                        ))
                    ) {
                        return
                    }

                    if (!RobotStatusResponser.getInstance(this@DispatchService).isNoNeedResponseMode && !RobotModeManager.getInstance(
                            this@DispatchService
                        ).isRobotDeepSleepMode
                    ) {
                        if (RobotModeManager.getInstance(this@DispatchService)
                                .isRobotWakeupMode
                        ) {
                            return
                        } else if (RobotModeManager.getInstance(this@DispatchService)
                                .isAppMode && (System.currentTimeMillis() - RobotStatusResponser.getInstance(
                                this@DispatchService
                            ).tapTime > 60 * 1000)
                        ) {
                            LetianpaiFunctionUtil.updateRobotSleepStatus(
                                this@DispatchService,
                                hour,
                                minute
                            )
                        } else if (!RobotModeManager.getInstance(this@DispatchService)
                                .isAppMode
                        ) {
                            LetianpaiFunctionUtil.updateRobotSleepStatus(
                                this@DispatchService,
                                hour,
                                minute
                            )
                        }
                    }
                }
            })
    }

    private fun uploadAutoChargingStatus() {
        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(this@DispatchService)) {
            if ((System.currentTimeMillis() - uploadEnterAutoCharging) > 1000 * 60 * 5) {
                com.letianpai.robot.components.network.nets.GeeUiNetManager.uploadEnterAutoCharging(
                    this@DispatchService, object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                        }

                        @Throws(IOException::class)
                        override fun onResponse(call: Call, response: Response) {
                            if (response.body != null) {
                                var info: String? = ""
                                if (response.body != null) {
                                    info = response.body!!.string()
                                }
                                var baseMessageInfo: BaseMessageInfo?
                                if (info != null) {
                                    uploadEnterAutoCharging = System.currentTimeMillis()
                                    GeeUILogUtils.logi(
                                        "letianpai_uploadEnterAutoCharging",
                                        "currentTime === info === 1 === " + info
                                    )
                                }
                            }
                        }
                    })
            }
        } else {
            if ((System.currentTimeMillis() - uploadExitAutoCharging) > 1000 * 60 * 5) {
                com.letianpai.robot.components.network.nets.GeeUiNetManager.uploadExitAutoCharging(
                    this@DispatchService, object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                        }

                        @Throws(IOException::class)
                        override fun onResponse(call: Call, response: Response) {
                            if (response.body != null) {
                                var info: String? = ""
                                if (response.body != null) {
                                    info = response.body!!.string()
                                }
                                var baseMessageInfo: BaseMessageInfo?
                                if (info != null) {
                                    uploadExitAutoCharging = System.currentTimeMillis()
                                    GeeUILogUtils.logi(
                                        "letianpai_uploadExitAutoCharging",
                                        "currentTime === info === 2 === " + info
                                    )
                                }
                            }
                        }
                    })
            }
        }
    }

    private fun checkService() {
        if (!LetianpaiFunctionUtil.isAppStoreServiceRunning(this@DispatchService)) {
            LetianpaiFunctionUtil.startAppStoreService(this@DispatchService)
        }
        if (!LetianpaiFunctionUtil.isAlarmServiceRunning(this@DispatchService)) {
            GeeUILogUtils.logi(
                "letianpai_alarm_taskservice",
                "============= create clock2 ============"
            )
            LetianpaiFunctionUtil.startAlarmService(this@DispatchService, null, null)
        }
    }


    /**
     * 检查机器人状态
     */
    private fun checkRobotTemp() {
        val temp: Float = LetianpaiFunctionUtil.cpuThermal
        TemperatureUpdateCallback.instance.setTemperature(temp)
    }

    /**
     * 24小时姿态
     *
     * @param hour
     * @param minute
     */
    private fun timeKeeperGesture(hour: Int, minute: Int) {
        if (!RobotModeManager.getInstance(this@DispatchService).isRobotMode()) {
            return
        }
        if (mHour != hour) {
            mHour = hour + 1
            val random: Random = Random()
            mMinute = random.nextInt(59) + 1
        }
        if (minute == mMinute) {
            RobotModeManager.getInstance(this@DispatchService).switchRobotMode(
                ViewModeConsts.VM_GESTURE, 1
            )
            val data: ArrayList<GestureData> = GestureCenter.hourGestureData(hour)
            if (data != null && data.size > 0 && data[0].expression != null) {
                data[0].expression!!.setIs24HourGesture(true)
            }
            GestureCallback.instance
                .setGestures(data, RGestureConsts.GESTURE_ID_24_HOUR)
        }
    }

    private fun addModeChangeListeners() {
        ModeChangeCallback.instance
            .setViewModeChangeListener(object : ModeChangeListener {
                override fun onViewModeChanged(viewMode: Int, modeStatus: Int) {
                    GeeUILogUtils.logi(
                        TAG,
                        "addModeChangeListeners()-setViewModeChangeListener: viewMode:$viewMode---:modeStatus:$modeStatus"
                    )
                    if (viewMode == ViewModeConsts.VM_DISPLAY_MODE) {
                        // TODO Launch Launcher
                    } else if (viewMode == ViewModeConsts.VM_REMOTE_CONTROL_MODE && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setRobotStatusCmd(
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO
                            )
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO
                            )
                            GeeUILogUtils.logi(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_DEMOSTRATE_MODE && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM
                            )
                            GeeUILogUtils.logi(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SPEECH
                            )
                            GeeUILogUtils.logi(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SPEECH
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_24_HOUR_PLAY && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_24HOUR
                            )
                            GeeUILogUtils.logi(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_24HOUR
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_AUTO_NEW_PLAY_MODE && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT
                            )
                            GeeUILogUtils.logi(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_SLEEP_MODE && modeStatus == 1) {
//                    try {
////                        iLetianpaiService.setRobotStatusCmd(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT,RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT);
//                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP);
//                        Log.e(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
                        try {
                            iLetianpaiService!!.setAppCmd(
                                RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT
                            )
                            GeeUILogUtils.logi(
                                RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT
                            )
                        } catch (e: Exception) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_DEEP_SLEEP_MODE && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP
                            )
                            GeeUILogUtils.logi(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if ((viewMode == ViewModeConsts.VM_CHARGING || viewMode == ViewModeConsts.VM_KILL_ROBOT_PROGRESS) && modeStatus == 1) {
                        mTaskHandler!!.postDelayed(object : Runnable {
                            override fun run() {
                                try {
                                    //The delay is to wait for the next app to get up before killing the previous one
                                    iLetianpaiService!!.setAppCmd(
                                        RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                        PackageConsts.ROBOT_PACKAGE_NAME
                                    )
                                    GeeUILogUtils.logi(
                                        RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                        PackageConsts.ROBOT_PACKAGE_NAME
                                    )
                                } catch (e: Exception) {
                                    GeeUILogUtils.logi(Log.getStackTraceString(e))
                                    e.printStackTrace()
                                }
                            }
                        }, 1000)
                    } else if ((viewMode == ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS) && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT
                            )
                        } catch (e: Exception) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if ((viewMode == ViewModeConsts.VM_KILL_IDENT_PROGRESS) && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                PackageConsts.PACKAGE_NAME_IDENT
                            )
                            GeeUILogUtils.logi(
                                RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                PackageConsts.PACKAGE_NAME_IDENT
                            )
                        } catch (e: Exception) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if ((viewMode == ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE) && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                                PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT
                            )
                            val speechCurStatus: String? =
                                SpeechCmdResponser.getInstance(this@DispatchService).speechCurrentStatus
                            if (speechCurStatus != AudioServiceConst.ROBOT_STATUS_SILENCE) {
                                closeSpeechAudio()
                            }
                        } catch (e: Exception) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if ((viewMode == ViewModeConsts.VM_TRTC_MONITOR) && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM
                            )
                            iLetianpaiService!!.setExpression(
                                RobotRemoteConsts.COMMAND_TYPE_FACE,
                                "h0227"
                            )
                        } catch (e: Exception) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if ((viewMode == ViewModeConsts.VM_TRTC_TRANSFORM) && modeStatus == 1) {
                        try {
                            iLetianpaiService!!.setAppCmd(
                                PackageConsts.ROBOT_PACKAGE_NAME,
                                RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM
                            )
                            iLetianpaiService!!.setExpression(
                                RobotRemoteConsts.COMMAND_TYPE_FACE,
                                "h0227"
                            )
                        } catch (e: RemoteException) {
                            GeeUILogUtils.logi(Log.getStackTraceString(e))
                            e.printStackTrace()
                        }
                    } else if (viewMode == ViewModeConsts.VM_KILL_SPEECH_SERVICE && modeStatus == 1) {
                        val speechCurStatus: String? =
                            SpeechCmdResponser.getInstance(this@DispatchService).speechCurrentStatus
                        if (speechCurStatus != AudioServiceConst.ROBOT_STATUS_SILENCE) {
                            closeSpeechAudio()
                        }
                    }
                }
            })
    }

    companion object {
        private val UPDATE_INTERNAL_TIME: Long = (60 * 60 * 1000).toLong()
        private const val TAG: String = "DispatchService"

        private const val SHOW_GESTURE: Int = 1
        private const val SHOW_GESTURE_STR: Int = 2
        private const val SHOW_GESTURES_STR: Int = 3
        private const val SHOW_GESTURES_WITH_ID: Int = 4
        private const val SHOW_GESTURE_STR_OBJECT: Int = 5
        private const val CLOSE_SPEECH_AUDIO: Int = 11
        private const val CLOSE_SPEECH_AUDIO_AND_LISTENING: Int = 12

        private const val FOOT_POWER: Int = 21
        private const val FOOT_SENSOR: Int = 22

        private const val POWER_OFF_IDLE: Int = 1
        private const val POWER_OFF_ING: Int = 2
        private const val POWER_OFF_FINISH: Int = 3
    }
}
