package com.letianpai.robot.control.broadcast

import android.content.Context

/**
 * 手表状态持有
 */
class LauncherInfoManager private constructor(private val mContext: Context) {
    /**
     * 获取充电状态
     *
     * @return
     */
    /**
     * 设置充电状态
     *
     * @param isCharging
     */
    var isChargingMode: Boolean = false
    /**
     * 获取电池电量
     *
     * @return
     */
    /**
     * 设置电量等级
     *
     * @param mBatteryLevel
     */
    var batteryLevel: Int = 0
    var wifiStates: Boolean = false

    init {
        init(mContext)
    }

    private fun init(context: Context) {
    }


    companion object {
        private var instance: LauncherInfoManager? = null
        fun getInstance(context: Context): LauncherInfoManager {
            synchronized(LauncherBroadcastReceiverManager::class.java) {
                if (instance == null) {
                    instance = LauncherInfoManager(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
