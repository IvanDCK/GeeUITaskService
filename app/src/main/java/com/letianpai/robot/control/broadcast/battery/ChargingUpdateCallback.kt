package com.letianpai.robot.control.broadcast.battery

/**
 * Created by liujunbin
 */
class ChargingUpdateCallback private constructor() {
    var battery: Int = 0
        private set
    var isCharging: Boolean = false
        private set
    private val mChargingListenerList: MutableList<ChargingUpdateListener?> = ArrayList()

    private object ChargingUpdateCallBackHolder {
        val instance: ChargingUpdateCallback = ChargingUpdateCallback()
    }

    interface ChargingUpdateListener {
        fun onChargingUpdateReceived(changingStatus: Boolean, percent: Int)
        fun onChargingUpdateReceived(changingStatus: Boolean, percent: Int, chargePlug: Int)
    }

    fun registerChargingStatusUpdateListener(listener: ChargingUpdateListener?) {
        if (mChargingListenerList != null) {
            mChargingListenerList.add(listener)
        }
    }

    fun unregisterChargingStatusUpdateListener(listener: ChargingUpdateListener?) {
        if (mChargingListenerList != null) {
            mChargingListenerList.remove(listener)
        }
    }

    fun setChargingStatus(changingStatus: Boolean, percent: Int) {
        this.battery = percent
        this.isCharging = changingStatus
        for (i in mChargingListenerList.indices) {
            if (mChargingListenerList[i] != null) {
                mChargingListenerList[i]!!.onChargingUpdateReceived(changingStatus, percent)
            }
        }
    }

    fun setChargingStatus(changingStatus: Boolean, percent: Int, chargePlug: Int) {
        this.battery = percent
        this.isCharging = changingStatus
        for (i in mChargingListenerList.indices) {
            if (mChargingListenerList[i] != null) {
                mChargingListenerList[i]!!
                    .onChargingUpdateReceived(changingStatus, percent, chargePlug)
            }
        }
    }

    companion object {
        const val LOW_BATTERY_SHUTDOWN_STANDARD: Int = 20
        const val LOW_BATTERY_NOTICE: Int = 30
        val instance: ChargingUpdateCallback
            get() = ChargingUpdateCallBackHolder.instance
    }
}
