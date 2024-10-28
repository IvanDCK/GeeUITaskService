package com.letianpai.robot.control.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback

/**
 *
 * @author liujunbin
 */
class NetWorkChangeReceiver : BroadcastReceiver() {
    private var mWifiManager: WifiManager? = null
    private var mConnectivityManager: ConnectivityManager? = null
    private var mNetworkinfo: NetworkInfo? = null
    private var mTelePhonyManager: TelephonyManager? = null


    override fun onReceive(context: Context, intent: Intent) {
        mWifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mTelePhonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            updateNetworkStatus(context, intent)
        } else if (intent.action == WifiManager.RSSI_CHANGED_ACTION) {
        } else if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
            when (intent.getIntExtra(
                WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN
            )) {
                WifiManager.WIFI_STATE_DISABLED -> LauncherInfoManager.getInstance(context)
                    .wifiStates = false

                WifiManager.WIFI_STATE_ENABLED -> LauncherInfoManager.getInstance(context)
                    .wifiStates = true
            }
        } else if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            mConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = mConnectivityManager!!.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                if (ConnectivityManager.TYPE_WIFI == activeNetworkInfo.type) {
                    NetworkChangingUpdateCallback.instance.setNetworkStatus(
                        NetworkChangingUpdateCallback.NETWORK_TYPE_WIFI,
                        3
                    )
                    //Mobile network
                } else if (ConnectivityManager.TYPE_MOBILE == activeNetworkInfo.type) {
                    NetworkChangingUpdateCallback.instance.setNetworkStatus(
                        NetworkChangingUpdateCallback.NETWORK_TYPE_MOBILE,
                        -1
                    )
                } else {
                    NetworkChangingUpdateCallback.instance.setNetworkStatus(
                        NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED,
                        -1
                    )
                }
            } else {
                NetworkChangingUpdateCallback.instance.setNetworkStatus(
                    NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED,
                    -1
                )
            }
        }
    }

    private fun updateNetworkStatus(context: Context, intent: Intent) {
        mNetworkinfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)

        if (mWifiManager!!.isWifiEnabled && isWifiConnected(context)) {
            val wifiInfo = mWifiManager!!.connectionInfo
            val level = WifiManager.calculateSignalLevel(wifiInfo.rssi, 3)
            NetworkChangingUpdateCallback.instance
                .setNetworkStatus(NetworkChangingUpdateCallback.NETWORK_TYPE_WIFI, level)
        } else {
            mNetworkinfo = mConnectivityManager!!.activeNetworkInfo
            if (null == mNetworkinfo) {
                NetworkChangingUpdateCallback.instance.setNetworkStatus(
                    NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED,
                    -1
                )
            } else if (!mNetworkinfo!!.isAvailable || !mNetworkinfo!!.isConnected) {
                NetworkChangingUpdateCallback.instance.setNetworkStatus(
                    NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED,
                    -1
                )
            } else if (mNetworkinfo!!.type == ConnectivityManager.TYPE_MOBILE) {
                // There is no need for a mobile network at this time and this part of the logic is not addressed
            }
        }
    }

    companion object {
        fun isWifiConnected(context: Context?): Boolean {
            if (context != null) {
                val mConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val mWiFiNetworkInfo =
                    mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (mWiFiNetworkInfo != null) {
                    return mWiFiNetworkInfo.isAvailable && mWiFiNetworkInfo.isConnected
                }
            }
            return false
        }
    }
}


