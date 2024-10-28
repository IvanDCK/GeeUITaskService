package com.letianpai.robot.control.nets

import android.content.Context
import android.os.Build
import com.letianpai.robot.components.network.nets.GeeUINetConsts
import com.letianpai.robot.components.network.nets.GeeUINetworkConsts
import com.letianpai.robot.components.network.nets.GeeUINetworkUtil
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.control.encryption.EncryptionUtils
import okhttp3.Callback

/**
 * @author liujunbin
 */
object GeeUiNetManager {
    /**
     * Get Calendar List
     * @param context
     * @param callback
     */
    fun getCalendarList(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.CALENDAR_LIST, callback)
    }

    /**
     * Get a list of countdown timers
     * @param context
     * @param callback
     */
    fun getCountDownList(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.COUNTDOWN_LIST, callback)
    }

    /**
     * Get fan information
     * @param context
     * @param callback
     */
    fun getFansInfoList(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.FANS_INFO_LIST, callback)
    }

    /**
     * Get a list of general information
     * @param context
     * @param callback
     */
    fun getGeneralInfoList(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.GENERAL_INFO, callback)
    }

    /**
     * Get weather information
     * @param context
     * @param callback
     */
    fun getWeatherInfo(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.WEATHER_INFO, callback)
    }

    /**
     * Get alarm list
     * @param context
     * @param callback
     */
    fun getClockList(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.CLOCK_LIST, callback)
    }


    //    /**
    //     * 获取股票信息
    //     * @param context
    //     * @param callback
    //     */
    //    public static void getDeviceInfo(Context context, Callback callback){
    //        GeeUINetworkUtil.get11(context, GeeUINetworkConsts.GET_SN_BY_MAC,callback);
    //    }
    fun getDeviceInfo(context: Context?, callback: Callback?) {
        val ts: String = EncryptionUtils.ts
        val auth: String = EncryptionUtils.getHardCodeSign(ts)
        GeeUINetworkUtil.get11(context, auth, ts, GeeUINetworkConsts.GET_SN_BY_MAC, callback)
    }

    /**
     * Get Stock Information
     * @param context
     * @param callback
     */
    fun getStock(context: Context?, callback: Callback?) {
        val ts: String = EncryptionUtils.ts
        val sn: String = Build.getSerial()
        val hardCode: String = SystemUtil.getHardCode()
        //        String hardCode = "YMcQMMZc49ZM0M";
        val auth: String = EncryptionUtils.getRobotSign(sn, hardCode, ts)

        GeeUINetworkUtil.get11(context, auth, sn, ts, GeeUINetworkConsts.STOCK_INFO, callback)
    }

    /**
     * Get a list of wake words
     * @param context
     * @param callback
     */
    fun getTipsList(context: Context?, callback: Callback?) {
        val hashMap: HashMap<String, String> = HashMap()
        val sn: String = SystemUtil.getLtpSn()
        hashMap[GeeUINetConsts.HASH_MAP_KEY_SN] = sn
        hashMap[GeeUINetConsts.HASH_MAP_KEY_CONFIG] = GeeUINetConsts.HASH_MAP_CONFIG_KEY_VALUE
        GeeUINetworkUtil.get(context, GeeUINetworkConsts.GET_COMMON_CONFIG, hashMap, callback)
        //        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.GET_COMMON_CONFIG,callback);
    }

    /**
     * Get full robot configuration
     * @param context
     * @param callback
     */
    fun getAllConfig(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.GET_ALL_CONFIG, callback)
    }

    /**
     *
     * @param context
     * @param callback
     */
    fun getTimeStamp(context: Context?, callback: Callback?) {
        GeeUINetworkUtil.get(context, GeeUINetworkConsts.GET_SERVER_TIME_STAMP, callback)
    }

    //    /**
    //     * 更新机器人状态
    //     * @param context
    //     * @param callback
    //     */
    //    public static void uploadStatus(Context context, Callback callback) {
    //        GeeUINetworkUtil.uploadStatus(context,callback);
    //    }
    //    /**
    //     * 更新机器人状态
    //     * @param context
    //     * @param callback
    //     */
    //    public static void uploadStatus(Context context, Callback callback) {
    //        String sn = SystemUtil.getLtpSn();
    //        String mac = SystemUtil.getWlanMacAddress();
    //        String wifiSsid = SystemFunctionUtil.getConnectWifiSsid(context);
    //        String btAddress = SystemFunctionUtil.getBtAddressByReflection();
    //        int battery = ChargingUpdateCallback.getInstance().getBattery();
    //        int volume = SleepModeManager.getInstance(context).getCurrentVolume();
    //
    //        //TODO Increased access
    //        HashMap hashMap = new HashMap();
    //        hashMap.put("battery_percent", battery);
    //        hashMap.put("ble", btAddress);
    ////        hashMap.put("humidity", 0);
    //        hashMap.put("mac", mac);
    ////        hashMap.put("mcu_version", "");
    //        hashMap.put("rom_version", Build.DISPLAY);
    //        hashMap.put("sn", sn);
    //        hashMap.put("sound_volume", volume);
    //        hashMap.put("system_version", Build.DISPLAY);
    //        hashMap.put("temperature", 0);
    ////        hashMap.put("update", 0);
    //        hashMap.put("wifi_name", wifiSsid);
    ////        hashMap.put("uptime", 0);
    //        GeeUINetworkUtil.post(GeeUINetworkConsts.UPLOAD_STATUS,hashMap,callback);
    //
    //    }
    //    /**
    //     * 更新机器人状态
    //     * @param context
    //     * @param callback
    //     */
    //    public static void uploadStatus(Context context, int hot,float temp,Callback callback) {
    //        String sn = SystemUtil.getLtpSn();
    //        String mac = SystemUtil.getWlanMacAddress();
    //        String ip = SystemUtil.getIp(context);
    //        String mcu = SystemUtil.getMcu();
    //        String wifiSsid = SystemFunctionUtil.getConnectWifiSsid(context);
    //        String btAddress = SystemFunctionUtil.getBtAddressByReflection();
    //        int battery = ChargingUpdateCallback.getInstance().getBattery();
    //        int volume = SleepModeManager.getInstance(context).getCurrentVolume();
    //        boolean isInHighTemperature = TemperatureUpdateCallback.getInstance().isInHighTemperature();
    //
    //
    //        //TODO Increased access
    //        HashMap hashMap = new HashMap();
    //        if (battery >0){
    //            hashMap.put("battery_percent", battery);
    //        }
    //        hashMap.put("ble", btAddress);
    ////        hashMap.put("humidity", 0);
    //        hashMap.put("mac", mac);
    //        hashMap.put("mcu_version", mcu);
    //        hashMap.put("ip", ip);
    //        hashMap.put("rom_version", Build.DISPLAY);
    //        hashMap.put("sn", sn);
    //        hashMap.put("sound_volume", volume);
    //        hashMap.put("system_version", Build.DISPLAY);
    //        hashMap.put("temperature", temp);
    //
    //        if (isInHighTemperature){
    //            hashMap.put("hot_status", 1);
    //        }else{
    //            hashMap.put("hot_status", 0);
    //        }
    ////        hashMap.put("update", 0);
    //        hashMap.put("wifi_name", wifiSsid);
    ////        hashMap.put("uptime", 0);
    //        if (ChargingUpdateCallback.getInstance().isCharging()){
    //            hashMap.put("charge_status", 1);
    //        }else{
    //            hashMap.put("charge_status", 2);
    //        }
    //        com.letianpai.robot.components.network.nets.GeeUiNetManager.uploadStatus(context, SystemUtil.isInChinese(),hashMap,callback);
    //    }
    /**
     * Update robot status
     * @param callback
     */
    fun moduleChange(module: String, callback: Callback?) {
        val modules: Array<String> = arrayOf(module)
        //TODO Increased access
        val hashMap: HashMap<String, Array<String>> = HashMap()
        hashMap["selected_module_tag_list"] = modules
        GeeUINetworkUtil.post1(GeeUINetworkConsts.POST_MODULE_CHANGE, hashMap, callback)
    }

    /**
     * Reset robot state
     * @param callback
     */
    fun robotReset(callback: Callback?) {
        val hashMap: HashMap<String, Int> = HashMap()
        hashMap["reset_status"] = 0
        GeeUINetworkUtil.post2(GeeUINetworkConsts.POST_RESET_STATUS, hashMap, callback)
    }

    /**
     * Reset robot state
     * @param callback
     */
    fun robotReset1(callback: Callback?) {
        val ts: String = EncryptionUtils.ts
        val sn: String = Build.getSerial()
        val hardCode: String = SystemUtil.getHardCode()
        //        String hardCode = "YMcQMMZc49ZM0M";
        val auth: String = EncryptionUtils.getRobotSign(sn, hardCode, ts)

        val hashMap: HashMap<String, Int> = HashMap()
        hashMap["reset_status"] = 0
        GeeUINetworkUtil.post2(hashMap, auth, ts, GeeUINetworkConsts.POST_RESET_STATUS, callback)
    }
}
