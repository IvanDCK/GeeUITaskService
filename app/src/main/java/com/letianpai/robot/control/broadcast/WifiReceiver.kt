package com.letianpai.robot.control.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback

/**
 * @author liujunbin
 * unused yujianbin Recorded on 18 February 24
 */
@Deprecated("")
class WifiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiManager.WIFI_STATE_CHANGED_ACTION == action) {
            // WiFi 状态变化
            val wifiState =
                intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            when (wifiState) {
                WifiManager.WIFI_STATE_ENABLED -> {}
                WifiManager.WIFI_STATE_DISABLED -> {}
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION == action) {
            // Network status changes
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
            if (networkInfo != null) {
                if (networkInfo.state == NetworkInfo.State.CONNECTED) {
                    // Connected to a WiFi network
                    Log.e("letianpai_net", "net_Connect")
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo.ssid // Get the SSID of the currently connected WiFi network.
                    NetworkChangingUpdateCallback.instance.setNetworkStatus(
                        NetworkChangingUpdateCallback.NETWORK_TYPE_WIFI,
                        3
                    )
                } else if (networkInfo.state == NetworkInfo.State.DISCONNECTED) {
                    Log.e("letianpai_net", "net_Disconnect")
                    // WiFi network disconnected
                    NetworkChangingUpdateCallback.instance.setNetworkStatus(
                        NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED,
                        3
                    )
                }
            }
        }
    }
}