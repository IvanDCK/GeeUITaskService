package com.letianpai.robot.response.sensor

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.callback.RobotCommandWordsCallback
import com.letianpai.robot.control.callback.TemperatureUpdateCallback
import com.letianpai.robot.control.consts.AudioServiceConst
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.callback.ModeChangeCallback
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.response.RobotFuncResponseManager
import com.letianpai.robot.response.robotStatus.RobotStatusResponser
import com.letianpai.robot.response.speech.SpeechCmdResponser
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.gesturefactory.manager.GestureResPool
import com.renhejia.robot.letianpaiservice.ILetianpaiService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author liujunbin
 */
class SensorCmdResponser private constructor(private val mContext: Context) {
    private val mGson: Gson? = null
    private val gestureResPool: GestureResPool? = null
    var intent: Intent? = null
    private var preCommand: String? = null
    private var preCommandTime: Long = 0


    fun commandDistribute(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        GeeUILogUtils.logi(
            TAG,
            "commandDistribute:command: $command  data: $data"
        )

        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext)) {
            GeeUILogUtils.logi(
                TAG,
                "commandDistribute:command: LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext): " + LetianpaiFunctionUtil.isAutoAppOnTheTop(
                    mContext
                )
            )
            return
        }
        when (command) {
            RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_START_DATA -> {
                if (!RobotModeManager.getInstance(mContext).isRobotMode()) {
                    return
                }
                if (fillCurrentCmdInfo(command)) {
                    startPrecipice()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_STOP_DATA -> {
                if (!RobotModeManager.getInstance(mContext).isRobotMode()) {
                    return
                }
                if (fillCurrentCmdInfo(command)) {
                    stopPrecipice()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_FORWARD -> if (fillCurrentCmdInfo(command)) {
                if (!ChargingUpdateCallback.instance.isCharging && !RobotModeManager.getInstance(
                        mContext
                    ).isRestMode
                ) {
                    ModeChangeCallback.instance
                        .setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1)
                    fallForward()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_LEFT -> if (fillCurrentCmdInfo(command)) {
                if (!ChargingUpdateCallback.instance.isCharging && !RobotModeManager.getInstance(
                        mContext
                    ).isRestMode
                ) {
                    ModeChangeCallback.instance
                        .setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1)
                    fallLeft()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_RIGHT -> if (fillCurrentCmdInfo(command)) {
                if (!ChargingUpdateCallback.instance.isCharging && !RobotModeManager.getInstance(
                        mContext
                    ).isRestMode
                ) {
                    ModeChangeCallback.instance
                        .setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1)
                    fallRight()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_BACKEND -> if (fillCurrentCmdInfo(command)) {
                if (!ChargingUpdateCallback.instance.isCharging && !RobotModeManager.getInstance(
                        mContext
                    ).isRestMode
                ) {
                    ModeChangeCallback.instance
                        .setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1)
                    fallBackend()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_START_DATA -> if (fillCurrentCmdInfo(
                    command
                )
            ) {
                if (!ChargingUpdateCallback.instance.isCharging
                    && !RobotModeManager.getInstance(mContext).isRestMode
                    && !RobotModeManager.getInstance(mContext).isAppMode
                    && !RobotModeManager.getInstance(mContext).isRegMode
                ) {
                    startFallDown()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_STOP_DATA -> if (fillCurrentCmdInfo(
                    command
                )
            ) {
                if (!ChargingUpdateCallback.instance.isCharging
                    && !RobotModeManager.getInstance(mContext).isRestMode
                    && !RobotModeManager.getInstance(mContext).isAppMode
                    && !RobotModeManager.getInstance(mContext).isRegMode
                ) {
                    stopFallDown()
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA, RobotRemoteConsts.COMMAND_TYPE_CONTROL_DOUBLE_TAP_DATA, RobotRemoteConsts.COMMAND_TYPE_CONTROL_LONG_PRESS_DATA -> responseFunctionTap(
                iLetianpaiService,
                command,
                data
            )

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_WAGGLE -> {
                fillCurrentCmdInfo(command)
                if (!ChargingUpdateCallback.instance.isCharging && !RobotModeManager.getInstance(
                        mContext
                    ).isRestMode
                ) {
                    waggle(iLetianpaiService)
                }
            }

            RobotRemoteConsts.COMMAND_TYPE_CONTROL_TOF -> {
                fillCurrentCmdInfo(command)
                if (!ChargingUpdateCallback.instance.isCharging
                    && !RobotModeManager.getInstance(mContext).isRestMode
                    && (!LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext))
                    && (RobotModeManager.getInstance(mContext).robotMode != ViewModeConsts.VM_POWER_ON_CHARGING)
                    && !RobotModeManager.getInstance(mContext).isRegMode
                ) {
                    tofSensorHand(iLetianpaiService)
                }
            }

            else -> {}
        }
    }

    private fun fillCurrentCmdInfo(command: String?): Boolean {
        if (command == null) {
            return false
        }
        val current = System.currentTimeMillis()
        if (command == preCommand) {
            if ((current - preCommandTime) > 400) {
                preCommand = command
                preCommandTime = System.currentTimeMillis()
                return true
            } else {
                return false
            }
        } else {
            preCommand = command
            preCommandTime = System.currentTimeMillis()
            return true
        }
    }

    private fun responseFunctionTap(
        iLetianpaiService: ILetianpaiService,
        command: String,
        data: String
    ) {
        val isHigh = TemperatureUpdateCallback.instance.isInHighTemperature
        val isAlarmRunning = LetianpaiFunctionUtil.isAlarmRunning(mContext)
        val trtcStatus = RobotModeManager.getInstance(mContext).robotTrtcStatus
        GeeUILogUtils.logi(
            TAG, ("""
     responseFunctionTap: command:$command----data:$data
     --isHigh:$isHigh
     --isAlarmRunning:$isAlarmRunning
     --trtcStatus:$trtcStatus
     """.trimIndent())
        )

        if (LetianpaiFunctionUtil.isSpeechRunning(mContext) || LetianpaiFunctionUtil.isLexRunning(
                mContext
            )
        ) {
            val speechStatus: String =
                SpeechCmdResponser.getInstance(mContext).speechCurrentStatus.toString()
            //The only time you need to tap your head to switch off the speech is when there is music and dancing,
            // otherwise tapping your head is to wake up the speech.
            if (speechStatus == AudioServiceConst.ROBOT_STATUS_MUSIC) {
                //If charging, playing music, tapping your head, and just turning off speech.
                // need to bring the logic out of the original method slowly,
                // otherwise it will get messier later on
                if (ChargingUpdateCallback.instance.isCharging) {
                    // The reason for closing it twice is that the first time it closes, speech sends silence, the
                    // but Spectrum does not close the interface. It's the second time it closes that speech closes the interface.
                    // That is, speech will not close the interface until it receives the close command after it sends the slience.
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService)
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService)
                } else {
                    //Pull up the robot and then turn off speech.
                    RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService)
                }
            } else {
                RobotFuncResponseManager.closeSpeechAudioAndListen(iLetianpaiService)
            }
            return
        }

        if (isHigh) {
            GestureCallback.instance.setGestures(
                GestureCenter.highTempTTSGestureData,
                RGestureConsts.GESTURE_COMMAND_HIGH_TEMP_TTS
            )
        } else if (isAlarmRunning) {
            RobotFuncResponseManager.closeApp(iLetianpaiService)
        } else if (trtcStatus != -1) {
            return
        } else if (RobotModeManager.getInstance(mContext).isRestMode && (!ChargingUpdateCallback.instance.isCharging)) {
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
        } else if (RobotModeManager.getInstance(mContext).isCloseScreenMode && (!ChargingUpdateCallback.instance.isCharging)) {
            RobotStatusResponser.getInstance(mContext).setTapTime()
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
        } else {
            when (command) {
                RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA -> {
                    RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    tap()
                }
                RobotRemoteConsts.COMMAND_TYPE_CONTROL_DOUBLE_TAP_DATA -> {
                    RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    doubleTap()
                }
                RobotRemoteConsts.COMMAND_TYPE_CONTROL_LONG_PRESS_DATA -> {
                    RobotFuncResponseManager.stopRobot(iLetianpaiService)
                    longPressTap()
                }
            }
        }
    }

    private fun startPrecipice() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "startPrecipice")
        GestureCallback.instance.setGestures(
            GestureCenter.danglingGestureData(),
            RGestureConsts.GESTURE_ID_DANGLING_START
        )
    }

    private fun stopPrecipice() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "stopPrecipice")
        GestureCallback.instance.setGestures(
            GestureCenter.fallGroundGesture,
            RGestureConsts.GESTURE_ID_DANGLING_END
        )
    }

    /**
     * Fallen posture begins
     */
    private fun startFallDown() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "startPrecipice: Fallen posture begins.")
        GestureCallback.instance.setGestures(
            GestureCenter.fallGroundGesture,
            RGestureConsts.GESTURE_ID_FALLDOWN_START
        )
    }

    /**
     * Fallen posture ends
     */
    private fun stopFallDown() {
        hideStatusBar()
        RobotModeManager.getInstance(mContext).isInTofMode = false
        RobotModeManager.getInstance(mContext).isInCliffMode = false
        GeeUILogUtils.logi(TAG, "stopFallDown: Fallen posture ends.")
        GestureCallback.instance.setGestures(
            GestureCenter.fallGroundGesture,
            RGestureConsts.GESTURE_ID_FALLDOWN_END
        )
    }

    //-------------------------------Touch FeedbackStart
    private fun tap() {
        GeeUILogUtils.logi(TAG, "click---")
        hideStatusBar()
        RobotModeManager.getInstance(mContext).isInTofMode = false
        RobotModeManager.getInstance(mContext).isInCliffMode = false
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.tapGesture, RGestureConsts.GESTURE_ID_TAP)
    }

    private fun doubleTap() {
        GeeUILogUtils.logi(TAG, "double click---")
        hideStatusBar()
        RobotModeManager.getInstance(mContext).isInTofMode = false
        RobotModeManager.getInstance(mContext).isInCliffMode = false
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.getdoubleTapGesture(), RGestureConsts.GESTURE_ID_DOUBLE_TAP)
    }

    private fun longPressTap() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "long click---")
        RobotModeManager.getInstance(mContext).isInTofMode = false
        RobotModeManager.getInstance(mContext).isInCliffMode = false
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.longPressGesture, RGestureConsts.GESTURE_ID_LONG_PRESS)
    }

    //-------------------------------Touch Feedback end
    /**
     * Anti-fall forward
     */
    private fun fallForward() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "fallForward: anti-fall forward")
        RobotModeManager.getInstance(mContext).isInCliffMode = true
        GestureCallback.instance.setGestures(
            GestureCenter.fallForwardGestureData(),
            RGestureConsts.GESTURE_ID_CLIFF_FORWARD
        )
    }

    private fun fallBackend() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "fallBackend: anti-fall backend")
        RobotModeManager.getInstance(mContext).isInCliffMode = true
        GestureCallback.instance.setGestures(
            GestureCenter.fallBackendGestureData(),
            RGestureConsts.GESTURE_ID_CLIFF_BACKEND
        )
    }

    private fun fallLeft() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "fallLeft: anti-fall left")
        RobotModeManager.getInstance(mContext).isInCliffMode = true
        GestureCallback.instance
            .setGestures(GestureCenter.fallLeftGestureData(), RGestureConsts.GESTURE_ID_CLIFF_LEFT)
    }

    private fun fallRight() {
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "fallRight: anti-fall right")
        RobotModeManager.getInstance(mContext).isInCliffMode = true
        GestureCallback.instance.setGestures(
            GestureCenter.fallRightGestureData(),
            RGestureConsts.GESTURE_ID_CLIFF_RIGHT
        )
    }

    /**
     * jolt
     */
    private fun waggle(iLetianpaiService: ILetianpaiService) {
        RobotFuncResponseManager.stopRobot(iLetianpaiService)
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "waggle: jolt")
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.waggleGesture, RGestureConsts.GESTURE_ID_WAGGLE)
    }


    private val lock = Any() // lock object
    private val tofCount = AtomicInteger(0)
    private val startTofTime = AtomicLong(0)
    private var isIgnoreTof = false
    private var isTofTrigger = false

    private fun resetTofVariables() {
        isTofTrigger = false
        isIgnoreTof = false
        tofCount.set(0)
        startTofTime.set(0)
    }

    //这里的为临时逻辑
    // private void tofSensor(ILetianpaiService iLetianpaiService) {
    //     long currentTime = System.currentTimeMillis();
    //     synchronized (lock) {
    //         if (startTofTime.get() != 0) {
    //             long diffTime = currentTime - startTofTime.get();
    //             if (diffTime >= 1000 * 60 * 10) { // 超过10分钟后处理避障
    //                 resetTofVariables();
    //             }
    //         }
    //
    //         if (isIgnoreTof) {
    //             return;
    //         }
    //
    //         if (tofCount.get() >= 5) {
    //             resetTofVariables();
    //             LogUtils.logd(TAG, "关闭避障");
    //             // 关闭避障
    //             isIgnoreTof = true;
    //             startTofTime.set(System.currentTimeMillis());
    //         } else if (!isTofTrigger) {
    //             // 触发了避障
    //             isTofTrigger = true;
    //             // 记录开始时间
    //             startTofTime.set(System.currentTimeMillis());
    //         } else {
    //             tofCount.incrementAndGet();
    //         }
    //
    //         //处理避障
    //         tofSensorHand(iLetianpaiService);
    //     }
    // }
    private fun tofSensorHand(iLetianpaiService: ILetianpaiService) {
        RobotFuncResponseManager.stopRobot(iLetianpaiService)
        hideStatusBar()
        GeeUILogUtils.logi(TAG, "sensor tof: obstacle avoidance")

        if (RobotModeManager.getInstance(mContext).isRobotWakeupMode ||
            RobotModeManager.getInstance(mContext).isSleepMode ||
            RobotModeManager.getInstance(mContext).isRobotDeepSleepMode

        ) {
            return
        }

        val isInCliffMode = RobotModeManager.getInstance(mContext).isInCliffMode
        val isInTofMode = RobotModeManager.getInstance(mContext).isInTofMode
        if (isInCliffMode || isInTofMode) {
            GeeUILogUtils.logi(
                TAG,
                "isInCliffMode:::$isInCliffMode-----isInTofMode:::$isInTofMode"
            )
            return
        }

        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1)
        RobotModeManager.getInstance(mContext).isInTofMode = true
        GestureCallback.instance
            .setGestures(GestureCenter.tofGesture, RGestureConsts.GESTURE_ID_Tof)
    }

    private fun hideStatusBar() {
        RobotCommandWordsCallback.instance.showBattery(false)
    }


    companion object {
        private const val TAG = "SensorCmdResponser"
        private var instance: SensorCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): SensorCmdResponser {
            synchronized(SensorCmdResponser::class.java) {
                if (instance == null) {
                    instance = SensorCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
