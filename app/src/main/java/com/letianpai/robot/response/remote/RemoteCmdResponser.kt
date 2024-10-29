package com.letianpai.robot.response.remote

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.letianpai.robot.alarm.GeeUIAlarmManager
import com.letianpai.robot.components.network.nets.GeeUIStatusUploader
import com.letianpai.robot.components.parser.recharge.ReChargeData
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.manager.SleepModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.callback.ModeChangeCallback
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.control.storage.SPUtils
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.control.system.LetianpaiLightUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.response.RobotFuncResponseManager
import com.letianpai.robot.response.app.AppCmdResponser
import com.letianpai.robot.response.robotStatus.RobotStatusResponser
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.commandlib.consts.MCUCommandConsts
import com.renhejia.robot.commandlib.consts.RobotExpressionConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.parser.DisplayMode.DisplayMode
import com.renhejia.robot.commandlib.parser.automode.AutoMode
import com.renhejia.robot.commandlib.parser.battery.BatterySwitch
import com.renhejia.robot.commandlib.parser.ble.BleConfig
import com.renhejia.robot.commandlib.parser.changemode.ModeChange
import com.renhejia.robot.commandlib.parser.clock.ClockInfos
import com.renhejia.robot.commandlib.parser.config.UserAppsConfig
import com.renhejia.robot.commandlib.parser.config.UserAppsConfigModel
import com.renhejia.robot.commandlib.parser.face.Face
import com.renhejia.robot.commandlib.parser.light.LightControl
import com.renhejia.robot.commandlib.parser.showmode.ChangeShowModule
import com.renhejia.robot.commandlib.parser.sleepmode.SleepModeConfig
import com.renhejia.robot.commandlib.parser.sound.Sound
import com.renhejia.robot.commandlib.parser.svolume.VolumeControl
import com.renhejia.robot.commandlib.parser.timeformat.TimeFormat
import com.renhejia.robot.commandlib.parser.timezone.TimeZone
import com.renhejia.robot.commandlib.parser.trtc.TRTC
import com.renhejia.robot.commandlib.parser.word.Word
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.letianpaiservice.ILetianpaiService
import java.util.Calendar
import java.util.stream.Collectors

/**
 * @author liujunbin
 */
class RemoteCmdResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        //Automatic APP switching
        if (command == "updateAppAutoShow") {
            //Save state to file
            val gson = Gson()
            try {
                val map = gson.fromJson<Map<String, Any>>(
                    data,
                    object : TypeToken<HashMap<String?, Any?>?>() {
                    }.type
                )
                if (map != null) {
                    val isAutoShow = map["is_auto_show"] as Double
                    //Setting the Memory Status
                    AppCmdResponser.getInstance(mContext)
                        .isAutoSwitchApp = isAutoShow == 1.0 //1, automatic switching, 0, manual switching
                    SPUtils.getInstance(mContext)?.putDouble("isAutoShow", isAutoShow)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        XLog.i("$TAG---commandDistribute:command $command  data:$data")
        val robotMode = RobotModeManager.getInstance(mContext).robotMode
        Log.e("---$TAG", "robotMode_AAA: $robotMode")
        when (command) {
            RobotRemoteConsts.COMMAND_TYPE_MOTION, RobotRemoteConsts.COMMAND_TYPE_ANTENNA_LIGHT, RobotRemoteConsts.COMMAND_TYPE_ANTENNA_MOTION -> responseRemoteControl(
                iLetianpaiService,
                command,
                data
            )

            RobotRemoteConsts.COMMAND_TYPE_SOUND -> if (LetianpaiFunctionUtil.isRobotOnTheTop(
                    mContext
                )
            ) {
                responseSound(iLetianpaiService, data)
            }

            RobotRemoteConsts.COMMAND_TYPE_RESET_DEVICE_INFO -> // Restore Factory Settings Network Request
                LetianpaiFunctionUtil.resetRobot(mContext)

            RobotRemoteConsts.COMMAND_TYPE_FACE -> responseFace(data)
            MCUCommandConsts.COMMAND_TYPE_TRTC -> responseTRTC(data)
            MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC -> responseExitTRTC(data)
            MCUCommandConsts.COMMAND_TYPE_TRTC_MONITOR -> responseTRTCMonitor(data)
            MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_MONITOR -> responseExitTRTCMonitor(data)
            MCUCommandConsts.COMMAND_TYPE_TRTC_TRANSFORM -> responseTRTCTransform(data)
            MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_TRANSFORM -> responseExitTRTCTransform(data)
            RobotRemoteConsts.COMMAND_TYPE_OTA -> {}
            RobotRemoteConsts.COMMAND_TYPE_CONTROL_SEND_WORD -> responseRemoteWord(
                iLetianpaiService,
                data
            )

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_DISPLAY_MODE -> responseDisplayViewControl(data)
            RobotRemoteConsts.COMMAND_TYPE_CONTROL_SOUND_VOLUME -> responseSoundVolumeControl(data)
            RobotRemoteConsts.COMMAND_TYPE_CONTROL_AUTO_MODE -> responseControlAutoMode(
                data,
                iLetianpaiService
            )

            RobotRemoteConsts.COMMAND_TYPE_CHANGE_MODE -> setRobotMode(iLetianpaiService, data)
            RobotRemoteConsts.COMMAND_TYPE_UPDATE_CLOCK_DATA -> if (!LetianpaiFunctionUtil.isAlarmServiceRunning(
                    mContext
                )
            ) {
                LetianpaiFunctionUtil.startAlarmService(mContext, data)
            }

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_DATE_CONFIG -> updateSystemTime(data)
            RobotRemoteConsts.COMMAND_TYPE_CHANGE_SHOW_MODULE -> responseChangeApp(
                iLetianpaiService,
                data
            )

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_AWAKE_CONFIG -> {}
            RobotRemoteConsts.COMMAND_TYPE_APP_DISPLAY_SWITCH_CONFIG -> {}
            RobotRemoteConsts.COMMAND_TYPE_UPDATE_GENERAL_CONFIG -> try {
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_GENERAL_CONFIG,
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_GENERAL_CONFIG
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_WEATHER_CONFIG -> try {
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_WEATHER_CONFIG,
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_WEATHER_CONFIG
                )
                // com.letianpai.robot.taskservice.remote.RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_CALENDAR_CONFIG -> try {
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_CALENDAR_CONFIG,
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_CALENDAR_CONFIG
                )
                // com.letianpai.robot.taskservice.remote.RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_FANS_CONFIG -> try {
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_FANS_CONFIG,
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_FANS_CONFIG
                )
                // com.letianpai.robot.taskservice.remote.RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_COUNT_DOWN_CONFIG -> try {
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_COUNT_DOWN_CONFIG,
                    RobotRemoteConsts.COMMAND_TYPE_UPDATE_COUNT_DOWN_CONFIG
                )
                // com.letianpai.robot.taskservice.remote.RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

            RobotRemoteConsts.COMMAND_TYPE_UPDATE_BATTERY_MODE_CONFIG -> updateBatteryModeConfig(
                data
            )

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_BRIGHTNESS -> updateRobotBrightness(
                mContext,
                data
            )

            RobotRemoteConsts.COMMAND_TYPE_REBOOT -> responseReboot()
            RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN -> responseShutdown()
            RobotRemoteConsts.COMMAND_TYPE_START_BIND_MIJIA -> startMijia(iLetianpaiService)
            RobotRemoteConsts.COMMAND_OPEN_AE -> openAudioEffect(iLetianpaiService)
            RobotRemoteConsts.COMMAND_CLOSE_AE -> closeAudioEffect(iLetianpaiService)
            RobotRemoteConsts.COMMAND_SHOW_CHARGING_ICON -> showChargingIcon()
            RobotRemoteConsts.COMMAND_HIDE_CHARGING_ICON -> hideChargingIcon()
            RobotRemoteConsts.COMMAND_CONTROL_TAKE_PHOTO -> takePhoto()
            RobotRemoteConsts.COMMAND_CONTROL_OPEN_DEVELOP_OPTIONS -> openDevelopOptions()
            RobotRemoteConsts.COMMAND_CONTROL_CLOSE_DEVELOP_OPTIONS -> closeDevelopOptions()
            RobotRemoteConsts.COMMAND_TYPE_UPDATE_SLEEP_MODE_CONFIG -> updateSleepModeInfo(data)
            RobotRemoteConsts.COMMAND_TYPE_UPDATE_BLE_CONFIG -> responseBleConfigChange(data)
            RobotRemoteConsts.COMMAND_VALUE_REMOVE_DEVICE -> unbindDevice()
            RobotRemoteConsts.COMMAND_TYPE_CONTROL_GYROSCOPE -> controlGyroscope(data)
            RobotRemoteConsts.COMMAND_TYPE_UPDATE_DEVICE_APP_MODE -> updateLocalApp(
                iLetianpaiService,
                data
            )

            RobotRemoteConsts.COMMAND_UPDATE_DEVICE_TIME_ZONE -> updateTimeZone(
                iLetianpaiService,
                data
            )

            RobotRemoteConsts.COMMAND_UPDATE_REAL_BATTERY -> GeeUIStatusUploader.getInstance(
                mContext
            )!!.uploadRobotStatusData()

            RobotRemoteConsts.COMMAND_UPDATE_AUTOMATIC_RECHARGE_CONFIG -> updateAutoChargingConfig(
                data
            )

            RobotRemoteConsts.COMMAND_VALUE_CLOSE_APP -> {
                val changeShowModule = mGson!!.fromJson(
                    data,
                    ChangeShowModule::class.java
                )
                if (changeShowModule != null) {
                    val packageName = changeShowModule.select_module_tag_list[0]
                    Log.i(
                        "---$TAG",
                        "close app packagename:$packageName"
                    )
                    if (packageName != null && packageName != "") {
                        LetianpaiFunctionUtil.closeAppByPackageName(packageName, mContext)
                        LetianpaiFunctionUtil.currentPackageName = ""
                        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
                    }
                }
            }

            else -> {}
        }
    }

    private fun updateAutoChargingConfig(data: String) {
        val reChargeData = mGson!!.fromJson(data, ReChargeData::class.java)
        if (reChargeData != null && reChargeData.automatic_recharge_val != 0) {
            RobotConfigManager.getInstance(mContext)!!.automaticRechargeVal =
                reChargeData.automatic_recharge_val
            RobotConfigManager.getInstance(mContext)!!
                .setAutomaticRechargeSwitch(reChargeData.automatic_recharge_switch)
            RobotConfigManager.getInstance(mContext)?.commit()
        }
    }

    private fun updateTimeZone(iLetianpaiService: ILetianpaiService, data: String) {
        val timeZone = mGson!!.fromJson(
            data,
            TimeZone::class.java
        )
        if (timeZone != null && !TextUtils.isEmpty(timeZone.zone)) {
            (mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setTimeZone(timeZone.zone)
            //设置系统不自动更新时区
            // ContentResolver contentResolver = mContext.getContentResolver();
            // Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME, 0);
            // Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 0);
            // String currentTimeZone = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE);
            // Log.d("Timezone", "Current timezone: " + currentTimeZone);
        }
    }

    private fun updateLocalApp(iLetianpaiService: ILetianpaiService, data: String) {
    }

    private fun controlGyroscope(data: String) {
        CommandResponseCallback.instance.setLTPCommand(
            RobotRemoteConsts.COMMAND_TYPE_CONTROL_GYROSCOPE,
            data
        )
    }

    private fun responseExitTRTCTransform(data: String) {
        RobotModeManager.getInstance(mContext).resetTrtcStatus()
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
        CommandResponseCallback.instance.setIdentifyCmd(
            AppCmdConsts.COMMAND_STOP_APP,
            AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL
        )
        CommandResponseCallback.instance.setLTPCommand(
            MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE,
            MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE
        )
        Thread {
            try {
                Thread.sleep(2000)
                CommandResponseCallback.instance.setRobotStatusCmdResponse(
                    AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE,
                    AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE
                )
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * 远程变声
     *
     * @param data
     */
    private fun responseTRTCTransform(data: String) {
        RobotModeManager.getInstance(mContext).robotTrtcStatus = 3
        LetianpaiFunctionUtil.changeToStand(mContext, true)
        LetianpaiFunctionUtil.hideStatusBar()
        ModeChangeCallback.instance.setModeChange(ViewModeConsts.VM_KILL_IDENT_PROGRESS, 1)
        LetianpaiFunctionUtil.openRobotMode(
            mContext,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_TRANSFORM,
            "h0227"
        )
        CommandResponseCallback.instance.setRobotStatusCmdResponse(
            AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE,
            AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE
        )

        val trtc = mGson!!.fromJson(data, TRTC::class.java)
        if (trtc != null) {
            Thread {
                try {
                    //延迟是为了释放资源，防止滋滋声
                    Thread.sleep(1500)
                    openVideoService(trtc.room_id, trtc.user_id, trtc.user_sig)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }.start()
        }
        CommandResponseCallback.instance.setLTPCommand(
            MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE,
            MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE
        )
    }

    private fun responseExitTRTCMonitor(data: String) {
        RobotModeManager.getInstance(mContext).resetTrtcStatus()
        // long currentTime = System.currentTimeMillis();
        // LetianpaiFunctionUtil.setExitLongTrtcTime(currentTime);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
        CommandResponseCallback.instance.setIdentifyCmd(
            AppCmdConsts.COMMAND_STOP_APP,
            AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL
        )
        CommandResponseCallback.instance.setLTPCommand(
            MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE,
            MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE
        )
        XLog.i("$TAG----responseExitTRTCMonitor----")
        Thread {
            try {
                XLog.i("letianpai_trtc", "================ 1 ===============")
                Thread.sleep(2000)
                CommandResponseCallback.instance.setRobotStatusCmdResponse(
                    AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE,
                    AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE
                )
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * 视频监控
     *
     * @param data
     */
    private fun responseTRTCMonitor(data: String) {
        RobotModeManager.getInstance(mContext).robotTrtcStatus = 1
        LetianpaiFunctionUtil.changeToStand(mContext, true)
        LetianpaiFunctionUtil.hideStatusBar()
        ModeChangeCallback.instance.setModeChange(ViewModeConsts.VM_KILL_IDENT_PROGRESS, 1)
        LetianpaiFunctionUtil.openRobotMode(
            mContext,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_MONITOR,
            "h0227"
        )
        CommandResponseCallback.instance.setRobotStatusCmdResponse(
            AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE,
            AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE
        )

        val trtc = mGson!!.fromJson(data, TRTC::class.java)
        if (trtc != null) {
            Thread {
                try {
                    //延迟是为了释放资源，防止滋滋声
                    Thread.sleep(1500)
                    openVideoService(trtc.room_id, trtc.user_id, trtc.user_sig)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }.start()
        }
        CommandResponseCallback.instance.setLTPCommand(
            MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE,
            MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE
        )
    }

    private fun responseExitTRTC(data: String) {
        RobotModeManager.getInstance(mContext).resetTrtcStatus()
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
        CommandResponseCallback.instance.setRobotStatusCmdResponse(
            AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE,
            AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE
        )
        CommandResponseCallback.instance.setLTPCommand(
            MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE,
            MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE
        )
    }

    private fun unbindDevice() {
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
        GestureCallback.instance.setGestures(
            GestureCenter.stopSoundEffectData(),
            RGestureConsts.GESTURE_ID_CLOSE_SOUND_EFFECT
        )
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_UNBIND_DEVICE, 0)
        RobotModeManager.getInstance(mContext).resetStatus()
        //停止服务
        // Intent intent1 = new Intent(mContext, DispatchService.class);
        // mContext.stopService(intent1);
        // new Handler(mContext.getMainLooper()).postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        //         //解除绑定,重置状态
        //         System.exit(0);
        //     }
        // },3000);
    }

    private fun responseBleConfigChange(data: String) {
        val bleConfig = mGson!!.fromJson(data, BleConfig::class.java)
            ?: return
        if (bleConfig.ble_switch == 1) {
            LetianpaiFunctionUtil.startBleService(mContext)
        } else if (bleConfig.ble_switch == 0) {
            LetianpaiFunctionUtil.stopBleService(mContext)
        }
    }

    /**
     * 更新睡眠模式信息
     */
    private fun updateSleepModeInfo(data: String) {
        val sleepModeConfig = mGson!!.fromJson(
            data,
            SleepModeConfig::class.java
        )
        if (sleepModeConfig != null) {
            if (sleepModeConfig == null && TextUtils.isEmpty(sleepModeConfig.start_time) || TextUtils.isEmpty(
                    sleepModeConfig.end_time
                )
            ) {
                return
            }
        }
        //        if (sleepModeConfig.getSleep_mode_switch() == 1){
        if (sleepModeConfig.close_screen_mode_switch == 1) {
            RobotConfigManager.getInstance(mContext)!!.closeScreenModeSwitch = true
        } else {
            RobotConfigManager.getInstance(mContext)!!.closeScreenModeSwitch = false
        }

        if (sleepModeConfig.sleep_sound_mode_switch == 1) {
            RobotConfigManager.getInstance(mContext)!!.sleepSoundModeSwitch = true
        } else {
            RobotConfigManager.getInstance(mContext)!!.sleepSoundModeSwitch = false
        }

        RobotConfigManager.getInstance(mContext)!!.sleepTimeStatusModeSwitch =
            sleepModeConfig.sleep_time_status_mode_switch

        val startTime = LetianpaiFunctionUtil.getCompareTime(sleepModeConfig.start_time!!)
        val endTime = LetianpaiFunctionUtil.getCompareTime(sleepModeConfig.end_time!!)

        XLog.i("letianpai_sleep" + "---sleep_status: " + RobotConfigManager.getInstance(mContext)!!.sleepModeStatus)
        XLog.i("letianpai_sleep---startTime: $startTime")
        XLog.i("letianpai_sleep---endTime: $endTime")

        RobotConfigManager.getInstance(mContext)!!.sleepStartTime = startTime
        RobotConfigManager.getInstance(mContext)!!.sleepEndTime = endTime
        RobotConfigManager.getInstance(mContext)!!.commit()

        updateRobotSleepStatus()
    }


    //    private void updateRobotSleepStatus() {
    //        Calendar calendar = Calendar.getInstance();
    //        int hour = calendar.get(Calendar.HOUR_OF_DAY);
    //        int minute = calendar.get(Calendar.MINUTE);
    //        int currentTime = LetianpaiFunctionUtil.getCompareTime(hour,minute);
    //
    //        Log.e("letianpai_sleep","hour: "+ hour);
    //        Log.e("letianpai_sleep","minute: "+ minute);
    //        Log.e("letianpai_sleep","currentTime: "+ currentTime);
    //
    //        boolean isInRange = LetianpaiFunctionUtil.isTimeInSleepRange(mContext,hour,minute);
    //        boolean isSleepModeOn = RobotConfigManager.getInstance(mContext).getSleepModeStatus();
    //        int currentMode = RobotModeManager.getInstance(mContext).getRobotMode();
    //        Log.e("letianpai_sleep","isInRange: "+ isInRange);
    //        if (isInRange && isSleepModeOn ){
    //            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE,1);
    //        }else if ((!isInRange || !isSleepModeOn) && (currentMode == ViewModeConsts.VM_SLEEP_MODE) ){
    //            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    //        }
    //
    //    }
    private fun updateRobotSleepStatus() {
        val calendar = Calendar.getInstance()
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val currentTime = LetianpaiFunctionUtil.getCompareTime(hour, minute)
        XLog.i(TAG + "---hour: " + hour + "---minute::" + minute + "---currentTime::" + minute)
        LetianpaiFunctionUtil.updateRobotSleepStatus(mContext, hour, minute)
    }

    /**
     * 关闭开发者模式
     */
    private fun closeDevelopOptions() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.ADB_ENABLED, 0)
        Settings.Global.putInt(
            mContext.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )
        val status1 =
            Settings.Global.getInt(mContext.contentResolver, Settings.Global.ADB_ENABLED, 0)
        val status2 = Settings.Global.getInt(
            mContext.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )
        XLog.i(TAG + "---closeDevelopOptions: === 1 === status1: " + status1 + "-----status2::" + status2)
    }

    /**
     * 打开开发者模式
     */
    private fun openDevelopOptions() {
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.ADB_ENABLED, 1)
        Settings.Global.putInt(
            mContext.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
        val status1 =
            Settings.Global.getInt(mContext.contentResolver, Settings.Global.ADB_ENABLED, 0)
        val status2 = Settings.Global.getInt(
            mContext.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )
        XLog.i(TAG + "---openDevelopOptions: === 1 === status1: " + status1 + "-----status2::" + status2)
    }

    private fun takePhoto() {
        LetianpaiFunctionUtil.takePhoto(mContext)
    }

    private fun hideChargingIcon() {
        RobotModeManager.getInstance(mContext).setShowChargingIconInRobotMode(false)
    }

    private fun showChargingIcon() {
        RobotModeManager.getInstance(mContext).setShowChargingIconInRobotMode(true)
    }

    private fun closeAudioEffect(iLetianpaiService: ILetianpaiService) {
        try {
            iLetianpaiService.setAudioEffect(
                RobotRemoteConsts.COMMAND_CLOSE_AE,
                RobotRemoteConsts.COMMAND_CLOSE_AE
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun openAudioEffect(iLetianpaiService: ILetianpaiService) {
        try {
            iLetianpaiService.setAudioEffect(
                RobotRemoteConsts.COMMAND_OPEN_AE,
                RobotRemoteConsts.COMMAND_OPEN_AE
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun responseShutdown() {
        SystemFunctionUtil.shutdownRobot(mContext)
    }

    private fun startMijia(iLetianpaiService: ILetianpaiService) {
        LetianpaiFunctionUtil.startMIotService(iLetianpaiService, mContext)
    }

    private fun responseReboot() {
        SystemFunctionUtil.reboot(mContext)
    }

    private fun updateRobotBrightness(mContext: Context, data: String) {
        val lightControl = mGson!!.fromJson(data, LightControl::class.java)
        if (lightControl != null) {
            LetianpaiLightUtil.setScreenBrightness(mContext, lightControl.volume_size)
        }
    }

    private fun updateBatteryModeConfig(data: String) {
        val batterySwitch =
            mGson!!.fromJson(data, BatterySwitch::class.java) ?: return
        val status = batterySwitch.general_battery_switch
        XLog.i(TAG + "---updateBatteryModeConfig---status: " + status)

        if (status == 1) {
            RobotConfigManager.getInstance(mContext)!!.setGeneralBatterySwitchStatus(true)
            RobotConfigManager.getInstance(mContext)!!.commit()
        } else if (status == 0) {
            RobotConfigManager.getInstance(mContext)!!.setGeneralBatterySwitchStatus(false)
            RobotConfigManager.getInstance(mContext)!!.commit()
        }
    }

    private fun responseSound(iLetianpaiService: ILetianpaiService, data: String) {
        val sound = mGson!!.fromJson(data, Sound::class.java) ?: return
        val sounds = sound.sound
        XLog.i("$TAG---responseSound---sounds::$sounds")

        if (TextUtils.isEmpty(sounds)) {
            return
        }
        try {
            iLetianpaiService.setAudioEffect(RobotRemoteConsts.COMMAND_TYPE_SOUND, sounds)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun responseRemoteControl(
        iLetianpaiService: ILetianpaiService?,
        command: String,
        data: String
    ) {
        if (iLetianpaiService != null && (!TextUtils.isEmpty(command))) {
            try {
                iLetianpaiService.setMcuCommand(command, data)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private fun responseChangeApp(iLetianpaiService: ILetianpaiService, data: Any) {
        //test code start
        LetianpaiFunctionUtil.getTopActivity(mContext)

        //test code end
        RobotStatusResponser.getInstance(mContext).setTapTime()
        val changeShowModule = mGson!!.fromJson(
            data as String,
            ChangeShowModule::class.java
        )
        if (changeShowModule != null) {
            val changeShowModules = changeShowModule.select_module_tag_list
            if (changeShowModules != null && changeShowModules.isNotEmpty()) {
                when (val changeModule = changeShowModule.select_module_tag_list[0]) {
                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_EVENT -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_EVENT_COUNTDOWN
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WEATHER -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_WEATHER
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_TIME
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_FANS -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_FANS
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_MESSAGE -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_MESSAGE
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_CUSTOM -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_CUSTOM
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_NEWS -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_NEWS
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_COMMEMORATION -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_COMMEMORATION
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_STOCK -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_STOCK
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WORD -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_WORD
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_LAMP -> {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_STATIC_MODE,
                            ViewModeConsts.APP_MODE_LAMP
                        )
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_SLEEP -> {
                        RobotModeManager.getInstance(mContext)
                            .switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1)
                        RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    }

                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_ROBOT -> {
                        RobotFuncResponseManager.getInstance(mContext)
                            .openRobotMode(RobotFuncResponseManager.OPEN_TYPE_REMOTE)
                    }

                    else -> {
                        LetianpaiFunctionUtil.openUniversalApp(
                            mContext,
                            changeModule,
                            iLetianpaiService
                        )
                    }
                }
            }
        }
    }

    fun closeUniversalApp(name: String, iLetianpaiService: ILetianpaiService?) {
        val userAppsConfigModel: UserAppsConfigModel =
            AppCmdResponser.getInstance(mContext).userAppsConfigModel!!
        if (userAppsConfigModel != null && userAppsConfigModel.data!!.isNotEmpty()) {
            val list = if (name.contains(".")) { //包名过滤
                userAppsConfigModel.data!!.stream()
                    .filter { item: UserAppsConfig -> item.appPackageName == name }
                    .collect(Collectors.toList())
            } else {
                userAppsConfigModel.data!!.stream()
                    .filter { item: UserAppsConfig -> item.appName == name }
                    .collect(Collectors.toList())
            }
            if (!list.isEmpty()) {
                val userAppsConfig = list.stream().findFirst().get()
                XLog.i(TAG + "---open others app.appName::" + userAppsConfig.appName + "--appPackageName::" + userAppsConfig.appPackageName + "--openContent::" + userAppsConfig.openContent)

                LetianpaiFunctionUtil.closeAppByPackageName(userAppsConfig.appPackageName, mContext)
                RobotModeManager.getInstance(mContext).switchToPreviousAppMode()
            }
        }
    }

    private fun responseSoundVolumeControl(data: String) {
        val volumeControl = mGson!!.fromJson(data, VolumeControl::class.java)
        if (volumeControl != null) {
            SleepModeManager.getInstance(mContext).setRobotVolume(volumeControl.volume_size)

            //            if (volumeControl.getVolume_size() > 12){
//                LetianpaiLightUtil.setScreenBrightness(mContext,250);
//            }else{
//                LetianpaiLightUtil.setScreenBrightness(mContext,120);
//            }
        }
    }

    private fun updateSystemTime(data: String) {
        val timeFormat = mGson!!.fromJson(
            data,
            TimeFormat::class.java
        )
        //TODO 需要Launcher响应系统时间变化
        if (timeFormat != null) {
            if (timeFormat.hour_24_switch == 1) {
                SystemFunctionUtil.set24HourFormat(mContext)
            } else {
                SystemFunctionUtil.set12HourFormat(mContext)
            }
        }
    }

    private fun updateClockData(data: String) {
//        {"clock_id":20,"action":"add","clock_info":{"clock_id":20,"clock_hour":23,"clock_min":39,"clock_time":"23:39","clock_title":"测试一下","is_on":1,"repeat_method":[1,4],"repeat_method_label":"星期一 星期四"}}
        XLog.i(TAG + "---updateClockData::: " + data)
        val clockInfos = mGson!!.fromJson(data, ClockInfos::class.java)
        if (clockInfos == null || clockInfos.clock_id == 0 || clockInfos.clock_info == null || clockInfos.clock_info!!.clock_time == null) {
            return
        }
        val times =
            clockInfos.clock_info!!.clock_time!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        if (times == null && times.size < 2) {
            return
        }
        val times0 = times[0].toInt()
        val times1 = times[1].toInt()
        XLog.i(TAG + "---updateClockData---times0:" + times0 + "---times1::" + times1)
        GeeUIAlarmManager.getInstance(mContext).createAlarm(times0, times1)

        //        LetianpaiFunctionUtil.openClock(mContext,clockInfos.getClock_info().getClock_time());
//        GeeUIAlarmManager.getInstance(mContext).createAlarm(mContext,Integer.valueOf(times[0]), Integer.valueOf(times[1]));
    }

    /**
     * 响应自动演示模式
     *
     * @param data
     */
    private fun responseControlAutoMode(data: String, iLetianpaiService: ILetianpaiService) {
        XLog.i(TAG + "---responseControlAutoMode:::响应自动演示模式: " + data)
        val autoMode = mGson!!.fromJson(data, AutoMode::class.java)
        if (autoMode?.cmd_tag == null) {
            return
        }
        val cmd_tag = autoMode.cmd_tag
        if (cmd_tag == RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_FOLLOW) {
            openPeopleFollow()
        } else if (cmd_tag == RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW) {
            exitPeopleFollow(iLetianpaiService)
        }
    }


    private fun exitPeopleFollow(iLetianpaiService: ILetianpaiService?) {
        if (iLetianpaiService != null) {
            try {
//                iLetianpaiService.setCommand(new LtpCommand(RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW, null));
                //TODO 第二个参数没用
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW,
                    RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    private fun responseRemoteWord(iLetianpaiService: ILetianpaiService, data: String) {
        val word = mGson!!.fromJson(data, Word::class.java)
        if (word != null && (word.word != null)) {
//            ExpressionChangeCallback.getInstance().showRemoteText(word.getWord());
            try {
//                iLetianpaiService.setTTS("speakText",word.getWord());
                if (word.word == RobotRemoteConsts.COMMAND_OPEN_AE) {
                    openAudioEffect(iLetianpaiService)
                } else if (word.word == RobotRemoteConsts.COMMAND_CLOSE_AE) {
                    closeAudioEffect(iLetianpaiService)
                } else {
                    iLetianpaiService.setTTS("speakText", word.word)
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 设置机器人模式
     *
     * @param data
     */
    private fun setRobotMode(iLetianpaiService: ILetianpaiService, data: String) {
        XLog.i(TAG + "---setRobotMode 1---data::" + data)

        val modeChange = mGson!!.fromJson(data, ModeChange::class.java)
        if (modeChange?.mode == null) {
            return
        }
        val mode = modeChange.mode
        XLog.i(TAG + "----setRobotMode 2---mode::" + mode)

        val modeStatus = modeChange.mode_status
        when (mode) {
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM -> RobotModeManager.getInstance(
                mContext
            ).switchRobotMode(
                ViewModeConsts.VM_REMOTE_CONTROL_MODE, modeStatus
            )

            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SHOW, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_STATIC -> {
                //                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DISPLAY_MODE, mode_status);
                if (ChargingUpdateCallback.instance.isCharging && (!RobotModeManager.getInstance(
                        mContext
                    ).isAppMode)
                ) {
                    RobotModeManager.getInstance(mContext).switchRobotMode(
                        ViewModeConsts.VM_DISPLAY_MODE,
                        RobotModeManager.getInstance(mContext).robotAppModeStatus
                    )
                } else {
                    RobotModeManager.getInstance(mContext).switchRobotMode(
                        ViewModeConsts.VM_DISPLAY_MODE,
                        RobotModeManager.getInstance(mContext).robotAppModeStatus
                    )
                }

                RobotFuncResponseManager.stopRobot(iLetianpaiService)
            }

            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP -> RobotModeManager.getInstance(
                mContext
            ).switchRobotMode(
                ViewModeConsts.VM_SLEEP_MODE, modeStatus
            )

            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_AUTO -> RobotModeManager.getInstance(
                mContext
            ).switchRobotMode(
                ViewModeConsts.VM_AUTO_MODE, modeStatus
            )

            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO -> RobotModeManager.getInstance(
                mContext
            ).switchRobotMode(
                ViewModeConsts.VM_DEMOSTRATE_MODE, modeStatus
            )

            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT -> //                if (ChargingUpdateCallback.getInstance().isCharging() && (!RobotModeManager.getInstance(mContext).isRobotSleepMode())) {
//                    RobotModeManager.getInstance(mContext).switchRobotMode(com.letianpai.robot.control.mode.ViewModeConsts.VM_SLEEP_MODE, 1);
//                } else {
//                    Log.e("letianpai_test_control", "switchToNewAutoPlayMode === COMMAND_VALUE_CHANGE_MODE_ROBOT ======= 4 ============");
//                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, mode_status);
//                }
                RobotFuncResponseManager.getInstance(mContext)
                    .openRobotMode(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        }
    }

    /**
     * 设置远程控制
     *
     * @param data
     */
    private fun responseDisplayViewControl(data: String) {
        XLog.i(TAG + "---responseDisplayViewControl---设置远程控制: " + data)
        val displayMode = mGson!!.fromJson(
            data,
            DisplayMode::class.java
        )
        if (displayMode?.cmd_tag == null) {
            return
        }
        val name = displayMode.cmd_tag
        var localName = ""
        if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_TIME) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_TIME
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_WEATHER) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_WEATHER
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_COUNTDOWN) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_COUNTDOWN
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_FANS) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_FANS
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_SCHEDULE) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_SCHEDULE
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_EMPTY) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_EMPTY
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_BLACK) {
            ExpressionChangeCallback.instance.showBlackView(true)
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_EXT_BLACK) {
            ExpressionChangeCallback.instance.showBlackView(false)
        }

        if (!TextUtils.isEmpty(localName)) {
            RobotModeManager.getInstance(mContext)
                .switchRobotMode(ViewModeConsts.VM_DEMOSTRATE_MODE, ViewModeConsts.VIEW_MODE_IN)
            ExpressionChangeCallback.instance.showDisplayView(localName)
        } else if (name == RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_RESET) {
            RobotModeManager.getInstance(mContext)
                .switchRobotMode(ViewModeConsts.VM_DEMOSTRATE_MODE, ViewModeConsts.VIEW_MODE_OUT)
        }
    }

    /**
     * 收到TRTC 的消息，音视频通话
     *
     * @param data
     */
    private fun responseTRTC(data: String) {
        RobotModeManager.getInstance(mContext).robotTrtcStatus = 2
        LetianpaiFunctionUtil.changeToStand(mContext, true)
        LetianpaiFunctionUtil.hideStatusBar()
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
        CommandResponseCallback.instance.setRobotStatusCmdResponse(
            AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE,
            AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE
        )
        val trtc = mGson!!.fromJson(data, TRTC::class.java)
        if (trtc != null) {
            Thread {
                try {
                    //延迟是为了释放资源，防止滋滋声
                    Thread.sleep(1500)
                    openVideos(trtc.room_id, trtc.user_id, trtc.user_sig)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }.start()
        }
        CommandResponseCallback.instance.setLTPCommand(
            MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE,
            MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE
        )
    }

    private fun openVideos(room_id: Int, user_id: String?, user_sig: String?) {
        val intent = Intent()
        intent.setComponent(
            ComponentName(
                "com.rhj.aduioandvideo",
                "com.tencent.trtc.videocall.VideoCallingActivity"
            )
        )
        intent.putExtra("roomId", room_id)
        intent.putExtra("userId", user_id)
        intent.putExtra("userSig", user_sig)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        mContext.startActivity(intent)
    }

    private fun openVideoService(room_id: Int, user_id: String?, user_sig: String?) {
        val intent = Intent()
        intent.setComponent(
            ComponentName(
                "com.rhj.aduioandvideo",
                "com.tencent.trtc.videocall.VideoService"
            )
        )
        intent.putExtra("roomId", room_id)
        intent.putExtra("userId", user_id)
        intent.putExtra("userSig", user_sig)
        mContext.startService(intent)
    }

    /**
     * 打开人脸跟随
     */
    private fun openPeopleFollow() {
        try {
            val intent = Intent()
            intent.setComponent(
                ComponentName(
                    "com.rockchip.gpadc.yolodemo",
                    "com.rockchip.gpadc.demo.MainActivity"
                )
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            mContext.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun responseFace(data: String) {
        XLog.i(TAG + "---responseFace--解析人脸: " + data)
        val robotFace = mGson!!.fromJson(
            data,
            Face::class.java
        )
        val face = robotFace.face
        if (TextUtils.isEmpty(face)) {
            return
        }
        if (face == RobotExpressionConsts.L_FACE_MAIN_IMAGE) {
        } else {
            ExpressionChangeCallback.instance.setExpression(face)
        }
    }

    companion object {
        private const val TAG = "RemoteCmdResponser"
        private var instance: RemoteCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): RemoteCmdResponser {
            synchronized(RemoteCmdResponser::class.java) {
                if (instance == null) {
                    instance = RemoteCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
