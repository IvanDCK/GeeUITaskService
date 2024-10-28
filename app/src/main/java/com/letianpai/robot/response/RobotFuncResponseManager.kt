package com.letianpai.robot.response

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.util.Log
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.renhejia.robot.commandlib.consts.PackageConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.consts.SpeechConst
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class RobotFuncResponseManager private constructor(private var mContext: Context) {
    init {
        init(mContext)
    }

    private fun init(context: Context) {
        this.mContext = context
    }

    /**
     * @param openType
     */
    fun openCommemoration(openType: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_COMMEMORATION)
    }

    /**
     * @param openType
     */
    fun openPeopleSearch(openType: String?) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_BODY_REG_MODE, 1)
        val intent = Intent()
        val cn = ComponentName(PackageConsts.PACKAGE_NAME_IDENT, PackageConsts.SERVICE_NAME_IDENT)
        intent.setComponent(cn)
        mContext.startService(intent)
    }

    /**
     * @param openType
     */
    fun closePeopleSearch(openType: String?) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_BODY_REG_MODE, 0)
        val intent = Intent()
        val cn = ComponentName(PackageConsts.PACKAGE_NAME_IDENT, PackageConsts.SERVICE_NAME_IDENT)
        intent.setComponent(cn)
        mContext.stopService(intent)
    }

    /**
     * 打开机器人模式
     *
     * @param openTypeSpeech
     */
    fun openRobotMode(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1)
        if (ChargingUpdateCallback.instance.isCharging) {
            try {
                LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false)
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            LetianpaiFunctionUtil.responseCharging(mContext)
        }
    }

    /**
     * 打开机器人模式
     *
     * @param openTypeSpeech
     */
    fun closeRobotMode(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 0)
    }

    fun openWeather(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_WEATHER)
    }

    fun openSleepMode(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1)
    }

    fun closeSleepMode(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 0)
    }

    fun openEventCountdown(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_EVENT_COUNTDOWN)
    }

    fun openNews(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_NEWS)
    }

    fun openMessage(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_MESSAGE)
    }

    fun openStock(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_STOCK)
    }

    fun openCustom(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_CUSTOM)
    }

    fun openSwitchApp(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext).switchToPreviousAppMode()
    }

    fun openWord(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_WORD)
    }

    fun openLamp(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_LAMP)
    }

    fun openTime(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_TIME)
    }

    fun openFans(openTypeSpeech: String?) {
        RobotModeManager.getInstance(mContext)
            .switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_FANS)
    }

    fun openPetsMode(openTypeSpeech: String?) {
        openRobotMode(openTypeSpeech)
    }

    fun openUpgrade(openTypeSpeech: String?) {
        LetianpaiFunctionUtil.startGeeUIOtaService(mContext)
    }

    companion object {
        private var instance: RobotFuncResponseManager? = null
        const val OPEN_TYPE_SPEECH: String = "voice"
        const val OPEN_TYPE_REMOTE: String = "remote"


        fun getInstance(context: Context): RobotFuncResponseManager {
            synchronized(RobotModeManager::class.java) {
                if (instance == null) {
                    instance = RobotFuncResponseManager(context.applicationContext)
                }
                return instance!!
            }
        }


        fun closeSpeechAudioAndListen(iLetianpaiService: ILetianpaiService) {
            Log.i("<<<", "closeSpeechAudioAndListen")
            try {
                iLetianpaiService.setTTS(
                    SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO_AND_LISTENING,
                    SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun closeSpeechAudio(iLetianpaiService: ILetianpaiService) {
            Log.d("<<<", "closeSpeechAudio")
            try {
                iLetianpaiService.setTTS(
                    SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO,
                    SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun closeApp(iLetianpaiService: ILetianpaiService) {
            try {
                iLetianpaiService.setAppCmd(
                    RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP,
                    RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun stopRobot(iLetianpaiService: ILetianpaiService) {
            try {
                iLetianpaiService.setAppCmd(
                    PackageConsts.ROBOT_PACKAGE_NAME,
                    RobotRemoteConsts.COMMAND_VALUE_EXIT
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun responseSensorEvent(iLetianpaiService: ILetianpaiService) {
            try {
                iLetianpaiService.setAppCmd(
                    PackageConsts.ROBOT_PACKAGE_NAME,
                    RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SENSOR
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
