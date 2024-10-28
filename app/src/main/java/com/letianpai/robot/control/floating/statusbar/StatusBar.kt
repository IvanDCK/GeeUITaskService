package com.letianpai.robot.control.floating.statusbar

import android.app.ActivityManager
import android.app.ActivityManager.AppTask
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.letianpai.robot.components.network.nets.WIFIConnectionManager
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.broadcast.NetWorkChangeReceiver
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.broadcast.timer.TimerKeeperCallback
import com.letianpai.robot.control.broadcast.timer.TimerKeeperCallback.TimerKeeperUpdateListener
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback.NetworkChangingUpdateListener
import com.letianpai.robot.control.callback.RobotCommandWordsCallback
import com.letianpai.robot.control.callback.RobotCommandWordsCallback.RobotCommandWordsUpdateListener
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.nets.GeeUINetResponseManager
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.response.app.AppCmdResponser.Companion.getInstance
import com.letianpai.robot.response.ble.BleCmdResponser.Companion.getInstance
import com.letianpai.robot.response.identify.IdentifyCmdResponser.Companion.getInstance
import com.letianpai.robot.response.mi.MiIotCmdResponser.Companion.getInstance
import com.letianpai.robot.response.remote.RemoteCmdResponser.Companion.getInstance
import com.letianpai.robot.response.robotStatus.RobotStatusResponser
import com.letianpai.robot.response.robotStatus.RobotStatusResponser.Companion.getInstance
import com.letianpai.robot.response.sensor.SensorCmdResponser.Companion.getInstance
import com.letianpai.robot.response.speech.SpeechCmdResponser.Companion.getInstance
import com.letianpai.robot.taskservice.R
import com.letianpai.robot.taskservice.dispatch.statusbar.StatusBarUpdateCallback
import com.letianpai.robot.taskservice.dispatch.statusbar.StatusBarUpdateCallback.StatusBarChangeListener
import java.lang.ref.WeakReference

/**
 * @author liujunbin
 */
class StatusBar : RelativeLayout {
    private var mContext: Context? = null
    private var noNetworkImage: ImageView? = null

    // private TextView bottomText;
    private var batteryCharging: BatteryCharging? = null

    //    private LinearLayout rootStatus;
    private var rootStatus: RelativeLayout? = null
    private var rlTitlePart: LinearLayout? = null
    private var llTitlePart: LinearLayout? = null
    private var emptyView: View? = null

    private var mHandler: UpdateViewHandler? = null
    private var localChargingStatus: Boolean = false

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        this.mContext = context
        mHandler = UpdateViewHandler(context)
        inflate(mContext, R.layout.robot_status_bar, this)

        initView()
        showWifiStatus()
        showChargingStatus()
        addUpdateTextListeners()
        addNetworkChangeListeners()
        addTimerUpdateCallback()
        addWordsChangeCallback()
        GeeUINetResponseManager.getInstance(mContext!!).displayInfo
    }


    private fun addWordsChangeCallback() {
        RobotCommandWordsCallback.instance
            .setRobotCommandWordsUpdateListener(object : RobotCommandWordsUpdateListener {
                override fun showBattery(showBattery: Boolean) {
                    GeeUILogUtils.logi(
                        TAG,
                        "RobotCommandWordsUpdateListener--showBattery::showBattery:$showBattery"
                    )
                    showBatteryStatus(showBattery)
                }
            })
    }

    private fun showChargingStatus() {
        val isCharging: Boolean = ChargingUpdateCallback.instance.isCharging
        val battery: Int = ChargingUpdateCallback.instance.battery
        GeeUILogUtils.logi(
            TAG,
            "RobotCommandWordsUpdateListener--showChargingStatus::isCharging:$isCharging--battery::$battery"
        )
        if (isCharging) {
            setCharging(battery)
        } else if (battery < ChargingUpdateCallback.LOW_BATTERY_NOTICE) {
            setBatteryLow(battery)
        } else {
            batteryCharging!!.visibility = GONE
        }
    }

    private fun showWifiStatus() {
        val status: Boolean = WIFIConnectionManager.isWifiConnected()
        GeeUILogUtils.logi(TAG, "setNoNetworkStatus_4_--status: $status")
        if (status) {
            hideNoNetworkStatus()
        } else {
            setNoNetworkStatus()
        }
    }

    private fun addUpdateTextListeners() {
        StatusBarUpdateCallback.instance.setStatusBarTextChangeListener(object :
            StatusBarChangeListener {
            override fun onStatusBarTextChanged(content: String?) {
                showText(content)
            }
        })
    }

    private fun addTimerUpdateCallback() {
        TimerKeeperCallback.instance
            .registerTimerKeeperUpdateListener(object : TimerKeeperUpdateListener {
                override fun onTimerKeeperUpdateReceived(hour: Int, minute: Int) {
                    updateTime()
                }
            })
    }


    private fun addNetworkChangeListeners() {
        NetworkChangingUpdateCallback.instance
            .registerChargingStatusUpdateListener(object : NetworkChangingUpdateListener {
                override fun onNetworkChargingUpdateReceived(networkType: Int, networkStatus: Int) {
                    GeeUILogUtils.logi(TAG, "networkType: " + networkType)
                    if (networkType == NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED) {
                        //Disconnected with a 1-minute delay before displaying the WiFi icon Disconnected
                        this@StatusBar.postDelayed(object : Runnable {
                            override fun run() {
                                setNoNetworkStatus()
                            }
                        }, 60000)

                        // setNoNetworkStatus();
                    } else {
                        hideNoNetworkStatus()
                    }
                }
            })

        ChargingUpdateCallback.instance.registerChargingStatusUpdateListener(object :
            ChargingUpdateCallback.ChargingUpdateListener {
            override fun onChargingUpdateReceived(changingStatus: Boolean, percent: Int) {
                if ((RobotModeManager.getInstance(mContext!!)
                        .robotMode == ViewModeConsts.VM_POWER_ON_CHARGING)
                ) {
                    return
                }
                GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_changingStatus: " + changingStatus)
                GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_percent: " + percent)
                GeeUILogUtils.logi(
                    TAG,
                    "ChargingUpdateCallback_SystemUtil.getRobotActivateStatus(): " + SystemUtil.getRobotActivateStatus()
                )
                if (changingStatus) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0010 ========== ")
                    if (RobotModeManager.getInstance(mContext!!)
                            .isShowChargingInRobotMode
                    ) {
                        setCharging(percent)
                    } else if (LetianpaiFunctionUtil.isSpeechOnTheTop(mContext!!) || LetianpaiFunctionUtil.isLexOnTheTop(
                            mContext!!
                        ) || LetianpaiFunctionUtil.isRobotOnTheTop(mContext!!) || RobotModeManager.getInstance(
                            mContext!!
                        ).isCommonRobotMode || RobotModeManager.getInstance(mContext!!)
                            .isSleepMode
                    ) {
                        if (batteryCharging != null) {
                            batteryCharging!!.setVisibility(GONE)
                        }
                    } else {
                        setCharging(percent)
                    }
                    //If not activated, return
                    if (!SystemUtil.getRobotActivateStatus()) {
                        return
                    }

                    //                    if (changingStatus && (changingStatus != localChargingStatus) && SystemUtil.getRobotActivateStatus()) {
                    if (changingStatus && (changingStatus != localChargingStatus)) {
                        GeeUILogUtils.logi(
                            TAG,
                            "ChargingUpdateCallback_=========== 0012 ========== localChargingStatus: " + localChargingStatus
                        )
                        GeeUILogUtils.logi(
                            TAG,
                            "ChargingUpdateCallback_=========== 0012 ========== changingStatus: $changingStatus"
                        )
                        LetianpaiFunctionUtil.responseCharging(mContext!!)
                    }
                    localChargingStatus = changingStatus
                } else if (!changingStatus && localChargingStatus) {
                    GeeUILogUtils.logi(
                        TAG,
                        "ChargingUpdateCallback_=========== 0013 ========== localChargingStatus: $localChargingStatus"
                    )
                    GeeUILogUtils.logi(
                        TAG,
                        "ChargingUpdateCallback_=========== 0013 ========== !changingStatus: " + !changingStatus
                    )
                    responseDischarging()
                    localChargingStatus = changingStatus
                } else if (percent < ChargingUpdateCallback.Companion.LOW_BATTERY_SHUTDOWN_STANDARD && changingStatus) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0014 ========== ")
                    setCharging(percent)
                } else if (percent < ChargingUpdateCallback.Companion.LOW_BATTERY_NOTICE) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0015 ========== ")
                    setBatteryLow(percent)
                } else {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0016 ========== ")
                    batteryCharging!!.setVisibility(GONE)
                }
            }

            override fun onChargingUpdateReceived(
                changingStatus: Boolean,
                percent: Int,
                chargePlug: Int
            ) {
            }
        })
    }

    //    private void responseDischarging() {
    //        batteryCharging.setVisibility(View.GONE);
    //        Log.e("letianpai_test000", "RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot(): " + RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot());
    //        if (RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot()) {
    //            Log.e("letianpai_test000", "RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot(): =========== 1 ========== ");
    //            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1);
    //            Log.e("letianpai_test000", "RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot(): =========== 2 ========== ");
    //        }
    //    }
    private fun responseDischarging() {
        batteryCharging!!.setVisibility(GONE)
        //        if (RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot() || RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging()) {
        GeeUILogUtils.logi(
            TAG,
            "RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging(): " + RobotModeManager.getInstance(
                mContext!!
            ).isRobotModeBeforeCharging()
        )
        GeeUILogUtils.logi(
            TAG,
            "RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging()==== 2 ====: " + RobotModeManager.getInstance(
                mContext!!
            ).isRobotModeBeforeCharging()
        )
        if (SystemUtil.getRobotActivateStatus() && RobotModeManager.getInstance(
                mContext!!
            ).isRobotModeBeforeCharging()
        ) {
            GeeUILogUtils.logi(
                TAG,
                "RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging() ==== 3 ====: " + RobotModeManager.getInstance(
                    mContext!!
                ).isRobotModeBeforeCharging()
            )
            //            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1);
//            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1);
            LetianpaiFunctionUtil.responseDisCharging(mContext!!)
            RobotModeManager.Companion.getInstance(mContext!!).setRobotModeBeforeChargingOn(false)
        }

        //        if (SystemUtil.getRobotActivateStatus()){
//            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_TIME);
//        }
    }

    private fun initView() {
        rootStatus = findViewById(R.id.root_status)
        llTitlePart = findViewById(R.id.ll_title_part)
        //        rootStatus.getBackground().setAlpha(256);
        noNetworkImage = findViewById(R.id.title_part)
        // bottomText = findViewById(R.id.bottom_part);
        batteryCharging = findViewById(R.id.bcCharging)
        emptyView = findViewById(R.id.empty_view)
        rlTitlePart = findViewById(R.id.rl_title_part)

        // noWiFiNoticeView = findViewById(R.id.noWiFiNoticeView);
        rlTitlePart!!.setOnClickListener(object : OnClickListener {
            override fun onClick(v: View) {
//                if (SystemUtil.getRobotStatus() && !NetWorkChangeReceiver.isWifiConnected(mContext)) {
//                    openWifiConnectView();
//                }

                if (SystemUtil.getTitleTouchStatus() && SystemUtil.getRobotActivateStatus()) {
                    if (RobotStatusResponser.getInstance(mContext!!).isNoNeedResponseMode) {
                        return
                    }
                    try {
//                        openGeeUISettings();
//                        if (!LetianpaiFunctionUtil.isRobotOnTheTop(mContext)) {
                        openGeeUIDesktop()

                        //                        }
                    } catch (e: Exception) {
                    }
                } else {
                    if (!LetianpaiFunctionUtil.isWifiConnectorOnTheTop(mContext!!)) {
                        openGeeUISettings()
                    }
                }
                //                if (SystemUtil.getTitleTouchStatus()){
//                    if (LetianpaiFunctionUtil.isDesktopAppOnTheTop(mContext)){
//                        Log.e("letianpai","hahahahahahahahahahahahahahahahahahahahahahahah ============================================");
//                        killAppByPackageName(mContext.getApplicationContext(), PackageConsts.PACKAGE_NAME_DESKTOP);
//                    }else{
//                        try {
////                        openGeeUISettings();
//                            openGeeUIDesktop();
//                        }catch (Exception e){
//
//                        }
//                    }
//
//
//                }
            }
        })
    }

    private fun openWifiConnectView() {
        val packageName: String = "com.letianpai.robot.wificonnet"
        val activityName: String = "com.letianpai.robot.wificonnet.MainActivity"
        val intent: Intent = Intent()
        intent.putExtra(OPEN_FROM, OPEN_FROM_TITLE)
        intent.setComponent(ComponentName(packageName, activityName))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        mContext!!.startActivity(intent)
    }

    private fun openGeeUISettings() {
        val packageName: String = "com.robot.geeui.setting"
        val activityName: String = "com.robot.geeui.setting.MainActivity"
        val intent: Intent = Intent()
        intent.putExtra(OPEN_FROM, OPEN_FROM_TITLE)
        intent.setComponent(ComponentName(packageName, activityName))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        mContext!!.startActivity(intent)
    }

    private fun openGeeUIDesktop() {
        val topPackageName: String? = LetianpaiFunctionUtil.getTopAppPackageName(mContext!!)
        val robotMode: Int = RobotModeManager.getInstance(mContext!!).robotMode
        val packageName = "com.letianpai.robot.desktop"
        val activityName = "com.letianpai.robot.desktop.MainActivity"
        val intent = Intent()
        intent.setComponent(ComponentName(packageName, activityName))
        intent.putExtra("package", topPackageName)
        intent.putExtra("mode", robotMode)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        mContext!!.startActivity(intent)
    }


    fun setNoNetworkStatus() {
        GeeUILogUtils.logi(
            TAG,
            "setNoNetworkStatus_1_---SystemUtil.getRobotStatus(): " + SystemUtil.getRobotStatus()
        )
        GeeUILogUtils.logi(
            TAG,
            "setNoNetworkStatus_2_---!NetWorkChangeReceiver.isWifiConnected(mContext): " + !NetWorkChangeReceiver.Companion.isWifiConnected(
                mContext
            )
        )
        if (SystemUtil.getRobotStatus() && !NetWorkChangeReceiver.isWifiConnected(mContext)) {
            // noWiFiNoticeView.setVisibility(View.VISIBLE);
            noNetworkImage!!.setVisibility(VISIBLE)
            //You don't need to enter the time when you disconnect from the network,
            // it shows whichever interface you're on
            // LetianpaiFunctionUtil.openTime(mContext);
        }
    }

    private fun hideNoNetworkStatus() {
        noNetworkImage!!.setVisibility(GONE)
        //         noWiFiNoticeView.setVisibility(View.GONE);
//        bottomText.setVisibility(View.GONE);
    }

    //    public void setBatteryLow(int percent) {
    ////        topImage.setVisibility(View.GONE);
    //        batteryCharging.setVisibility(View.VISIBLE);
    //        batteryCharging.setBatteryLow(percent);
    //        bottomText.setVisibility(View.VISIBLE);
    //        bottomText.setText(R.string.battery_low);
    //    }
    fun setBatteryLow(percent: Int) {
        if (RobotModeManager.Companion.getInstance(mContext!!).isCommonRobotMode) {
            // bottomText.setVisibility(View.GONE);
            // bottomText.setText("");
        } else {
            if (LetianpaiFunctionUtil.isVideoCallRunning(mContext!!) || LetianpaiFunctionUtil.isRobotAppRunning(
                    mContext!!
                )
            ) {
                // bottomText.setVisibility(View.GONE);
            } else {
                // bottomText.setVisibility(View.VISIBLE);
            }
            batteryCharging!!.setVisibility(VISIBLE)
            batteryCharging!!.setBatteryLow(percent.toFloat())
            //            bottomText.setText(R.string.battery_low);
        }
    }

    fun setCharging(percent: Int) {
        batteryCharging!!.setVisibility(VISIBLE)
        batteryCharging!!.setBatteryLevel(percent.toFloat())
    }

    fun setDisplayTime() {
        // bottomText.setVisibility(View.VISIBLE);
        setBottomText(TimeUtil.correctTime)
    }

    /**
     * 设置底部文案
     *
     * @param content
     */
    fun setBottomText(content: String) {
        GeeUILogUtils.logi(TAG, "setBottomText: " + content)
        // bottomText.setText(content);
    }

    /**
     * 设置底部文案
     */
    fun setBottomTextVisibility(isShow: Boolean) {
        GeeUILogUtils.logi(TAG, "======== 1 ======== setBottomTextVisibility_isShow " + isShow)
        if (isShow) {
            // bottomText.setVisibility(View.VISIBLE);
        } else {
            // bottomText.setVisibility(View.INVISIBLE);
        }
    }

    private inner class UpdateViewHandler(context: Context) : Handler() {
        private val context: WeakReference<Context>

        init {
            this.context = WeakReference(context)
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                SHOW_TIME -> {
                    GeeUILogUtils.logi("letianpai_statusbar", "======== 10 ========")
                    if (msg.obj != null) {
                        setBottomText((msg.obj) as String)
                    }
                }

                UPDATE_TIME -> {}
                UPDATE_BOTTOM_TEXT -> if (msg.obj != null) {
                    GeeUILogUtils.logi(
                        TAG,
                        "UpdateViewHandler:handleMessage:UPDATE_BOTTOM_TEXT: " + msg.obj as Boolean
                    )
                    setBottomTextVisibility(msg.obj as Boolean)
                }

                UPDATE_SHOW_BATTERY -> {
                    if (batteryCharging == null) {
                        return
                    }
                    val isCharging: Boolean =
                        ChargingUpdateCallback.instance.isCharging
                    val showBattery: Boolean = msg.obj as Boolean
                    val isShowChargingInRobotMode: Boolean = RobotModeManager.getInstance(
                        mContext!!
                    ).isShowChargingInRobotMode
                    val isInSleepMode: Boolean = RobotModeManager.getInstance(
                        mContext!!
                    ).robotMode == ViewModeConsts.VM_SLEEP_MODE
                    if (isShowChargingInRobotMode && isCharging && !isInSleepMode) {
                        batteryCharging!!.visibility = VISIBLE
                    } else if (showBattery && isCharging) {
                        batteryCharging!!.visibility = VISIBLE
                    } else if (!showBattery && isCharging) {
                        batteryCharging!!.visibility = GONE
                    } else {
                        batteryCharging!!.visibility = GONE
                    }
                    GeeUILogUtils.logi(
                        TAG,
                        "UpdateViewHandler:handleMessage:isCharging: $isCharging--showBattery:$showBattery--isShowChargingInRobotMode:$isShowChargingInRobotMode--isInSleepMode:$isInSleepMode"
                    )
                }
            }
        }
    }

    fun showText(text: String?) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return
        }
        val message = Message()
        message.what = SHOW_TIME
        message.obj = text
        mHandler!!.sendMessage(message)
    }

    // public void updateBottomText(boolean isShowText) {
    //     Log.e(TAG, " updateBottomText::isShowText: " + isShowText +"--SystemUtil.getRobotActivateStatus()--"+SystemUtil.getRobotActivateStatus());
    //     if (isShowText && !SystemUtil.getRobotActivateStatus()) {
    //         return;
    //     }
    //     Message message = new Message();
    //     message.what = UPDATE_BOTTOM_TEXT;
    //     message.obj = isShowText;
    //     mHandler.sendMessage(message);
    // }
    fun showBatteryStatus(isShowBattery: Boolean) {
        GeeUILogUtils.logi(TAG, " showBatteryStatus::isShowBattery: $isShowBattery")
        val message = Message()
        message.what = UPDATE_SHOW_BATTERY
        message.obj = isShowBattery
        mHandler!!.sendMessage(message)
    }

    fun updateTime() {
        val message = Message()
        message.what = UPDATE_TIME
        mHandler!!.sendMessage(message)
    }

    fun setAlarmText() {
        val message = Message()
        message.what = SET_ALARM_TEXT
        mHandler!!.sendMessage(message)
    }

    companion object {
        private val TAG: String = "StatusBar"
        private const val SHOW_TIME: Int = 110
        private const val UPDATE_TIME: Int = 111
        private const val UPDATE_BOTTOM_TEXT: Int = 112
        private const val UPDATE_SHOW_BATTERY: Int = 115
        private const val SET_ALARM_TEXT: Int = 116

        // private NoWiFiNoticeView noWiFiNoticeView;
        private const val OPEN_FROM: String = "from"
        private const val OPEN_FROM_TITLE: String = "from_title"


        fun killAppByPackageName(context: Context, packageName: String) {
            val activityManager: ActivityManager? =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            if (activityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val appTasks: List<AppTask> = activityManager.getAppTasks()
                    for (appTask: AppTask in appTasks) {
                        if (appTask.taskInfo.baseIntent.component!!
                                .packageName == packageName
                        ) {
                            appTask.finishAndRemoveTask()
                            break
                        }
                    }
                } else {
                    val appProcesses: List<RunningAppProcessInfo> =
                        activityManager.runningAppProcesses
                    for (appProcess: RunningAppProcessInfo in appProcesses) {
                        if (appProcess.processName == packageName) {
                            activityManager.killBackgroundProcesses(packageName)
                            break
                        }
                    }
                }
            }
        }
    }
}
