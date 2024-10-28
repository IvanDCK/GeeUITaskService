package com.letianpai.robot.control.callback

/**
 * Created by liujunbin
 */
class NetworkChangingUpdateCallback private constructor() {
    private val currentNetworkType = 0

    private val mNetworkChangeUpdateListenerList = ArrayList<NetworkChangingUpdateListener>()

    private object NetworkChangingUpdateHolder {
        val instance: NetworkChangingUpdateCallback = NetworkChangingUpdateCallback()
    }

    interface NetworkChangingUpdateListener {
        fun onNetworkChargingUpdateReceived(networkType: Int, networkStatus: Int)
    }

    fun registerChargingStatusUpdateListener(listener: NetworkChangingUpdateListener?) {
        if (listener != null) {
            mNetworkChangeUpdateListenerList.add(listener)
        }
    }

    fun unregisterChargingStatusUpdateListener(listener: NetworkChangingUpdateListener?) {
        if (listener != null && mNetworkChangeUpdateListenerList.size > 0) {
            mNetworkChangeUpdateListenerList.remove(listener)
        }
    }

    fun setNetworkStatus(networkType: Int, networkStatus: Int) {
        for (i in mNetworkChangeUpdateListenerList.indices) {
            mNetworkChangeUpdateListenerList[i].onNetworkChargingUpdateReceived(
                networkType,
                networkStatus
            )
        }
    }

    val isNetworkConnected: Boolean
        get() = (currentNetworkType == NETWORK_TYPE_WIFI)

    companion object {
        const val NETWORK_TYPE_DISABLED: Int = 0
        const val NETWORK_TYPE_WIFI: Int = 1
        const val NETWORK_TYPE_MOBILE: Int = 2
        val instance: NetworkChangingUpdateCallback
            get() = NetworkChangingUpdateHolder.instance
    }
}
