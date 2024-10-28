package com.letianpai.robot.control.storage

import android.content.Context
import com.letianpai.robot.control.floating.statusbar.TimeUtil

/**
 * Robotics Preference Manager
 * @author liujunbin
 */
class RobotConfigManager private constructor(private val mContext: Context?) : RobotConfigConst {
    private val mRobotSharedPreference: RobotSharedPreference = RobotSharedPreference(
        mContext,
        RobotSharedPreference.SHARE_PREFERENCE_NAME,
        RobotSharedPreference.ACTION_INTENT_CONFIG_CHANGE
    )


    private fun initKidSmartConfigState() {
    }

    fun commit(): Boolean {
        return mRobotSharedPreference.commit()
    }

    val isGeneralBatterySwitchOn: Boolean
        get() {
            var isActivated: Boolean = mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_GENERAL_BATTERY_SWITCH,
                false
            )
            isActivated = false
            return isActivated
        }

    fun setGeneralBatterySwitchStatus(generalBatterySwitchStatus: Boolean) {
        mRobotSharedPreference.putBoolean(
            RobotConfigConst.KEY_GENERAL_BATTERY_SWITCH,
            generalBatterySwitchStatus
        )
    }

    var robotVolume: Int
        get() {
            return mRobotSharedPreference.getInt(RobotConfigConst.KEY_VOLUME, 6)
        }
        set(volume) {
            var volume: Int = volume
            if (volume > 15) {
                volume = 15
            } else if (volume < 0) {
                volume = 0
            }
            mRobotSharedPreference.putInt(RobotConfigConst.KEY_VOLUME, volume)
        }

    var sleepStartTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_START_SLEEP_TIME,
                0
            )
        }
        set(time) {
            mRobotSharedPreference.putInt(RobotConfigConst.KEY_START_SLEEP_TIME, time)
        }

    var sleepEndTime: Int
        get() {
            return mRobotSharedPreference.getInt(RobotConfigConst.KEY_END_SLEEP_TIME, 0)
        }
        set(time) {
            mRobotSharedPreference.putInt(RobotConfigConst.KEY_END_SLEEP_TIME, time)
        }

    var sleepModeStatus: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_SLEEP_MODE_STATUS,
                false
            )
        }
        set(status) {
            mRobotSharedPreference.putBoolean(
                RobotConfigConst.KEY_SLEEP_MODE_STATUS,
                status
            )
        }

    var closeScreenModeSwitch: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_CLOSE_SCREEN_MODE_SWITCH,
                false
            )
        }
        set(status) {
            mRobotSharedPreference.putBoolean(
                RobotConfigConst.KEY_CLOSE_SCREEN_MODE_SWITCH,
                status
            )
        }

    var sleepSoundModeSwitch: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_SLEEP_SOUND_MODE_SWITCH,
                true
            )
        }
        set(status) {
            mRobotSharedPreference.putBoolean(
                RobotConfigConst.KEY_SLEEP_SOUND_MODE_SWITCH,
                status
            )
        }

    var sleepTimeStatusModeSwitch: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_SLEEP_TIME_STATUS_MODE_SWITCH,
                2
            )
        }
        set(status) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_SLEEP_TIME_STATUS_MODE_SWITCH,
                status
            )
        }

    val noticeTitle: String?
        get() {
            val key: String = TimeUtil.noticeKeyTime
            return mRobotSharedPreference.getString(key, null)
        }

    fun getNoticeTitle(key: String?): String {
        return mRobotSharedPreference.getString(key, null)
    }

    fun setNoticeTitle(key: String?, title: String) {
        mRobotSharedPreference.putString(key, title)
    }

    fun getRemindText(key: Int): String {
        return mRobotSharedPreference.getString(RobotConfigConst.KEY_REMIND_ + key, null)
    }

    fun setReminderText(key: Int, title: String) {
        mRobotSharedPreference.putString(RobotConfigConst.KEY_REMIND_ + key, title)
    }

    var remindWaterStartTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_REMIND_WATER_START_TIME,
                0
            )
        }
        set(time) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_REMIND_WATER_START_TIME,
                time
            )
        }

    var remindWaterEndTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_REMIND_WATER_END_TIME,
                0
            )
        }
        set(time) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_REMIND_WATER_END_TIME,
                time
            )
        }

    fun settRemindWaterSwitch(status: Boolean) {
        mRobotSharedPreference.putBoolean(
            RobotConfigConst.KEY_REMIND_WATER_SWITCH,
            status
        )
    }

    val remindWaterSwitch: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_REMIND_WATER_SWITCH,
                false
            )
        }

    var remindSedStartTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_REMIND_SIT_START_TIME,
                0
            )
        }
        ////
        set(time) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_REMIND_SIT_START_TIME,
                time
            )
        }

    var remindSedEndTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_REMIND_SIT_END_TIME,
                0
            )
        }
        set(time) {
            mRobotSharedPreference.putInt(RobotConfigConst.KEY_REMIND_SIT_END_TIME, time)
        }

    var remindSedSwitch: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_REMIND_SIT_SWITCH,
                false
            )
        }
        set(status) {
            mRobotSharedPreference.putBoolean(
                RobotConfigConst.KEY_REMIND_SIT_SWITCH,
                status
            )
        }

    var remindSiteStartTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_REMIND_SIT_POSTURE_START_TIME,
                0
            )
        }
        /////
        set(time) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_REMIND_SIT_POSTURE_START_TIME,
                time
            )
        }

    var remindSiteEndTime: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_REMIND_SIT_POSTURE_END_TIME,
                0
            )
        }
        set(time) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_REMIND_SIT_POSTURE_END_TIME,
                time
            )
        }

    var remindSiteSwitch: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_REMIND_SIT_POSTURE_SWITCH,
                false
            )
        }
        set(status) {
            mRobotSharedPreference.putBoolean(
                RobotConfigConst.KEY_REMIND_SIT_POSTURE_SWITCH,
                status
            )
        }

    var remindKeepNoticeSwitch: Boolean
        get() {
            return mRobotSharedPreference.getBoolean(
                RobotConfigConst.KEY_REMIND_KEEP_NOTICE_SWITCH,
                false
            )
        }
        set(status) {
            mRobotSharedPreference.putBoolean(
                RobotConfigConst.KEY_REMIND_KEEP_NOTICE_SWITCH,
                status
            )
        }

    var remindKeepNoticeTime: String?
        get() {
            return mRobotSharedPreference.getString(
                RobotConfigConst.KEY_REMIND_KEEP_NOTICE_TIME,
                null
            )
        }
        set(noticeTime) {
            mRobotSharedPreference.putString(
                RobotConfigConst.KEY_REMIND_KEEP_NOTICE_TIME,
                noticeTime!!
            )
        }

    var remindKeepNoticeWeek: String?
        get() {
            return mRobotSharedPreference.getString(
                RobotConfigConst.KEY_REMIND_KEEP_NOTICE_WEEK,
                null
            )
        }
        set(noticeTime) {
            mRobotSharedPreference.putString(
                RobotConfigConst.KEY_REMIND_KEEP_NOTICE_WEEK,
                noticeTime!!
            )
        }

    var updateTime: Long
        get() {
            return mRobotSharedPreference.getLong(
                RobotConfigConst.KEY_UPDATE_ROBOT_TIME,
                0L
            )
        }
        set(time) {
            mRobotSharedPreference.putLong(RobotConfigConst.KEY_UPDATE_ROBOT_TIME, time)
        }

    val automaticRechargeSwitch: Boolean
        get() {
            return (mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_AUTOMATIC_RECHARGE_SWITCH,
                0
            ) == 1)
        }

    fun setAutomaticRechargeSwitch(switchStatus: Int) {
        mRobotSharedPreference.putInt(
            RobotConfigConst.KEY_AUTOMATIC_RECHARGE_SWITCH,
            switchStatus
        )
    }

    var automaticRechargeVal: Int
        get() {
            return mRobotSharedPreference.getInt(
                RobotConfigConst.KEY_AUTOMATIC_RECHARGE_VAL,
                50
            )
        }
        set(rechargeVal) {
            mRobotSharedPreference.putInt(
                RobotConfigConst.KEY_AUTOMATIC_RECHARGE_VAL,
                rechargeVal
            )
        }


    companion object {
        private var mRobotConfigManager: RobotConfigManager? = null
        fun getInstance(context: Context?): RobotConfigManager? {
            if (mRobotConfigManager == null) {
                mRobotConfigManager = RobotConfigManager(context)
                mRobotConfigManager!!.initKidSmartConfigState()
                mRobotConfigManager!!.commit()
            }
            return mRobotConfigManager
        }
    }
}
