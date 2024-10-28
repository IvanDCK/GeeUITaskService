package com.letianpai.robot.control.nets

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkInfo.DetailedState
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
//import androidx.privacysandbox.tools.core.generator.build
import com.letianpai.robot.components.utils.GeeUILogUtils
import java.lang.reflect.Method

/**
 * @author liujunbin
 */
class WIFIConnectionManager(private val mContext: Context) {
    val wifiManager: WifiManager
    private var networkId: Int = 0
    private var currentSsid: String? = null
    private var currentPassword: String? = null
    var isSetIncorrectPassword: Boolean = false


    init {
        wifiManager =
            mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * Try to connect to the specified wifi
     *
     * @param ssid     wifi name
     * @param password cryptographic password
     * @return If the connection is successful
     */
    fun connect(ssid: String, password: String): Boolean {
        this.currentSsid = ssid
        this.currentPassword = password

        val isConnected: Boolean = isConnected(mContext, ssid) //Currently connected to the specified wifi
        GeeUILogUtils.logi(TAG, "connect: is already connected = $isConnected")
        if (isConnected) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            networkId = wifiManager.addNetwork(newWifiConfig(ssid, password, true) as WifiConfiguration)
        } else {
            networkId = wifiManager.addNetwork(newWifiConfigOld(ssid, password, true))
        }

        val result: Boolean = wifiManager.enableNetwork(networkId, true)

        //        mWifiManager.removeNetwork(networkId);
        return result
    }

    /**
     * Try to connect to the specified wifi
     *
     * @return If the connection is successful
     */
    fun connect(): Boolean {
        if (TextUtils.isEmpty(currentSsid) || TextUtils.isEmpty(currentPassword)) {
            return false
        } else {
            return connect(currentSsid!!, currentPassword!!)
        }
    }

    /**
     * Configure WiFiConfiguration based on wifi name and password,
     * each attempt will first disconnect the existing connection.
     *
     * @param isClient Whether the current device is acting as a client or a server,
     * affects the SSID and PWD.
     */
    private fun newWifiConfig(ssid: String, password: String, isClient: Boolean): Any {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API >= 29
            WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .setIsHiddenSsid(true)
                .build()
        } else {
            // API < 29
            newWifiConfigOld(ssid, password, isClient)
        }
    }

    private fun newWifiConfigOld(
        ssid: String,
        password: String,
        isClient: Boolean
    ): WifiConfiguration {
        val config: WifiConfiguration = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()
        if (isClient) { // As a client, connect to the server's wifi hotspot with double quotes.
            config.SSID = "\"" + ssid + "\""
            config.preSharedKey = "\"" + password + "\""
        } else { // As a server, open wifi hotspot without double quotes.
            config.SSID = ssid
            config.preSharedKey = password
        }
        config.hiddenSSID = true
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
        config.status = WifiConfiguration.Status.ENABLED
        return config
    }

    val isWifiEnabled: Boolean
        /**
         * @return 热点是否已开启
         */
        get() {
            try {
                val methodIsWifiApEnabled: Method =
                    WifiManager::class.java.getDeclaredMethod("isWifiApEnabled")
                return methodIsWifiApEnabled.invoke(wifiManager) as Boolean
            } catch (e: Exception) {
                GeeUILogUtils.logi(
                    TAG,
                    "isWifiEnabled: " + Log.getStackTraceString(e)
                )
                return false
            }
        }

    /**
     * 是否已连接指定wifi
     */
    fun isConnected(context: Context, ssid: String?): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo = wifiManager.connectionInfo

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                wifiInfo.ssid.replace("\"", "") == ssid
    }


    fun isConnectedOld(ssid: String?): Boolean {
        val wifiInfo: WifiInfo? = wifiManager.getConnectionInfo()
        if (wifiInfo == null) {
            return false
        }
        when (wifiInfo.supplicantState) {
            SupplicantState.AUTHENTICATING, SupplicantState.ASSOCIATING, SupplicantState.ASSOCIATED, SupplicantState.FOUR_WAY_HANDSHAKE, SupplicantState.GROUP_HANDSHAKE, SupplicantState.COMPLETED -> {
                GeeUILogUtils.logi(
                    "auto_connect",
                    " wifiInfo.getSSID(): " + wifiInfo.getSSID().replace("\"", "").toString()
                )
                return wifiInfo.getSSID().replace("\"", "") == ssid
            }

            else -> return false
        }
    }

    val isConnected: Boolean
        /**
         * Whether the specified wifi is connected
         */
        get() {
            return if (TextUtils.isEmpty(currentSsid)) {
                false
            } else {
                isConnected(mContext, currentSsid) && isNetworkAvailable(
                    mContext
                )
            }
        }

    /**
     * 打开WiFi
     *
     * @return
     */
    fun openWifi(): Boolean {
        var opened: Boolean = true
        if (!wifiManager.isWifiEnabled) {
            opened = wifiManager.setWifiEnabled(true)
        }
        return opened
    }

    /**
     * Turn off wifi
     *
     * @return
     */
    fun closeWifi(): Boolean {
        var closed: Boolean = true
        if (wifiManager.isWifiEnabled) {
            closed = wifiManager.setWifiEnabled(false)
        }
        return closed
    }

    /**
     * Disconnect
     *
     * @return
     */
    fun disconnect(): WIFIConnectionManager {
        if (networkId != 0) {
            wifiManager.disableNetwork(networkId)
        }
        wifiManager.disconnect()
        return this
    }

    /**
     * Delete Network
     *
     * @return
     */
    fun removeNetwork(): Boolean {
        if (networkId != 0) {
            return wifiManager.removeNetwork(networkId)
        }
        return false
    }

    /**
     * Whether the specified Wifi has been connected
     */
    fun everConnected(ssid: String): WifiConfiguration? {
//        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
////            return TODO;
//        }

        var ssid: String = ssid
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val existingConfigs: List<WifiConfiguration>? = wifiManager.getConfiguredNetworks()
        if (existingConfigs.isNullOrEmpty()) {
            return null
        }
        ssid = "\"" + ssid + "\""
        for (existingConfig: WifiConfiguration in existingConfigs) {
            if (existingConfig.SSID == ssid) {
                return existingConfig
            }
        }
        return null
    }

    val localIp: String?
        /**
         * 获取本机的ip地址
         */
        get() {
            return convertIp(wifiManager.getConnectionInfo().getIpAddress())
        }

    private fun convertIp(ipAddress: Int): String? {
        if (ipAddress == 0) return null
        return (((ipAddress and 0xff).toString() + "." + (ipAddress shr 8 and 0xff) + "."
                + (ipAddress shr 16 and 0xff) + "." + (ipAddress shr 24 and 0xff)))
    }

    fun getConnectState(context: Context, SSID: String): Int {
        var connectState: Int = WIFI_STATE_NONE

        if (Build.VERSION.SDK_INT >= 26) {
            //Qualcomm 8.0 GO
            val wifiInfo: WifiInfo? = wifiManager.getConnectionInfo()
            val manager: ConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkInfo: NetworkInfo? = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            if (wifiInfo != null) {
                GeeUILogUtils.logi(
                    "",
                    "getConnectState()..connectState: " + connectState + "；ossid: " + SSID + ":wifiinfo ssid: " + wifiInfo.getSSID()
                )
                if (wifiInfo.getSSID() == "\"" + SSID + "\"" || wifiInfo.getSSID() == SSID) {
                    if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        GeeUILogUtils.logi(
                            "",
                            "getConnectState().. network type: " + networkInfo.getType()
                        )
                        val detailedState: DetailedState = networkInfo.getDetailedState()
                        //LogUtils.logd(TAG, ".SSID = " + wifiConfiguration.SSID + " " + detailedState + " status = " + wifiConfiguration.status + " rssi = " + rssi);
                        if (detailedState == DetailedState.CONNECTING
                            || detailedState == DetailedState.AUTHENTICATING
                            || detailedState == DetailedState.OBTAINING_IPADDR
                        ) {
                            connectState = WIFI_STATE_CONNECTED
                        } else if (detailedState == DetailedState.CONNECTED
                            || detailedState == DetailedState.CAPTIVE_PORTAL_CHECK
                        ) {
                            connectState = WIFI_STATE_CONNECTED
                        }

                        GeeUILogUtils.logi(
                            "",
                            "getConnectState().. detailedState: " + detailedState
                        )
                    }
                }
            }
        }
        return connectState
    }

    private fun findNetworkidBySsid(ssid: String): Int {
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return -1
        }
        val wifiConfigs: List<WifiConfiguration>? = wifiManager.getConfiguredNetworks()
        var curNetworkId: Int = -1
        if (wifiConfigs != null) {
            for (existingConfig: WifiConfiguration in wifiConfigs) {
                GeeUILogUtils.logi(
                    TAG,
                    "removeNetwork() wifiConfigs.. networkId: " + existingConfig.networkId
                )
                if (existingConfig.SSID == "\"" + ssid + "\"" || existingConfig.SSID == ssid) {
                    GeeUILogUtils.logi(
                        TAG,
                        "removeNetwork().. networkId: " + existingConfig.networkId
                    )
                    curNetworkId = existingConfig.networkId
                    break
                }
            }
        }
        GeeUILogUtils.logi(TAG, "removeNetwork().. return networkId: $curNetworkId")
        return curNetworkId
    }

    fun removeNetwork(ssid: String) {
        var curNetworkId: Int = -1

        curNetworkId = findNetworkidBySsid(ssid)
        if (curNetworkId != -1) {
            wifiManager.disconnect()
            val removeResult: Boolean = wifiManager.removeNetwork(curNetworkId)
            GeeUILogUtils.logi("auto_connect", "removeResult = $removeResult")
        }
    }

    fun clearWifiPasswords() {
        // Get WifiManager instance
        val wifiManager: WifiManager =
            mContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Getting a list of saved Wi-Fi configurations
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val wifiConfigurations: List<WifiConfiguration>? = wifiManager.configuredNetworks

        // Iterate through the Wi-Fi configuration list and remove the password for each network
        if (wifiConfigurations != null) {
            for (wifiConfiguration: WifiConfiguration in wifiConfigurations) {
                wifiManager.removeNetwork(wifiConfiguration.networkId) // Deleting Network Configurations
            }
        }

        // Saving configuration changes
        wifiManager.saveConfiguration()
    }


    companion object {
        private val TAG: String = WIFIConnectionManager::class.java.getName()
        private var sInstance: WIFIConnectionManager? = null
        const val WIFI_STATE_NONE: Int = 0
        const val WIFI_STATE_CONNECTING: Int = 2
        const val WIFI_STATE_CONNECTED: Int = 4
        fun getInstance(context: Context): WIFIConnectionManager {
            if (sInstance == null) {
                synchronized(WIFIConnectionManager::class.java) {
                    if (sInstance == null) {
                        sInstance = WIFIConnectionManager(context)
                    }
                }
            }
            return sInstance!!
        }


        fun getSSID(ctx: Context): String {
            val wifiManager: WifiManager =
                ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.getConnectionInfo()
            val ssid: String = wifiInfo.getSSID()
            return ssid.replace("\"".toRegex(), "")
        }

        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        /*
        fun isNetworkAvailableOld(context: Context): Boolean {
            val cm: ConnectivityManager? = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null) {
            } else {
                //如果仅仅是用来判断网络连接
                //则可以使用 cm.getActiveNetworkInfo().isAvailable();
                val info: Array<NetworkInfo> = cm.getAllNetworkInfo()
                if (info != null) {
                    for (i in info.indices) {
                        if (info.get(i).getState() == NetworkInfo.State.CONNECTED) {
                            return true
                        }
                    }
                }
            }
            return false
        }
         */

    }
}
