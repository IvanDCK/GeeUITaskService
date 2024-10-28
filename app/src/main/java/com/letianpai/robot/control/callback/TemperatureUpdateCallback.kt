package com.letianpai.robot.control.callback

/**
 * Created by liujunbin
 */
class TemperatureUpdateCallback private constructor() {
    var isInHighTemperature: Boolean = false
        private set

    var temp: Float = 0f
        private set
    private val mTemperatureUpdateListener: MutableList<TemperatureUpdateListener?> =
        ArrayList()

    private object TemperatureUpdateCallBackHolder {
        val instance: TemperatureUpdateCallback = TemperatureUpdateCallback()
    }

    interface TemperatureUpdateListener {
        fun onTemperatureUpdate(temp: Float)
    }

    fun registerTemperatureUpdateListener(listener: TemperatureUpdateListener?) {
        if (mTemperatureUpdateListener != null) {
            mTemperatureUpdateListener.add(listener)
        }
    }

    fun unregisterTemperatureUpdateListener(listener: TemperatureUpdateListener?) {
        if (mTemperatureUpdateListener != null) {
            mTemperatureUpdateListener.remove(listener)
        }
    }

    fun setTemperature(temp: Float) {
        this.temp = temp
        if (temp >= HIGH_TEMP) {
            isInHighTemperature = true
        } else if (temp <= TARGET_TEMP) {
            isInHighTemperature = false
        } else if (temp > TARGET_TEMP && temp < HIGH_TEMP) {
        } else {
            isInHighTemperature = false
        }

        for (i in mTemperatureUpdateListener.indices) {
            if (mTemperatureUpdateListener[i] != null) {
                mTemperatureUpdateListener[i]!!.onTemperatureUpdate(temp)
            }
        }
    }

    companion object {
        const val HIGH_TEMP: Int = 90
        const val TARGET_TEMP: Int = 75
        val instance: TemperatureUpdateCallback
            get() = TemperatureUpdateCallBackHolder.instance
    }
}
