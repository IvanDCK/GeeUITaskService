package com.letianpai.robot.alarm.receiver

/**
 * Created by liujunbin
 */
class AlarmCallback private constructor() {
    private val mTimerKeeperUpdateListener: MutableList<AlarmTimeListener?> = ArrayList()

    private object AlarmCallbackHolder {
        val instance: AlarmCallback = AlarmCallback()
    }

    interface AlarmTimeListener {
        fun onAlarmTimeOut(hour: Int, minute: Int)
    }

    fun registerAlarmTimeListener(listener: AlarmTimeListener?) {
        if (mTimerKeeperUpdateListener != null) {
            mTimerKeeperUpdateListener.add(listener)
        }
    }

    fun unregisterAlarmTimeListener(listener: AlarmTimeListener?) {
        if (mTimerKeeperUpdateListener != null) {
            mTimerKeeperUpdateListener.remove(listener)
        }
    }


    fun setAlarmTimeout(hour: Int, minute: Int) {
        for (i in mTimerKeeperUpdateListener.indices) {
            if (mTimerKeeperUpdateListener[i] != null) {
                mTimerKeeperUpdateListener[i]!!.onAlarmTimeOut(hour, minute)
            }
        }
    }

    companion object {
        val instance: AlarmCallback
            get() = AlarmCallbackHolder.instance
    }
}
