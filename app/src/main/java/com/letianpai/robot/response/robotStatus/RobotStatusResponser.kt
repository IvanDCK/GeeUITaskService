package com.letianpai.robot.response.robotStatus

import android.content.Context
import android.os.Handler
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class RobotStatusResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    var isOtaMode: Boolean = false
        private set
    var isFactoryMode: Boolean = false
        private set
    var tapTime: Long = 0
        private set
    private val wakeUpTime: Long = 0

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService?, command: String?, data: String?) {
        XLog.i("commandDistribute----command: $command /data: $data")

        if (command == null || data == null) {
            return
        }

        when (command) {
            AppCmdConsts.COMMAND_TYPE_SET_ROBOT_MODE -> if (data == AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_IN) {
                openFactoryMode()
            } else if (data == AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_OUT) {
                closeFactoryMode()
            } else if (data == AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_IN) {
                openOTAMode()
            } else if (data == AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_OUT) {
                closeOTAMode()
            } else if (data == AppCmdConsts.COMMAND_VALUE_TO_PREVIOUS_MODE) {
                if ((LetianpaiFunctionUtil.isVideoCallRunning(mContext))
                    || (LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext))
                ) {
                    return
                }
                RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
            } else if (data == AppCmdConsts.COMMAND_VALUE_CLOCK_START) {
                GestureCallback.instance.setGestures(
                    GestureCenter.clockGestureData(),
                    RGestureConsts.GESTURE_COMMAND_CLOCK_START
                )
            } else if (data == AppCmdConsts.COMMAND_VALUE_CLOCK_STOP) {
                GestureCallback.instance.setGestures(
                    GestureCenter.closeClockGestureData(),
                    RGestureConsts.GESTURE_COMMAND_CLOCK_STOP
                )
                Handler(mContext.mainLooper).postDelayed({ //舵机卸力
                    if (RobotModeManager.getInstance(mContext).robotMode == ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT) {
                        RobotModeManager.getInstance(mContext).switchRobotMode(
                            ViewModeConsts.VM_AUDIO_WAKEUP_MODE,
                            0
                        )
                    }
                    if (RobotModeManager.getInstance(mContext).isAppMode) {
                        LetianpaiFunctionUtil.changeToStand(mContext)
                    }
                }, 2000)
            }

            else -> {}
        }
    }

    private fun closeOTAMode() {
        isOtaMode = false
    }

    private fun openOTAMode() {
        isOtaMode = true
        GestureCallback.instance.setGestures(
            GestureCenter.stopSoundEffectData(),
            RGestureConsts.GESTURE_ID_CLOSE_SOUND_EFFECT
        )
    }

    private fun closeFactoryMode() {
        isFactoryMode = false
    }

    private fun openFactoryMode() {
        isFactoryMode = true
    }

    val isFactoryMode1: Boolean
        get() = LetianpaiFunctionUtil.isFactoryOnTheTop(mContext)

    val isOtaMode1: Boolean
        get() = LetianpaiFunctionUtil.isOtaOnTheTop(mContext)

    val isNoNeedResponseMode: Boolean
        get() = if (isOtaMode1) {
            true
        } else if (isFactoryMode1) {
            true
        } else {
            false
        }

    fun setTapTime() {
        this.tapTime = System.currentTimeMillis()
    }

    companion object {
        private var instance: RobotStatusResponser? = null
        @JvmStatic
        fun getInstance(context: Context): RobotStatusResponser {
            synchronized(RobotStatusResponser::class.java) {
                if (instance == null) {
                    instance = RobotStatusResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
