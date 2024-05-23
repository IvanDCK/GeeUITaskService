package com.letianpai.robot.control.nets;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.letianpai.robot.components.locale.LocaleUtils;
import com.letianpai.robot.components.network.nets.GeeUINetConsts;
import com.letianpai.robot.components.network.nets.GeeUINetworkConsts;
import com.letianpai.robot.components.network.nets.GeeUINetworkUtil;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.callback.TemperatureUpdateCallback;
import com.letianpai.robot.control.encryption.EncryptionUtils;
import com.letianpai.robot.control.manager.SleepModeManager;
import com.letianpai.robot.control.system.SystemFunctionUtil;

import java.util.HashMap;

import okhttp3.Callback;

/**
 * @author liujunbin
 */
public class GeeUiNetManager {

    /**
     * 获取日历列表
     * @param context
     * @param callback
     */
    public static void getCalendarList(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.CALENDAR_LIST,callback);
    }

    /**
     * 获取倒计时列表
     * @param context
     * @param callback
     */
    public static void getCountDownList(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.COUNTDOWN_LIST,callback);
    }

    /**
     * 获取粉丝信息
     * @param context
     * @param callback
     */
    public static void getFansInfoList(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.FANS_INFO_LIST,callback);
    }

    /**
     * 获取通用信息列表
     * @param context
     * @param callback
     */
    public static void getGeneralInfoList(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.GENERAL_INFO,callback);
    }

    /**
     * 获取天气信息
     * @param context
     * @param callback
     */
    public static void getWeatherInfo(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.WEATHER_INFO,callback);
    }

    /**
     * 获取闹钟列表
     * @param context
     * @param callback
     */
    public static void getClockList(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.CLOCK_LIST,callback);
    }

//    /**
//     * 获取股票信息
//     * @param context
//     * @param callback
//     */
//    public static void getDeviceInfo(Context context, Callback callback){
//        GeeUINetworkUtil.get11(context, GeeUINetworkConsts.GET_SN_BY_MAC,callback);
//    }


    public static void getDeviceInfo(Context context, Callback callback){
        String ts = EncryptionUtils.getTs();
        String auth = EncryptionUtils.getHardCodeSign(ts);
        GeeUINetworkUtil.get11(context,auth, ts,GeeUINetworkConsts.GET_SN_BY_MAC,callback);

    }

    /**
     * 获取股票信息
     * @param context
     * @param callback
     */
    public static void getStock(Context context, Callback callback){
        String ts = EncryptionUtils.getTs();
        String sn = Build.getSerial();
        String hardCode = SystemUtil.getHardCode();
//        String hardCode = "YMcQMMZc49ZM0M";
        String auth = EncryptionUtils.getRobotSign(sn,hardCode,ts);

        GeeUINetworkUtil.get11(context , auth, sn,ts, GeeUINetworkConsts.STOCK_INFO,callback);
    }

    /**
     * 获取唤醒词列表
     * @param context
     * @param callback
     */
    public static void getTipsList(Context context, Callback callback){

        HashMap<String,String> hashMap = new HashMap<>();
        String sn = SystemUtil.getLtpSn();
        hashMap.put(GeeUINetConsts.HASH_MAP_KEY_SN,sn);
        hashMap.put(GeeUINetConsts.HASH_MAP_KEY_CONFIG,GeeUINetConsts.HASH_MAP_CONFIG_KEY_VALUE);
        GeeUINetworkUtil.get(context,GeeUINetworkConsts.GET_COMMON_CONFIG,hashMap,callback);
//        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.GET_COMMON_CONFIG,callback);
    }

    /**
     * 获取机器人全部配置
     * @param context
     * @param callback
     */
    public static void getAllConfig(Context context, Callback callback){
        GeeUINetworkUtil.get1(context, GeeUINetworkConsts.GET_ALL_CONFIG,callback);
    }

    /**
     *
     * @param context
     * @param callback
     */
    public static void getTimeStamp(Context context, Callback callback){
        GeeUINetworkUtil.get(context, GeeUINetworkConsts.GET_SERVER_TIME_STAMP,callback);
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
//        //TODO 增加获取
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
//        //TODO 增加获取
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
     * 更新机器人状态
     * @param callback
     */
    public static void moduleChange( String module ,Callback callback) {
        String [] modules  = new String[]{module};
        //TODO 增加获取
        HashMap hashMap = new HashMap();
        hashMap.put("selected_module_tag_list", modules);
        GeeUINetworkUtil.post1(GeeUINetworkConsts.POST_MODULE_CHANGE,hashMap,callback);
    }

    /**
     * 重置机器人状态
     * @param callback
     */
    public static void robotReset(Callback callback) {
        HashMap hashMap = new HashMap();
        hashMap.put("reset_status", 0);
        GeeUINetworkUtil.post2(GeeUINetworkConsts.POST_RESET_STATUS,hashMap,callback);

    }

    /**
     * 重置机器人状态
     * @param callback
     */
    public static void robotReset1(Callback callback) {
        String ts = EncryptionUtils.getTs();
        String sn = Build.getSerial();
        String hardCode = SystemUtil.getHardCode();
//        String hardCode = "YMcQMMZc49ZM0M";
        String auth = EncryptionUtils.getRobotSign(sn,hardCode,ts);

        HashMap hashMap = new HashMap();
        hashMap.put("reset_status", 0);
        GeeUINetworkUtil.post2(hashMap,auth,ts,GeeUINetworkConsts.POST_RESET_STATUS,callback);

    }

}
