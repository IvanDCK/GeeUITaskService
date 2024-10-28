package com.letianpai.robot.control.broadcast

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.letianpai.robot.control.broadcast.appswitch.AppSwitchReceiver
import com.letianpai.robot.control.broadcast.battery.BatteryReceiver
import com.letianpai.robot.control.broadcast.timer.TimerReceiver

/**
 * Broadcast Manager
 *
 * @author liujunbin
 */
class LauncherBroadcastReceiverManager(var mContext: Context) {
    init {
        init(mContext)
    }

    private fun init(context: Context) {
        // TODO Add broadcasts to listen to for initialisation
        // TODO This is a unified entry point to listen to the state, the only listening position,
        //  the subsequent need for state, unified here to listen to the distribution after
        setBatteryListener()
        setNetWorkChangeListener()
        // setWifiChangeListener();
        setTimeListener()
        AppSwitchListener()
        setUSBReceiver()
    }

    //Battery listener
    private fun setBatteryListener() {
        val batteryReceiver = BatteryReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED)
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        mContext.registerReceiver(batteryReceiver, intentFilter)
    }

    /**
     * Setting up the USB
     */
    private fun setUSBReceiver() {
        val usbStatusReceiver = USBStatusReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(USBStatusReceiver.Companion.ACTION)
        mContext.registerReceiver(usbStatusReceiver, intentFilter)
    }


    private fun setNetWorkChangeListener() {
        val netChangeReceiver = NetWorkChangeReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION)
        //        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        mContext.registerReceiver(netChangeReceiver, intentFilter)
    }

    // private void setWifiChangeListener() {
    //     WifiReceiver wifiReceiver = new WifiReceiver();
    //     IntentFilter intentFilter = new IntentFilter();
    //     intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    //     intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    //     mContext.registerReceiver(wifiReceiver, intentFilter);
    // }
    private fun setTimeListener() {
        val mTimeReceiver = TimerReceiver()
        val timeFilter = IntentFilter()
        timeFilter.addAction(Intent.ACTION_TIME_TICK)
        mContext.registerReceiver(mTimeReceiver, timeFilter)
    }

    private fun AppSwitchListener() {
        val appSwitchReceiver = AppSwitchReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        filter.addDataScheme("package")
        mContext.registerReceiver(appSwitchReceiver, filter)
    }


    companion object {
        private var instance: LauncherBroadcastReceiverManager? = null

        fun getInstance(context: Context): LauncherBroadcastReceiverManager {
            synchronized(LauncherBroadcastReceiverManager::class.java) {
                if (instance == null) {
                    instance = LauncherBroadcastReceiverManager(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
