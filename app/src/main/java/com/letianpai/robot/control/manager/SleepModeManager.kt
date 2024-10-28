package com.letianpai.robot.control.manager

import android.content.Context
import android.media.AudioManager
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.response.app.AppCmdResponser.Companion.getInstance
import com.letianpai.robot.response.ble.BleCmdResponser.Companion.getInstance
import com.letianpai.robot.response.identify.IdentifyCmdResponser.Companion.getInstance
import com.letianpai.robot.response.mi.MiIotCmdResponser.Companion.getInstance
import com.letianpai.robot.response.remote.RemoteCmdResponser.Companion.getInstance
import com.letianpai.robot.response.robotStatus.RobotStatusResponser.Companion.getInstance
import com.letianpai.robot.response.sensor.SensorCmdResponser.Companion.getInstance
import com.letianpai.robot.response.speech.SpeechCmdResponser.Companion.getInstance

/**
 * Sleep Mode Manager
 *
 * @author liujunbin
 */
class SleepModeManager private constructor(private val mContext: Context) {
    private var audioManager: AudioManager? = null
    private val vTag: String = "volume1111"

    init {
        init(mContext)
    }

    private fun init(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?

        //TODO Block Message Alerts
//        test();
    }

    private fun test() {
        val currentSystem: Int = audioManager!!.getStreamVolume(AudioManager.STREAM_SYSTEM)
        val currentAccessibility: Int =
            audioManager!!.getStreamVolume(AudioManager.STREAM_ACCESSIBILITY)
        val currentAlarm: Int = audioManager!!.getStreamVolume(AudioManager.STREAM_ALARM)
        val currentRing: Int = audioManager!!.getStreamVolume(AudioManager.STREAM_RING)
        val currentMusic: Int = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentVoiceCall: Int = audioManager!!.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        val currentDTMF: Int = audioManager!!.getStreamVolume(AudioManager.STREAM_DTMF)
        val currentNotification: Int =
            audioManager!!.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        GeeUILogUtils.logi(vTag, "currentSystem: $currentSystem")
        GeeUILogUtils.logi(vTag, "currentAccessibility: $currentAccessibility")
        GeeUILogUtils.logi(vTag, "currentAlarm: $currentAlarm")
        GeeUILogUtils.logi(vTag, "currentRing: $currentRing")
        GeeUILogUtils.logi(vTag, "currentMusic: $currentMusic")
        GeeUILogUtils.logi(vTag, "currentVoiceCall: $currentVoiceCall")
        GeeUILogUtils.logi(vTag, "currentDTMF: $currentDTMF")
        GeeUILogUtils.logi(vTag, "currentNotification: $currentNotification")

        //        2023-03-31 17:39:41.591  9423-9423  volume                  com.renhejia.robot.launcher          E  currentSystem: 5
//        2023-03-31 17:39:41.591  9423-9423  volume                  com.renhejia.robot.launcher          E  currentAccessibility: 13
//        2023-03-31 17:39:41.591  9423-9423  volume                  com.renhejia.robot.launcher          E  currentAlarm: 6
//        2023-03-31 17:39:41.592  9423-9423  volume                  com.renhejia.robot.launcher          E  currentRing: 5
//        2023-03-31 17:39:41.592  9423-9423  volume                  com.renhejia.robot.launcher          E  currentMusic: 13
//        2023-03-31 17:39:41.592  9423-9423  volume                  com.renhejia.robot.launcher          E  currentVoiceCall: 3
//        2023-03-31 17:39:41.592  9423-9423  volume                  com.renhejia.robot.launcher          E  currentDTMF: 13
//        2023-03-31 17:39:41.592  9423-9423  volume                  com.renhejia.robot.launcher          E  currentNotification: 5

//        audioManager.setStreamVolume(AudioManager.STREAM_ACCESSIBILITY, 5, 0);
        GeeUILogUtils.logi(vTag, "currentSystem1: $currentSystem")
        GeeUILogUtils.logi(vTag, "currentAccessibility1: $currentAccessibility")
        GeeUILogUtils.logi(vTag, "currentAlarm1: $currentAlarm")
        GeeUILogUtils.logi(vTag, "currentRing1: $currentRing")
        GeeUILogUtils.logi(vTag, "currentMusic1: $currentMusic")
        GeeUILogUtils.logi(vTag, "currentVoiceCall1: $currentVoiceCall")
        GeeUILogUtils.logi(vTag, "currentDTMF: $currentDTMF")
        GeeUILogUtils.logi(vTag, "currentNotification: $currentNotification")
    }

    val currentVolume: Int
        get() {
            val currentAccessibility: Int =
                audioManager!!.getStreamVolume(AudioManager.STREAM_ACCESSIBILITY)
            return currentAccessibility
        }

    fun setRobotVolume(volume: Int) {
        var volume: Int = volume
        if (volume > 15) {
            volume = 15
        } else if (volume < 0) {
            volume = 0
        }
        audioManager!!.setStreamVolume(AudioManager.STREAM_ACCESSIBILITY, volume, 0)
        audioManager!!.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0)
        RobotConfigManager.getInstance(mContext)!!.robotVolume = volume
        RobotConfigManager.getInstance(mContext)!!.commit()
    }

    fun volumeDown() {
        val volume: Int = currentVolume
        GeeUILogUtils.logi("letianpai_volume", "volumeDown: $volume")
        setRobotVolume(volume - 2)
    }

    fun volumeUp() {
        val volume: Int = currentVolume
        GeeUILogUtils.logi("letianpai_volume", "volumeUp: $volume")
        setRobotVolume(volume + 2)
    }

    fun setRobotVolumeTo20() {
//        setRobotVolume(20);
        setRobotVolume(20)
    }

    fun volumeMax() {
        setRobotVolume(15)
    }

    fun volumeMin() {
        setRobotVolume(5)
    }


    companion object {
        private var instance: SleepModeManager? = null
        fun getInstance(context: Context): SleepModeManager {
            synchronized(SleepModeManager::class.java) {
                if (instance == null) {
                    instance = SleepModeManager(context.getApplicationContext())
                }
                return instance!!
            }
        }
    }
}
