package com.letianpai.robot.control.encryption;

import android.content.Context;

import com.google.gson.Gson;
import com.letianpai.robot.components.network.system.SystemUtil;

/**
 * @author liujunbin
 */
public class EncryptionUtils {
    private Gson mGson;

    private static EncryptionUtils instance;
    private Context mContext;
    private static final String partSecretKey = "your partSecretKey";

    private EncryptionUtils(Context context) {
        this.mContext = context;
        init();
    }

    public static EncryptionUtils getInstance(Context context) {
        synchronized (EncryptionUtils.class) {
            if (instance == null) {
                instance = new EncryptionUtils(context.getApplicationContext());
            }
            return instance;
        }

    }

    private void init() {
        mGson = new Gson();

    }

    private void getHardcode() {

    }

    /**
     * 获取签名的方式
     * 
     * @param inputValue
     * @param ts
     * @return
     */
    private static String getDeviceSign(String inputValue, String ts) {
        String deviceSecretKey = MD5.encode(inputValue + ts + partSecretKey);
        String macSign = Sha256Utils.getSha256Str(inputValue + ts + deviceSecretKey);
        return macSign;

    }

    public static String getRobotSign() {
        String mac = getRobotMac();
        String ts = getTs();
        String robotSign = getDeviceSign(mac, ts);
        robotSign = "Bearer " + robotSign;
        return robotSign;

    }

    public static String getHardCodeSign(String ts) {
        String mac = getRobotMac();
        String robotSign = getDeviceSign(mac, ts);
        robotSign = "Bearer " + robotSign;
        return robotSign;

    }

    /**
     * 获取机器人签名
     * 
     * @param sn
     * @param hardcode
     * @param ts
     * @return
     */
    public static String getRobotSign(String sn, String hardcode, String ts) {
        String robotSign = getDeviceSign(sn + hardcode, ts);
        robotSign = "Bearer " + robotSign;
        return robotSign;

    }

    public static String getDeviceSign(String sn, String hardcode, String ts) {
        String robotSign = getDeviceSign(sn + hardcode, ts);
        return robotSign;

    }

    public static String getRobotMac() {
        return (SystemUtil.getWlanMacAddress()).toLowerCase();
    }

    public static String getTs() {
        return (long) (System.currentTimeMillis() / 1000) + "";
    }

}
