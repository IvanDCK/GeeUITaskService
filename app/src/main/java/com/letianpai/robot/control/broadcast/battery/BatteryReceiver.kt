package com.letianpai.robot.control.broadcast.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.letianpai.robot.control.broadcast.LauncherInfoManager
import com.letianpai.robot.response.app.AppCmdResponser.Companion.getInstance
import com.letianpai.robot.response.ble.BleCmdResponser.Companion.getInstance
import com.letianpai.robot.response.identify.IdentifyCmdResponser.Companion.getInstance
import com.letianpai.robot.response.mi.MiIotCmdResponser.Companion.getInstance
import com.letianpai.robot.response.remote.RemoteCmdResponser.Companion.getInstance
import com.letianpai.robot.response.robotStatus.RobotStatusResponser.Companion.getInstance
import com.letianpai.robot.response.sensor.SensorCmdResponser.Companion.getInstance
import com.letianpai.robot.response.speech.SpeechCmdResponser.Companion.getInstance
import kotlin.math.max
import kotlin.math.min

/**
 * @author liujunbin
 */
class BatteryReceiver : BroadcastReceiver() {
    private var mContext: Context? = null

    override fun onReceive(context: Context, intent: Intent) {
        this.mContext = context
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(context, intent)
            Intent.ACTION_POWER_DISCONNECTED -> responseDisconnect()
            Intent.ACTION_POWER_CONNECTED -> responseConnect()
        }
    }

    private fun responseDisconnect() {
        val percent: Int = LauncherInfoManager.getInstance(mContext!!).batteryLevel
        LauncherInfoManager.getInstance(mContext!!).isChargingMode = false
        ChargingUpdateCallback.instance.setChargingStatus(false, percent)
    }

    private fun responseConnect() {
        val percent: Int = LauncherInfoManager.getInstance(mContext!!).batteryLevel
        LauncherInfoManager.getInstance(mContext!!).isChargingMode = true
        ChargingUpdateCallback.instance.setChargingStatus(true, percent)
    }

    private fun handleBatteryChanged(context: Context, intent: Intent) {
        val bundle = intent.extras

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (null == bundle) {
            return
        }

        // Get current battery level
        val current = bundle.getInt(BatteryManager.EXTRA_LEVEL, 0)
        // Get total power
        val total = bundle.getInt(BatteryManager.EXTRA_SCALE, 100)

        if (total == 0) {
            return
        }
        var percent = current * 100 / total
        percent = max(0.0, min(percent.toDouble(), 100.0)).toInt()
        LauncherInfoManager.getInstance(context).batteryLevel = percent

        //TODO When the battery is low, you need to do a shutdown reminder
//        if (percent < 4
////                && (! LauncherInfoManager.getInstance(context).hadShowCountdownDialog())
////                && (! LauncherInfoManager.getInstance(mContext).isChargingMode())
//        ){
////            LauncherInfoManager.getInstance(context).setHadShowCountdownDialog(true);
//            showCountdownDialog();
//        }
        val status =
            intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        if (percent <= 15) {
            showBatteryLowDialog(context)
        }

        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> responseCharging(context, percent)
            BatteryManager.BATTERY_STATUS_FULL -> if (isCharging(chargePlug)) {
                responseCharging(context, percent)
            } else {
                responseDisCharging(context, percent)
            }

            BatteryManager.BATTERY_STATUS_DISCHARGING -> {
                responseDisCharging(context, percent)
                responseDisCharging(context, percent, chargePlug)
            }

            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> {
                responseDisCharging(context, percent)
                responseDisCharging(context, percent, chargePlug)
            }

            BatteryManager.BATTERY_STATUS_UNKNOWN -> {
                responseDisCharging(context, percent)
                responseDisCharging(context, percent, chargePlug)
            }
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(ACTION_BATTERY_UPDATE))
    }

    /**
     * Response break
     * @param context
     * @param percent
     * @param chargePlug
     */
    private fun responseDisCharging(context: Context, percent: Int, chargePlug: Int) {
        //TODO Need to add repeat pop-up protection
        //TODO to be check and removed

        LauncherInfoManager.Companion.getInstance(context).isChargingMode = false
        ChargingUpdateCallback.instance.setChargingStatus(false, percent, chargePlug)
        hideChargingView()
    }

    /**
     * Response break
     * @param context
     * @param percent
     */
    private fun responseDisCharging(context: Context, percent: Int) {
        //TODO Need to add repeat pop-up protection
        //TODO to be check and removed

        LauncherInfoManager.getInstance(context).isChargingMode = false
        ChargingUpdateCallback.instance.setChargingStatus(false, percent)
        hideChargingView()
    }


    //
    //    /**
    //     * Response Charging
    //     *
    //     * @param context
    //     * @param percent
    //     */
    //    private void responseCharging(Context context, int percent, int chargePlug) {
    //        sendShowChargingDialog(context,percent);
    //        LauncherInfoManager.getInstance(context).setChargingMode(true);
    //        ChargingUpdateCallback.getInstance().setChargingStatus(true, percent);
    //        showChargingView();
    //        killThirdApps();
    //    }
    //
    /**
     * Response Charging
     *
     * @param context
     * @param percent
     */
    private fun responseCharging(context: Context, percent: Int) {
        sendShowChargingDialog(context, percent)
        LauncherInfoManager.getInstance(context).isChargingMode = true
        ChargingUpdateCallback.instance.setChargingStatus(true, percent)
        showChargingView()
        killThirdApps()
    }

    /**
     * Show charging pop-up
     * @param context
     * @param percent
     */
    private fun sendShowChargingDialog(context: Context, percent: Int) {
        // TODO Show the power-up pop-up window, here the channel interface, do not do UI display,
        // TODO only do the message passing, display layer to do this logic
    }

    /**
     * Show Low Power Popup
     *
     * @param mContext
     */
    private fun showBatteryLowDialog(mContext: Context) {
        //TODO Need to do a low point alert
        // TODO Optimist needs to do segmented power alert logic (20%, 10%, 5%)
    }

    /**
     *
     * @param context
     * @return
     */
    private fun getScreenStatus(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isScreenOn
    }

    private fun isCharging(chargePlug: Int): Boolean {
        return if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC || chargePlug == BatteryManager.BATTERY_PLUGGED_USB || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
        ) {
            true
        } else {
            false
        }
    }

    private fun showChargingView() {
        //TODO Show popup page in progress
        //TODO Here the channel interface, do not do UI display, only do the message passing, display layer to do this logic
    }

    private fun hideChargingView() {
        //TODO Here the channel interface, do not do UI display, only do the message passing, display layer to do this logic
    }

    /**
     * Countdown Shutdown Interface
     */
    fun showCountdownDialog() {
        //TODO Here the channel interface, do not do UI display, only do the message passing, display layer to do this logic
    }

    /**
     * Applications that kill limit lists
     */
    private fun killThirdApps() {
        //TODO Give the framework layer a message to kill the third-party process when charging.
    }


    companion object {
        const val ACTION_BATTERY_UPDATE: String = "com.renhejia.robot.action.battery_update"
    }
}
