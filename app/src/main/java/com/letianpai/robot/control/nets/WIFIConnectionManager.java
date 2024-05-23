package com.letianpai.robot.control.nets;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.app.ActivityCompat;
import com.letianpai.robot.components.utils.GeeUILogUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author liujunbin
 */
public class WIFIConnectionManager {

    private static final String TAG = WIFIConnectionManager.class.getName();
    private static WIFIConnectionManager sInstance = null;
    private WifiManager mWifiManager;
    private int networkId;
    private Context mContext;
    private String currentSsid;
    private String currentPassword;
    public final static int WIFI_STATE_NONE = 0;
    public final static int WIFI_STATE_CONNECTING = 2;
    public final static int WIFI_STATE_CONNECTED = 4;
    private boolean isSetIncorrectPassword = false;


    public WIFIConnectionManager(Context context) {
        this.mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static WIFIConnectionManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (WIFIConnectionManager.class) {
                if (sInstance == null) {
                    sInstance = new WIFIConnectionManager(context);
                }
            }
        }
        return sInstance;
    }


    /**
     * 尝试连接指定wifi
     *
     * @param ssid     wifi名
     * @param password 密码
     * @return 是否连接成功
     */
    public boolean connect(@NonNull String ssid, @NonNull String password) {
        this.currentSsid = ssid;
        this.currentPassword = password;

        boolean isConnected = isConnected(ssid);//当前已连接至指定wifi
        GeeUILogUtils.logi(TAG, "connect: is already connected = " + isConnected);
        if (isConnected) {
            return true;
        }
        networkId = mWifiManager.addNetwork(newWifiConfig(ssid, password, true));
        boolean result = mWifiManager.enableNetwork(networkId, true);
//        mWifiManager.removeNetwork(networkId);

        return result;
    }

    /**
     * 尝试连接指定wifi
     *
     * @return 是否连接成功
     */
    public boolean connect() {
        if (TextUtils.isEmpty(currentSsid) || TextUtils.isEmpty(currentPassword)) {
            return false;
        } else {
            return connect(currentSsid, currentPassword);
        }
    }

    /**
     * 根据wifi名与密码配置 WiFiConfiguration, 每次尝试都会先断开已有连接
     *
     * @param isClient 当前设备是作为客户端,还是作为服务端, 影响SSID和PWD
     */
    @NonNull
    private WifiConfiguration newWifiConfig(String ssid, String password, boolean isClient) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        if (isClient) {//作为客户端, 连接服务端wifi热点时要加双引号
            config.SSID = "\"" + ssid + "\"";
            config.preSharedKey = "\"" + password + "\"";
        } else {//作为服务端, 开放wifi热点时不需要加双引号
            config.SSID = ssid;
            config.preSharedKey = password;
        }
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        return config;
    }

    /**
     * @return 热点是否已开启
     */
    public boolean isWifiEnabled() {
        try {
            Method methodIsWifiApEnabled = WifiManager.class.getDeclaredMethod("isWifiApEnabled");
            return (boolean) methodIsWifiApEnabled.invoke(mWifiManager);
        } catch (Exception e) {
            GeeUILogUtils.logi(TAG, "isWifiEnabled: " + Log.getStackTraceString(e));
            return false;
        }
    }

    /**
     * 是否已连接指定wifi
     */
    public boolean isConnected(String ssid) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return false;
        }
        switch (wifiInfo.getSupplicantState()) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
            case COMPLETED:
                GeeUILogUtils.logi("auto_connect", " wifiInfo.getSSID(): " + wifiInfo.getSSID().replace("\"", "").toString());
                return wifiInfo.getSSID().replace("\"", "").equals(ssid);
            default:
                return false;
        }
    }

    /**
     * 是否已连接指定wifi
     */
    public boolean isConnected() {
        if (TextUtils.isEmpty(currentSsid)) {
            return false;
        } else {
            return isConnected(currentSsid) && isNetworkAvailable(mContext);
        }
    }

    public static String getSSID(Context ctx) {
        WifiManager wifiManager =
                (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        return ssid.replaceAll("\"", "");
    }

    /**
     * 打开WiFi
     *
     * @return
     */
    public boolean openWifi() {
        boolean opened = true;
        if (!mWifiManager.isWifiEnabled()) {
            opened = mWifiManager.setWifiEnabled(true);
        }
        return opened;
    }

    /**
     * 关闭wifi
     *
     * @return
     */
    public boolean closeWifi() {
        boolean closed = true;
        if (mWifiManager.isWifiEnabled()) {
            closed = mWifiManager.setWifiEnabled(false);
        }
        return closed;
    }

    /**
     * 断开连接
     *
     * @return
     */
    public WIFIConnectionManager disconnect() {
        if (networkId != 0) {
            mWifiManager.disableNetwork(networkId);
        }
        mWifiManager.disconnect();
        return this;
    }

    /**
     * 删除网络
     *
     * @return
     */
    public boolean removeNetwork() {
        if (networkId != 0) {
            return mWifiManager.removeNetwork(networkId);
        }
        return false;
    }

    /**
     * 是否连接过指定Wifi
     */
    @Nullable
    public WifiConfiguration everConnected(String ssid) {
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

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        if (existingConfigs == null || existingConfigs.isEmpty()) {
            return null;
        }
        ssid = "\"" + ssid + "\"";
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals(ssid)) {
                return existingConfig;
            }
        }
        return null;
    }

    /**
     * 获取本机的ip地址
     */
    @Nullable
    public String getLocalIp() {
        return convertIp(mWifiManager.getConnectionInfo().getIpAddress());
    }

    private String convertIp(int ipAddress) {
        if (ipAddress == 0) return null;
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    public int getConnectState(Context context, String SSID) {
        int connectState = WIFI_STATE_NONE;

        if (Build.VERSION.SDK_INT >= 26) {
            //高通8.0 GO
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (wifiInfo != null) {
                GeeUILogUtils.logi("", "getConnectState()..connectState: " + connectState + "；ossid: " + SSID + ":wifiinfo ssid: " + wifiInfo.getSSID());
                if (wifiInfo.getSSID().equals("\"" + SSID + "\"") || wifiInfo.getSSID().equals(SSID)) {

                    if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        GeeUILogUtils.logi("", "getConnectState().. network type: " + networkInfo.getType());
                        NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
                        //LogUtils.logd(TAG, ".SSID = " + wifiConfiguration.SSID + " " + detailedState + " status = " + wifiConfiguration.status + " rssi = " + rssi);
                        if (detailedState.equals(NetworkInfo.DetailedState.CONNECTING)
                                || detailedState.equals(NetworkInfo.DetailedState.AUTHENTICATING)
                                || detailedState.equals(NetworkInfo.DetailedState.OBTAINING_IPADDR)) {
                            connectState = WIFI_STATE_CONNECTED;
                        } else if (detailedState.equals(NetworkInfo.DetailedState.CONNECTED)
                                || detailedState.equals(NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK)) {
                            connectState = WIFI_STATE_CONNECTED;
                        }

                        GeeUILogUtils.logi("", "getConnectState().. detailedState: " + detailedState);
                    }
                }

            }

        }
        return connectState;

    }

    private int findNetworkidBySsid(String ssid) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }
        List<WifiConfiguration> wifiConfigs = mWifiManager.getConfiguredNetworks();
        int curNetworkId = -1;
        if (wifiConfigs != null) {
            for (WifiConfiguration existingConfig : wifiConfigs) {
                GeeUILogUtils.logi(TAG, "removeNetwork() wifiConfigs.. networkId: " + existingConfig.networkId);
                if (existingConfig.SSID.equals("\"" + ssid + "\"") || existingConfig.SSID.equals(ssid)) {
                    GeeUILogUtils.logi(TAG, "removeNetwork().. networkId: " + existingConfig.networkId);
                    curNetworkId = existingConfig.networkId;
                    break;
                }
            }
        }
        GeeUILogUtils.logi(TAG, "removeNetwork().. return networkId: " + curNetworkId);
        return curNetworkId;
    }

    public void removeNetwork(final String ssid) {
        int curNetworkId = -1;

        curNetworkId = findNetworkidBySsid(ssid);
        if (curNetworkId != -1) {
            mWifiManager.disconnect();
            boolean removeResult = mWifiManager.removeNetwork(curNetworkId);
            GeeUILogUtils.logi("auto_connect", "removeResult = " + removeResult);
        }

    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            //如果仅仅是用来判断网络连接
            //则可以使用 cm.getActiveNetworkInfo().isAvailable();
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setSetIncorrectPassword(boolean setIncorrectPassword) {
        isSetIncorrectPassword = setIncorrectPassword;
    }

    public boolean isSetIncorrectPassword() {
        return isSetIncorrectPassword;
    }

    public void clearWifiPasswords() {
        // 获取 WifiManager 实例
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        // 获取保存的 Wi-Fi 配置列表
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();

        // 遍历 Wi-Fi 配置列表，移除每个网络的密码
        if (wifiConfigurations != null) {
            for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
                wifiManager.removeNetwork(wifiConfiguration.networkId); // 删除网络配置
            }
        }

        // 保存配置更改
        wifiManager.saveConfiguration();
    }
}
