package com.letianpai.robot.control.manager

import android.content.Context
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.callback.RobotCommandWordsCallback
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.callback.ModeChangeCallback
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.response.robotStatus.RobotStatusResponser
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_BODY
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_FACE
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_HAND
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * 模式管理
 *
 * @author liujunbin
 */
class RobotModeManager private constructor(private var mContext: Context) : ViewModeConsts {
    private var robotModeBeforeSleepMode: Int = 0
    private var robotModeBeforeCharging: Int = 0
    private var isRobotModeBeforeCharging: Boolean = false
    private var robotTempStatus: Int = ViewModeConsts.VM_STATIC_MODE // There is no demo mode for
    private var robotMiddleTempStatus: Int = 0 //

    var robotMode: Int = ViewModeConsts.VM_STATIC_MODE


    /**
     * The current mode status of the robot, which may have overlapping values with robotAppModeStatus.
     * The main thing to do is to distinguish between entering and exiting the current mode.
     */
    var robotModeStatus: Int = 0


    /**
     * State only in static mode, the state is used to distinguish between individual apps
     */
    var robotAppModeStatus: Int = 0
        private set


    /**
     * 1, for remote monitoring
     * 2, for video call
     * 3, represents remote voice change
     * 4, for demo mode
     */
    var robotTrtcStatus: Int = -1

    var isAlarming: Boolean = false
    var modeList: ArrayList<String?> = ArrayList()
    var isShowChargingInRobotMode: Boolean = false
        private set

    //Are robots going over the cliff
    var isInTofMode: Boolean = false
    var isInCliffMode: Boolean = false
    private var iLetianpaiService: ILetianpaiService? = null
    fun setiLetianpaiService(iLetianpaiService: ILetianpaiService?) {
        this.iLetianpaiService = iLetianpaiService
    }

    fun resetStatus() {
        robotTempStatus = ViewModeConsts.VM_STATIC_MODE
        robotMiddleTempStatus = 0
        robotMode = ViewModeConsts.VM_UNBIND_DEVICE
        robotModeStatus = 0
        robotAppModeStatus = 0
        robotTrtcStatus = -1
    }

    private fun init(context: Context) {
        this.mContext = context
    }

    fun resetTrtcStatus() {
        robotTrtcStatus = -1
    }

    val isRobotWakeupMode: Boolean
        /**
         * @return
         */
        get() {
            if (robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE || robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT) {
                return true
            } else {
                return false
            }
        }

    val isRobotDeepSleepMode: Boolean
        /**
         * @return
         */
        get() {
            GeeUILogUtils.logi(
                TAG,
                "---isRobotDeepSleepMode ---- robotMode: $robotMode"
            )
            if (robotMode == ViewModeConsts.VM_DEEP_SLEEP_MODE) {
                return true
            } else {
                return false
            }
        }

    val isSleepMode: Boolean
        /**
         * @return
         */
        get() {
            if (robotMode == ViewModeConsts.VM_SLEEP_MODE) {
                return true
            } else {
                return false
            }
        }

    val isRestMode: Boolean
        /**
         * @return
         */
        get() {
            return when (robotMode) {
                ViewModeConsts.VM_SLEEP_MODE -> {
                    true
                }
                ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE -> {
                    true
                }
                ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE -> {
                    true
                }
                else -> {
                    false
                }
            }
        }

    val isCloseScreenMode: Boolean
        /**
         * @return
         */
        get() {
            return robotMode == ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE
        }

    val isPowerOnChargingMode: Boolean
        /**
         * @return
         */
        get() {
            return robotMode == ViewModeConsts.VM_POWER_ON_CHARGING
        }

    val isAppMode: Boolean
        /**
         * @return
         */
        get() {
            return robotMode == ViewModeConsts.VM_STATIC_MODE
        }

    val isRobotSleepMode: Boolean
        /**
         * @return
         */
        get() {
            return robotMode == ViewModeConsts.VM_SLEEP_MODE
        }

    /**
     * @return
     */
    fun isRobotMode(): Boolean {
        return robotMode == ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
    }

    val isCommonRobotMode: Boolean
        /**
         * @return
         */
        get() {
            GeeUILogUtils.logi("letianpai_robot_status", "---robotMode: $robotMode")
            return robotMode == ViewModeConsts.VM_AUTO_NEW_PLAY_MODE || robotMode == ViewModeConsts.VM_DEMOSTRATE_MODE || robotMode == ViewModeConsts.VM_REMOTE_CONTROL_MODE || robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE || robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT || robotMode == ViewModeConsts.VM_HAND_REG_MODE || robotMode == ViewModeConsts.VM_TAKE_PHOTO
        }

    //After the app opens successfully, go back to record mode
    fun switchRobotMode(mode: Int, status: Int) {
        GeeUILogUtils.logi(TAG, "----switchRobotMode: $mode | Status: $status")
        //Factory mode or ota is not allowed to be switched.
        if (RobotStatusResponser.getInstance(mContext).isNoNeedResponseMode) {
            GeeUILogUtils.logi(
                TAG,
                "----switchRobotMode: " + RobotStatusResponser.getInstance(mContext).isNoNeedResponseMode
            )
            return
        }
        //Does not respond if the device is unbound
        if (mode == ViewModeConsts.VM_UNBIND_DEVICE) {
            return
        }
        //Record mode entry and exit, currently only used in charging response wake-up
        // related to the place LetianpaiFunctionUtil.responseCharging inside the
        robotModeStatus = status

        when (mode) {
            ViewModeConsts.VM_STANDBY_MODE -> switchToStandbyMode()
            ViewModeConsts.VM_STATIC_MODE -> {
                this.robotTempStatus = ViewModeConsts.VM_STATIC_MODE
                switchToDisplayMode(status)
            }

            ViewModeConsts.VM_CHARGING_MODE -> switchToChargingMode()
            ViewModeConsts.VM_SLEEP_MODE -> if (status == 1) {
                gotoSleep()
            } else {
                switchToPreviousPlayMode()
            }

            ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE -> if (status == 1) {
                gotoBlackScreenSleep()
            } else {
                switchToPreviousPlayMode()
            }

            ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE -> if (status == 1) {
                gotoNightBlackScreenSleep()
            } else {
                switchToPreviousPlayMode()
            }

            ViewModeConsts.VM_DEEP_SLEEP_MODE -> gotoDeepSleepMode(status)
            ViewModeConsts.VM_FUNCTION_MODE -> switchToFunctionMode()
            ViewModeConsts.VM_REMOTE_CONTROL_MODE -> switchToRemoteControlMode(status)
            ViewModeConsts.VM_DEMOSTRATE_MODE -> switchToDemonstrateMode(mode, status)

            ViewModeConsts.VM_AUTO_PLAY_MODE -> switchToAutoPlayMode()
            ViewModeConsts.VM_AUTO_NEW_PLAY_MODE -> {
                //                if (isRestMode() && ChargingUpdateCallback.instance.isCharging()) {
                LetianpaiFunctionUtil.updateModeStatusOnServer(
                    mContext,
                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT
                )
                if (ChargingUpdateCallback.instance.isCharging) {
                    this.robotTempStatus = ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
                    this.robotMode = ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
                    return
                }
                switchToNewAutoPlayMode()
            }

            ViewModeConsts.VM_AUDIO_WAKEUP_MODE -> switchAudioWakeupMode(status)
            ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT -> {
                screenOn()
                this.robotMode = ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT
            }

            ViewModeConsts.VM_HAND_REG_MODE -> openHandReg(status)
            ViewModeConsts.VM_BODY_REG_MODE -> openBodyReg(status)
            ViewModeConsts.VM_FACE_REG_MODE -> openFaceReg(status)
            ViewModeConsts.VM_GESTURE ->                 //There is no need to do anything for the time being, it should stop the interaction between left and right.
                this.robotMode = ViewModeConsts.VM_GESTURE

            ViewModeConsts.VM_MI_IOT_MODE -> this.robotMode =
                ViewModeConsts.VM_MI_IOT_MODE

            ViewModeConsts.VM_EMOTION -> if (status == 1) {
                this.robotMode = ViewModeConsts.VM_EMOTION
            }

            ViewModeConsts.VM_TAKE_PHOTO -> openTakePhoto(status)
            ViewModeConsts.VM_POWER_ON_CHARGING -> {
                if (status == 1) {
                    this.robotMode = ViewModeConsts.VM_POWER_ON_CHARGING
                }
                responsePowerCharging()
            }

            ViewModeConsts.VM_AUTO_CHARGING -> if (status == 1) {
                this.robotMode = ViewModeConsts.VM_AUTO_CHARGING
                LetianpaiFunctionUtil.openAutoCharging(mContext)
            }
        }
    }

    private fun responsePowerCharging() {
        //If charging go to robot moving posture, if not charging go directly to static mode
        if (ChargingUpdateCallback.instance.isCharging) {
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, false)
            GestureCallback.instance.setGestures(
                GestureCenter.powerOnChargingGestureData(),
                RGestureConsts.GESTURE_POWER_ON_CHARGING
            )
        } else {
            switchToPreviousAppMode()
        }
    }

    /**
     *
     */
    private fun gotoDeepSleepMode(status: Int) {
        GeeUILogUtils.logi(
            "letianpai_deep_sleep",
            " =================================== gotoDeepSleepMode ==== 0 ===================================gstatus：$status"
        )
        if (status == 1) {
            GeeUILogUtils.logi(
                "letianpai_deep_sleep",
                "controlSteeringEngine_========================== gotoDeepSleepMode ==== 1 =========================="
            )
            //            LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false);
            LetianpaiFunctionUtil.changeToStand(mContext)
            //            LetianpaiFunctionUtil.openTimeViewForSleep1(mContext);
            LetianpaiFunctionUtil.openTimeViewForDeepSleep(mContext)

            if (RobotConfigManager.getInstance(mContext)!!.sleepSoundModeSwitch) {
                GestureCallback.instance.setGestures(
                    GestureCenter.goToHibernationGesture1(),
                    RGestureConsts.GESTURE_COMMAND_HIBERNATION
                )
            }
            //            GestureCallback.instance.setGestures(GestureCenter.goToHibernationGesture1(), RGestureConsts.GESTURE_COMMAND_HIBERNATION);
            //TODO Open the animation interface of Launcher
            CommandResponseCallback.instance.setRobotStatusCmdResponse(
                AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE
            )
            ModeChangeCallback.instance
                .setModeChange(ViewModeConsts.VM_DEEP_SLEEP_MODE, 1)
            GeeUILogUtils.logi(
                "letianpai_deep_sleep",
                " =================================== gotoDeepSleepMode ==== 3 =================================== "
            )
            //            SleepModeManager.getInstance(mContext).setRobotVolume(5);
            this.robotMode = ViewModeConsts.VM_DEEP_SLEEP_MODE
        } else {
            GeeUILogUtils.logi(
                "letianpai_deep_sleep",
                " =================================== gotoDeepSleepMode ==== 4 =================================== "
            )
            //            RobotModeManager.getInstance(mContext).switchToPreviousRobotMode();
            getInstance(mContext).switchToPreviousPlayMode()
            GestureCallback.instance.setGestures(
                GestureCenter.stopHibernationGesture(),
                RGestureConsts.GESTURE_COMMAND_EXIT_HIBERNATION
            )
            CommandResponseCallback.instance.setRobotStatusCmdResponse(
                AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE
            )
            //            SleepModeManager.getInstance(mContext).setRobotVolume(RobotConfigManager.getInstance(mContext).getRobotVolume());
        }
    }

    private fun gotoSleep() {
        GeeUILogUtils.logi("letianpai_birthday", "---robotMode: " + robotMode)

        this.robotModeBeforeSleepMode = robotMode
        this.robotMode = ViewModeConsts.VM_SLEEP_MODE
        RobotCommandWordsCallback.instance.showBattery(false)
        LetianpaiFunctionUtil.openTimeViewForSleep(mContext)
        // ModeChangeCallback.instance.setModeChange(ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS, 1);
        //Play sleep music
        GestureCallback.instance.setGestures(
            GestureCenter.goToHibernationGesture1(),
            RGestureConsts.GESTURE_COMMAND_HIBERNATION
        )
    }

    private fun gotoBlackScreenSleep() {
        GeeUILogUtils.logi(
            "letianpai_sleep_test_gotoBlackScreenSleep",
            "---robotMode: $robotMode"
        )
        gotoScreenSleep(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE)
    }

    private fun gotoScreenSleep(mode: Int) {
        GeeUILogUtils.logi("letianpai_sleep_test_repeat", "=========== 1 =======")
        GeeUILogUtils.logi(
            "letianpai_sleep_test_letianpai_birthday",
            "---robotMode ==== 1 ====: $robotMode"
        )
        //        if (robotMode == mode) {
//            Log.e("letianpai_sleep_test_letianpai_birthday", "robotMode ==== 2 ====: " + robotMode);
//            return;
//        }
        GeeUILogUtils.logi(
            "letianpai_sleep_test_letianpai_birthday",
            "---robotMode ==== 3 ====: $robotMode"
        )
        this.robotModeBeforeSleepMode = robotMode
        this.robotMode = mode
        //        RobotCommandWordsCallback.instance.showBattery(false);
        LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false)
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
        SystemFunctionUtil.goToSleep(mContext)
    }

    private fun gotoNightBlackScreenSleep() {
        GeeUILogUtils.logi("letianpai_birthday", "---gotoNightBlackScreenSleep: ======")
        gotoScreenSleep(ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE)
    }


    private fun openHandReg(status: Int) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 2 ============== ")
        screenOn()
        if (status == 1) {
//            ModeChangeCallback.instance.setModeChange(VM_HAND_REG_MODE,1);
            LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_HAND, "h0025")
            GeeUILogUtils.logi(
                "letianpai_control",
                "---controlSteeringEngine_========================== 4 =========================="
            )
            LetianpaiFunctionUtil.controlSteeringEngine(mContext,
                footSwitch = true,
                sensorSwitch = true
            )
            //TODO Add a gesture
//            GestureCallback.instance.setGesture(GestureConsts.GESTURE_HAND_RECOGNITION);
            this.robotMode = ViewModeConsts.VM_HAND_REG_MODE
        } else if (status == 2) { //Open the ai related activity and go to the gesture mode
            LetianpaiFunctionUtil.controlSteeringEngine(mContext,
                footSwitch = true,
                sensorSwitch = true
            )
            this.robotMode = ViewModeConsts.VM_HAND_REG_MODE
        } else {
            switchToPreviousPlayMode()
        }
    }

    private fun openBodyReg(status: Int) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 3 ============== ")
        screenOn()
        if (status == 1) {
            LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_BODY, "h0025")
            GeeUILogUtils.logi(
                "letianpai_control",
                "---controlSteeringEngine_========================== 4 =========================="
            )
            LetianpaiFunctionUtil.controlSteeringEngine(mContext,
                footSwitch = true,
                sensorSwitch = true
            )
            //TODO Add a gesture
//            GestureCallback.instance.setGesture(GestureConsts.GESTURE_HAND_RECOGNITION);
            this.robotMode = ViewModeConsts.VM_BODY_REG_MODE
        } else {
            switchToPreviousPlayMode()
        }
    }

    private fun openFaceReg(status: Int) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 4 ============== ")
        screenOn()
        if (status == 1) {
            LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_FACE, "h0025")
            GeeUILogUtils.logi(
                "letianpai_control",
                "---controlSteeringEngine_========================== 4 =========================="
            )
            LetianpaiFunctionUtil.controlSteeringEngine(mContext,
                footSwitch = true,
                sensorSwitch = true
            )
            //TODO Add a gesture
//            GestureCallback.instance.setGesture(GestureConsts.GESTURE_HAND_RECOGNITION);
            this.robotMode = ViewModeConsts.VM_FACE_REG_MODE
        } else {
            switchToPreviousPlayMode()
        }
    }

    private fun openTakePhoto(status: Int) {
        screenOn()
        if (status == 1) {
            if (robotMode == ViewModeConsts.VM_TAKE_PHOTO) {
                return
            }
            if (LetianpaiFunctionUtil.openTakePhoto(mContext)) {
                this.robotMode = ViewModeConsts.VM_TAKE_PHOTO
            }
            // LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_TAKE_PHOTO, "h0182");
            // LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            // Intent intent = new Intent();
            // intent.setComponent(new ComponentName("com.ltp.ident", "com.ltp.ident.services.TakePhotoService"));
            // mContext.startService(intent);
            // this.robotMode = VM_TAKE_PHOTO;
        } else {
            //如果是other模式，关闭speech
            GeeUILogUtils.logi(TAG, "openTakePhoto: robotAppModeStatus: $robotAppModeStatus")
            if (robotAppModeStatus == ViewModeConsts.APP_MODE_OTHER) {
                ModeChangeCallback.instance
                    .setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1)
            }
            switchToPreviousPlayMode()
        }
    }

    private var preTime: Long = 0

    init {
        init(mContext)
    }

    fun switchToPreviousPlayMode() {
        //防止不知道那里的逻辑过快切换导致的问题
        val currentTime: Long = System.currentTimeMillis()
        if (currentTime - preTime < 2000) return
        preTime = currentTime
        val isCharging: Boolean = ChargingUpdateCallback.instance.isCharging
        GeeUILogUtils.logi(
            TAG, "---switchToPreviousPlayMode\n" +
                    "--:isCharging:" + isCharging +
                    "\n---:robotMiddleTempStatus:" + robotMiddleTempStatus +
                    "\n---:robotTempStatus:" + robotTempStatus +
                    "\n---:robotModeStatus:" + robotModeStatus +
                    "\n---:robotAppModeStatus:" + robotAppModeStatus
        )
        if (isCharging) {
            LetianpaiFunctionUtil.responseCharging(mContext)
        } else {
            if (robotMiddleTempStatus != 0) {
                switchRobotMode(robotMiddleTempStatus, 1)
            } else {
                if (robotTempStatus == ViewModeConsts.VM_STATIC_MODE) {
                    switchRobotMode(ViewModeConsts.VM_STATIC_MODE, robotAppModeStatus)
                    ModeChangeCallback.instance
                        .setModeChange(ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS, 1)
                } else {
                    switchRobotMode(robotTempStatus, 1)
                }
            }
        }
    }

    val isPreviousModeIsAppMode: Boolean
        get() {
            return if (robotMiddleTempStatus != 0) {
                false
            } else {
                robotTempStatus == ViewModeConsts.VM_STATIC_MODE
            }
        }

    val isPreviousModeIsRobotMode: Boolean
        get() {
            return robotTempStatus == ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
        }


    // public void switchToPreviousPlayModeNew() {
    //     Log.e("letianpai_sleep_test", " =================================== letianpai_sleep_test ==== 1 =================================== ");
    //
    //     Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 1 =================================== robotMiddleTempStatus: " + robotMiddleTempStatus);
    //     if (robotMiddleTempStatus != 0) {
    //         Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 2 =================================== robotMiddleTempStatus: " + robotMiddleTempStatus);
    //         if (ChargingUpdateCallback.instance.isCharging()) {
    //             LetianpaiFunctionUtil.responseCharging(mContext);
    //         } else {
    //             switchRobotMode(robotMiddleTempStatus, 1);
    //         }
    //
    //     } else {
    //         Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 3 =================================== robotMiddleTempStatus: " + robotMiddleTempStatus);
    //         if (robotTempStatus == VM_STATIC_MODE) {
    //             Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 4 =================================== robotTempStatus: " + robotTempStatus);
    //             Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 5 =================================== robotAppModeStatus: " + robotAppModeStatus);
    //             switchRobotMode(VM_STATIC_MODE, robotAppModeStatus);
    //             // TODO killRobot
    //             ModeChangeCallback.instance.setModeChange(ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS, 1);
    //
    //         } else {
    //             if (ChargingUpdateCallback.instance.isCharging()) {
    //                 LetianpaiFunctionUtil.responseCharging(mContext);
    //             } else {
    //                 Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 6 =================================== robotTempStatus: " + robotTempStatus);
    //                 switchRobotMode(robotTempStatus, 1);
    //             }
    //
    //         }
    //     }
    //
    // }
    fun switchToPreviousAppMode() {
        RobotCommandWordsCallback.instance.showBattery(true)
        switchRobotMode(ViewModeConsts.VM_STATIC_MODE, robotAppModeStatus)
    }

    private fun switchToAutoPlayMode() {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 7 ============== ")
        screenOn()
        //        GestureCallback.instance.setGesture(GestureConsts.GESTURE_TEST0);
    }

    /**
     * 进入展示模式
     */
    private fun switchToNewAutoPlayMode() {
        RobotCommandWordsCallback.instance.showBattery(false)
        robotMiddleTempStatus = ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
        this.robotMode = ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
        ModeChangeCallback.instance
            .setModeChange(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1)
        screenOn()
        // TODO Launch Robot Mode App
//        CommandResponseCallback.instance.setLTPCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,new PowerMotion(3,1).toString()));
        LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true)
        LetianpaiFunctionUtil.openRobotMode(
            mContext,
            RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT,
            null
        )
        this.robotTempStatus = ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
    }

    /**
     *
     * stop automation
     */
    private fun stopAutoPlayMode() {
        stopRobotDispatchService()
    }

    private fun startRobotDispatchService() {
        // TODO Launch Robot Mode App
//        Intent intent = new Intent(mContext, RobotDispatchService.class);
//        mContext.startService(intent);
    }

    private fun stopRobotDispatchService() {
        //TODO Discontinuation of robotic services
    }

    private fun screenOn() {
        SystemFunctionUtil.wakeUp(mContext)
    }

    private fun switchToChargingMode() {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 11 ============== ")
        screenOn()
    }

    fun restoreRobotAppModeStatus() {
        robotAppModeStatus = 0
    }

    fun switchToDisplayMode(status: Int) {
        GeeUILogUtils.logi("RobotModeManager", "---switchToDisplayMode  status:: $status")
        screenOn()

        if (status == 0 || status == ViewModeConsts.APP_MODE_TIME) {
            //This status value should only be recorded if the app has been opened successfully
            syncRobotStatus(status)
            LetianpaiFunctionUtil.openTime(mContext)
        } else if (status == ViewModeConsts.APP_MODE_WEATHER) {
            if (LetianpaiFunctionUtil.openWeather(mContext)) {
                //This status value should only be recorded if the app has been opened successfully
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_EVENT_COUNTDOWN) {
            if (LetianpaiFunctionUtil.openEventCountdown(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_FANS) {
            if (LetianpaiFunctionUtil.openFans(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_MESSAGE) {
            if (LetianpaiFunctionUtil.openMessage(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_NEWS) {
            if (LetianpaiFunctionUtil.openNews(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_CUSTOM) {
            if (LetianpaiFunctionUtil.openCustom(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_COMMEMORATION) {
            if (LetianpaiFunctionUtil.openCommemoration(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_STOCK) {
            if (LetianpaiFunctionUtil.openStock(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_WORD) {
            if (LetianpaiFunctionUtil.openWord(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_LAMP) {
            if (LetianpaiFunctionUtil.openLamp(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_EXPRESSION) {
            if (LetianpaiFunctionUtil.openExpressionApp(mContext)) {
                syncRobotStatus(status)
            }
        } else if (status == ViewModeConsts.APP_MODE_OTHER) {
            LetianpaiFunctionUtil.resetOtherAppStatus(mContext)
            syncRobotStatus(status)
        } else if (status == ViewModeConsts.APP_MODE_MIJIA) {
            //Mijia doesn't need to modify the app status
            LetianpaiFunctionUtil.openBindMijia(mContext)
        }

        if (ChargingUpdateCallback.instance.isCharging) {
            getInstance(mContext).setRobotModeBeforeCharging()
        }
    }

    //Synchronising the various states of the robot
    private fun syncRobotStatus(status: Int) {
        robotAppModeStatus = status
        robotMiddleTempStatus = 0
        robotTempStatus = ViewModeConsts.VM_STATIC_MODE
        robotMode = ViewModeConsts.VM_STATIC_MODE
    }

    fun ttsUninstallAppText() {
        //Uninstalled
        val text = if (SystemFunctionUtil.isChinese) {
            "The app is not installed"
        } else {
            "The app is not installed"
        }
        ttsContent(text)
    }

    private fun ttsContent(ttsText: String) {
        try {
            if (iLetianpaiService != null) {
                iLetianpaiService!!.setTTS("speakText", ttsText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun switchToStandbyMode() {
//        ExpressionChangeCallback.instance.showMainImage();
    }

    private fun switchToFunctionMode() {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 14 ============== ")
        screenOn()
    }

    /**
     * 切换到语音唤醒
     */
    private fun switchAudioWakeupMode(modeStatus: Int) {
        GeeUILogUtils.logi(
            "RobotModeManager",
            "---switchAudioWakeupMode:: $modeStatus ---isRegMode()-- $isRegMode"
        )
        screenOn()
        if (modeStatus == 0) {
            if (!isRegMode) {
                switchToPreviousPlayMode()
            }
        } else if (modeStatus == 1) {
            this.robotMode = ViewModeConsts.VM_AUDIO_WAKEUP_MODE
            ExpressionChangeCallback.instance.showDisplayView(null)
            GeeUILogUtils.logi(
                "uiTaskService",
                "---switchAudioWakeupMode ======= 1 ======= : VM_AUDIO_WAKEUP_MODE $robotMode"
            )
        } else if (modeStatus == 2) {
            this.robotMode = ViewModeConsts.VM_AUDIO_WAKEUP_MODE
            GeeUILogUtils.logi(
                "uiTaskService",
                "---switchAudioWakeupMode ======= 1 ======= : VM_AUDIO_WAKEUP_MODE $robotMode"
            )
        }
    }

    val isRegMode: Boolean
        get() {
            if (this.robotMode == ViewModeConsts.VM_HAND_REG_MODE || this.robotMode == ViewModeConsts.VM_BODY_REG_MODE || this.robotMode == ViewModeConsts.VM_FACE_REG_MODE) {
                return true
            }
            return false
        }

    /**
     * 切换至演示模式
     */
    private fun switchToDemonstrateMode(mode: Int, modeStatus: Int) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "----screenOn  ========== 16 ============== ")
        screenOn()
        if (modeStatus == 1) {
            getInstance(mContext).robotTrtcStatus = 4
            RobotCommandWordsCallback.instance.showBattery(false)
            // robotMiddleTempStatus = VM_DEMOSTRATE_MODE;
            LetianpaiFunctionUtil.openRobotMode(mContext, "demo", null)
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true)
            //Turn off wake on speech
            CommandResponseCallback.instance.setRobotStatusCmdResponse(
                AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE
            )
            this.robotMode = mode
        } else {
            getInstance(mContext).resetTrtcStatus()
            RobotCommandWordsCallback.instance.showBattery(true)
            //Wake on speech
            CommandResponseCallback.instance.setRobotStatusCmdResponse(
                AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE
            )
            // robotMiddleTempStatus = 0;
            switchToPreviousPlayMode()
        }
    }


    private fun switchToRemoteControlMode(status: Int) {
        if (status == 1) {
            RobotCommandWordsCallback.instance.showBattery(false)
            robotMiddleTempStatus = ViewModeConsts.VM_REMOTE_CONTROL_MODE
            ModeChangeCallback.instance
                .setModeChange(ViewModeConsts.VM_REMOTE_CONTROL_MODE, 1)
            LetianpaiFunctionUtil.openRobotMode(mContext, "demo", null)
            GeeUILogUtils.logi(
                "letianpai_control",
                "---controlSteeringEngine_========================== 9 =========================="
            )
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true)
            this.robotMode = ViewModeConsts.VM_REMOTE_CONTROL_MODE
        } else {
            RobotCommandWordsCallback.instance.showBattery(true)
            robotMiddleTempStatus = 0
            //TODO Return to the previous normal operation
            switchToPreviousPlayMode()
        }
    }


    fun setRobotModeBeforeSleepMode(robotModeBeforeSleepMode: Int) {
        this.robotModeBeforeSleepMode = robotModeBeforeSleepMode
    }

    fun setRobotModeBeforeCharging() {
        this.robotModeBeforeCharging = robotMode
        //        this.robotModeBeforeCharging = robotTempStatus;
    }

    fun setRobotModeBeforeChargingOn(isRobotMode: Boolean) {
        this.isRobotModeBeforeCharging = isRobotMode
        //        this.robotModeBeforeCharging = robotTempStatus;
    }

    fun isRobotModeBeforeCharging(): Boolean {
        return isRobotModeBeforeCharging
    }

    val robotModeBeforeChargingIsRobot: Boolean
        //    public int getRobotModeBeforeCharging() {
        get() {
            return robotModeBeforeCharging == ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
        }

    fun setShowChargingIconInRobotMode(showChargingIconInRobotMode: Boolean) {
        isShowChargingInRobotMode = showChargingIconInRobotMode
    }

    val isNoExpressionMode: Boolean
        get() {
            if (LetianpaiFunctionUtil.isVideoCallOnTheTop(mContext) || LetianpaiFunctionUtil.isVideoCallServiceRunning(
                    mContext
                )
                || (LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext))
            ) {
                return true
            }
            return false
        }

    fun setToRobotMode() {
        robotMode =
            ViewModeConsts.VM_AUTO_NEW_PLAY_MODE
    }

    companion object {
        private val TAG: String = "RobotModeManager"
        private var instance: RobotModeManager? = null
        fun getInstance(context: Context): RobotModeManager {
            synchronized(RobotModeManager::class.java) {
                if (instance == null) {
                    instance = RobotModeManager(context.getApplicationContext())
                }
                return instance!!
            }
        }
    }
}
