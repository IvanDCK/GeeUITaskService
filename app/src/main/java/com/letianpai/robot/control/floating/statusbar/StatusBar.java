package com.letianpai.robot.control.floating.statusbar;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.letianpai.robot.components.locale.LocaleUtils;
import com.letianpai.robot.components.network.nets.WIFIConnectionManager;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.broadcast.NetWorkChangeReceiver;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.broadcast.timer.TimerKeeperCallback;
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback;
import com.letianpai.robot.control.callback.RobotCommandWordsCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.nets.GeeUINetResponseManager;
import com.letianpai.robot.control.service.DispatchService;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.response.robotStatus.RobotStatusResponser;
import com.letianpai.robot.taskservice.R;
import com.letianpai.robot.taskservice.dispatch.statusbar.StatusBarUpdateCallback;
import com.renhejia.robot.commandlib.consts.PackageConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.parser.tips.Tips;
import com.renhejia.robot.commandlib.parser.tips.TipsName;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

/**
 * @author liujunbin
 */
public class StatusBar extends RelativeLayout {

    private static  String TAG = "StatusBar";
    private Context mContext;
    private ImageView noNetworkImage;
    // private TextView bottomText;
    private BatteryCharging batteryCharging;
    //    private LinearLayout rootStatus;
    private RelativeLayout rootStatus;
    private LinearLayout rlTitlePart;
    private LinearLayout llTitlePart;
    private View emptyView;

    private UpdateViewHandler mHandler;
    private static final int SHOW_TIME = 110;
    private static final int UPDATE_TIME = 111;
    private static final int UPDATE_BOTTOM_TEXT = 112;
    private static final int UPDATE_SHOW_BATTERY = 115;
    private static final int SET_ALARM_TEXT = 116;
    private boolean localChargingStatus;
    // private NoWiFiNoticeView noWiFiNoticeView;
    private static final String OPEN_FROM = "from";
    private static final String OPEN_FROM_TITLE = "from_title";


    public StatusBar(Context context) {
        super(context);
        init(context);
    }

    public StatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatusBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        mHandler = new UpdateViewHandler(context);
        inflate(mContext, R.layout.robot_status_bar, this);

        initView();
        showWifiStatus();
        showChargingStatus();
        addUpdateTextListeners();
        addNetworkChangeListeners();
        addTimerUpdateCallback();
        addWordsChangeCallback();
        GeeUINetResponseManager.getInstance(mContext).getDisplayInfo();
    }


    private void addWordsChangeCallback() {
        RobotCommandWordsCallback.getInstance().setRobotCommandWordsUpdateListener(new RobotCommandWordsCallback.RobotCommandWordsUpdateListener() {

            @Override
            public void showBattery(boolean showBattery) {
                GeeUILogUtils.logi(TAG, "RobotCommandWordsUpdateListener--showBattery::showBattery:" + showBattery);
                showBatteryStatus(showBattery);
            }
        });
    }

    private void showChargingStatus() {
        boolean isCharging = ChargingUpdateCallback.getInstance().isCharging();
        int battery = ChargingUpdateCallback.getInstance().getBattery();
        GeeUILogUtils.logi(TAG, "RobotCommandWordsUpdateListener--showChargingStatus::isCharging:" + isCharging +"--battery::"+battery);
        if (isCharging) {
            setCharging(battery);
        } else if (battery < ChargingUpdateCallback.LOW_BATTERY_NOTICE) {
            setBatteryLow(battery);
        } else {
            batteryCharging.setVisibility(View.GONE);
        }
    }

    private void showWifiStatus() {
        boolean status = WIFIConnectionManager.getInstance(mContext).isWifiConnected();
        GeeUILogUtils.logi(TAG, "setNoNetworkStatus_4_--status: " + status);
        if (status) {
            hideNoNetworkStatus();
        } else {
            setNoNetworkStatus();
        }
    }

    private void addUpdateTextListeners() {
        StatusBarUpdateCallback.getInstance().setStatusBarTextChangeListener(new StatusBarUpdateCallback.StatusBarChangeListener() {
            @Override
            public void onStatusBarTextChanged(String content) {
                showText(content);
            }
        });
    }

    private void addTimerUpdateCallback() {
        TimerKeeperCallback.getInstance().registerTimerKeeperUpdateListener(new TimerKeeperCallback.TimerKeeperUpdateListener() {
            @Override
            public void onTimerKeeperUpdateReceived(int hour, int minute) {
                updateTime();
            }
        });
    }


    private void addNetworkChangeListeners() {
        NetworkChangingUpdateCallback.getInstance().registerChargingStatusUpdateListener(new NetworkChangingUpdateCallback.NetworkChangingUpdateListener() {
            @Override
            public void onNetworkChargingUpdateReceived(int networkType, int networkStatus) {
                GeeUILogUtils.logi(TAG, "networkType: " + networkType);
                if (networkType == NetworkChangingUpdateCallback.NETWORK_TYPE_DISABLED) {
                    //断网了延时1分钟，在显示WiFi图标断开
                    StatusBar.this.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setNoNetworkStatus();
                        }
                    },60000);

                    // setNoNetworkStatus();
                } else {
                    hideNoNetworkStatus();
                }
            }
        });

        ChargingUpdateCallback.getInstance().registerChargingStatusUpdateListener(new ChargingUpdateCallback.ChargingUpdateListener() {
            @Override
            public void onChargingUpdateReceived(boolean changingStatus, int percent) {
                if ((RobotModeManager.getInstance(mContext).getRobotMode() == ViewModeConsts.VM_POWER_ON_CHARGING)) {
                    return;
                }
                GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_changingStatus: " + changingStatus);
                GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_percent: " + percent);
                GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_SystemUtil.getRobotActivateStatus(): " + SystemUtil.getRobotActivateStatus());
                if (changingStatus) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0010 ========== ");
                    if (RobotModeManager.getInstance(mContext).isShowChargingInRobotMode()) {
                        setCharging(percent);
                    } else if (LetianpaiFunctionUtil.isSpeechOnTheTop(mContext) || LetianpaiFunctionUtil.isLexOnTheTop(mContext) || LetianpaiFunctionUtil.isRobotOnTheTop(mContext) || RobotModeManager.getInstance(mContext).isCommonRobotMode() || RobotModeManager.getInstance(mContext).isSleepMode()) {
                        if (batteryCharging != null) {
                            batteryCharging.setVisibility(View.GONE);
                        }
                    } else {
                        setCharging(percent);
                    }
                    //如果没有激活，返回
                    if (!SystemUtil.getRobotActivateStatus()){
                        return;
                    }

//                    if (changingStatus && (changingStatus != localChargingStatus) && SystemUtil.getRobotActivateStatus()) {
                    if (changingStatus && (changingStatus != localChargingStatus)) {
                        GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0012 ========== localChargingStatus: "+ localChargingStatus);
                        GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0012 ========== changingStatus: "+ changingStatus);
                        LetianpaiFunctionUtil.responseCharging(mContext);
                    }
                    localChargingStatus = changingStatus;
                } else if (!changingStatus && localChargingStatus) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0013 ========== localChargingStatus: "+ localChargingStatus);
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0013 ========== !changingStatus: "+ !changingStatus);
                    responseDischarging();
                    localChargingStatus = changingStatus;

                } else if (percent < ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD && changingStatus) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0014 ========== ");
                    setCharging(percent);
                } else if (percent < ChargingUpdateCallback.LOW_BATTERY_NOTICE) {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0015 ========== ");
                    setBatteryLow(percent);
                } else {
                    GeeUILogUtils.logi(TAG, "ChargingUpdateCallback_=========== 0016 ========== ");
                    batteryCharging.setVisibility(View.GONE);
                }
            }

            @Override
            public void onChargingUpdateReceived(boolean changingStatus, int percent, int chargePlug) {

            }
        });
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

    private void responseDischarging() {
        batteryCharging.setVisibility(View.GONE);
//        if (RobotModeManager.getInstance(mContext).getRobotModeBeforeChargingIsRobot() || RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging()) {
        GeeUILogUtils.logi(TAG,"RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging(): "+ RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging());
        GeeUILogUtils.logi(TAG,"RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging()==== 2 ====: "+ RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging());
        if (SystemUtil.getRobotActivateStatus() && RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging()) {
            GeeUILogUtils.logi(TAG,"RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging() ==== 3 ====: "+ RobotModeManager.getInstance(mContext).isRobotModeBeforeCharging());
//            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1);
//            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1);
            LetianpaiFunctionUtil.responseDisCharging(mContext);
            RobotModeManager.getInstance(mContext).setRobotModeBeforeChargingOn(false);
        }
//        if (SystemUtil.getRobotActivateStatus()){
//            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_TIME);
//        }

    }

    private void initView() {
        rootStatus = findViewById(R.id.root_status);
        llTitlePart = findViewById(R.id.ll_title_part);
//        rootStatus.getBackground().setAlpha(256);
        noNetworkImage = findViewById(R.id.title_part);
        // bottomText = findViewById(R.id.bottom_part);
        batteryCharging = findViewById(R.id.bcCharging);
        emptyView = findViewById(R.id.empty_view);
        rlTitlePart = findViewById(R.id.rl_title_part);
        // noWiFiNoticeView = findViewById(R.id.noWiFiNoticeView);

        rlTitlePart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (SystemUtil.getRobotStatus() && !NetWorkChangeReceiver.isWifiConnected(mContext)) {
//                    openWifiConnectView();
//                }

                if (SystemUtil.getTitleTouchStatus() && SystemUtil.getRobotActivateStatus()){
                    if (RobotStatusResponser.getInstance(mContext).isNoNeedResponseMode()){
                        return;
                    }
                    try {
//                        openGeeUISettings();
//                        if (!LetianpaiFunctionUtil.isRobotOnTheTop(mContext)) {
                            openGeeUIDesktop();
//                        }

                    }catch (Exception e){

                    }

                }else{
                    if (!LetianpaiFunctionUtil.isWifiConnectorOnTheTop(mContext)){
                        openGeeUISettings();
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
        });
    }

    private void openWifiConnectView() {
        String packageName = "com.letianpai.robot.wificonnet";
        String activityName = "com.letianpai.robot.wificonnet.MainActivity";
        Intent intent = new Intent();
        intent.putExtra(OPEN_FROM,OPEN_FROM_TITLE);
        intent.setComponent(new ComponentName(packageName, activityName));
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }

    private void openGeeUISettings() {
        String packageName = "com.robot.geeui.setting";
        String activityName = "com.robot.geeui.setting.MainActivity";
        Intent intent = new Intent();
        intent.putExtra(OPEN_FROM,OPEN_FROM_TITLE);
        intent.setComponent(new ComponentName(packageName, activityName));
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }
    public static void killAppByPackageName(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
                for (ActivityManager.AppTask appTask : appTasks) {
                    if (appTask.getTaskInfo().baseIntent.getComponent().getPackageName().equals(packageName)) {
                        appTask.finishAndRemoveTask();
                        break;
                    }
                }
            } else {
                List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess.processName.equals(packageName)) {
                        activityManager.killBackgroundProcesses(packageName);
                        break;
                    }
                }
            }
        }
    }

    private void openGeeUIDesktop() {
        String topPackageName = LetianpaiFunctionUtil.getTopAppPackageName(mContext);
        int robotMode = RobotModeManager.getInstance(mContext).getRobotMode();
        String packageName = "com.letianpai.robot.desktop";
        String activityName = "com.letianpai.robot.desktop.MainActivity";
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, activityName));
        intent.putExtra("package",topPackageName);
        intent.putExtra("mode",robotMode);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }


    public void setNoNetworkStatus() {
        GeeUILogUtils.logi(TAG, "setNoNetworkStatus_1_---SystemUtil.getRobotStatus(): " + SystemUtil.getRobotStatus());
        GeeUILogUtils.logi(TAG, "setNoNetworkStatus_2_---!NetWorkChangeReceiver.isWifiConnected(mContext): " + !NetWorkChangeReceiver.isWifiConnected(mContext));
        if (SystemUtil.getRobotStatus() && !NetWorkChangeReceiver.isWifiConnected(mContext)) {
            // noWiFiNoticeView.setVisibility(View.VISIBLE);
            noNetworkImage.setVisibility(View.VISIBLE);
            //断网了不需要进入时间，在哪个界面就显示哪个界面
            // LetianpaiFunctionUtil.openTime(mContext);
        }
    }

    private void hideNoNetworkStatus() {
        noNetworkImage.setVisibility(View.GONE);
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

    public void setBatteryLow(int percent) {
        if (RobotModeManager.getInstance(mContext).isCommonRobotMode()) {
            // bottomText.setVisibility(View.GONE);
            // bottomText.setText("");

        } else {
            if (LetianpaiFunctionUtil.isVideoCallRunning(mContext) || LetianpaiFunctionUtil.isRobotAppRunning(mContext)){
                // bottomText.setVisibility(View.GONE);
            }else{
                // bottomText.setVisibility(View.VISIBLE);
            }
            batteryCharging.setVisibility(View.VISIBLE);
            batteryCharging.setBatteryLow(percent);
//            bottomText.setText(R.string.battery_low);
        }

    }

    public void setCharging(int percent) {
        batteryCharging.setVisibility(View.VISIBLE);
        batteryCharging.setBatteryLevel(percent);
    }

    public void setDisplayTime() {
        // bottomText.setVisibility(View.VISIBLE);
        setBottomText(TimeUtil.getCorrectTime());
    }

    /**
     * 设置底部文案
     *
     * @param content
     */
    public void setBottomText(String content) {
        GeeUILogUtils.logi(TAG, "setBottomText: " + content);
        // bottomText.setText(content);
    }

    /**
     * 设置底部文案
     */
    public void setBottomTextVisibility(boolean isShow) {
        GeeUILogUtils.logi(TAG, "======== 1 ======== setBottomTextVisibility_isShow " + isShow);
        if (isShow) {
            // bottomText.setVisibility(View.VISIBLE);
        } else {
            // bottomText.setVisibility(View.INVISIBLE);
        }
    }

    private class UpdateViewHandler extends android.os.Handler {
        private final WeakReference<Context> context;

        public UpdateViewHandler(Context context) {
            this.context = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOW_TIME:
                    GeeUILogUtils.logi("letianpai_statusbar", "======== 10 ========");
                    if (msg.obj != null) {
                        setBottomText((String) (msg.obj));
                    }
                    break;

                case UPDATE_TIME:
//                    setBottomText(TimeUtil.getCorrectTime());
                    break;

                case UPDATE_BOTTOM_TEXT:
                    if (msg.obj != null) {
                        GeeUILogUtils.logi(TAG, "UpdateViewHandler:handleMessage:UPDATE_BOTTOM_TEXT: "+(boolean) msg.obj);
                        setBottomTextVisibility((boolean) msg.obj);
                    }

                    break;
                case UPDATE_SHOW_BATTERY:
                    if (batteryCharging == null) {
                        return;
                    }
                    boolean isCharging = ChargingUpdateCallback.getInstance().isCharging();
                    boolean showBattery = (boolean) msg.obj;
                    boolean isShowChargingInRobotMode = RobotModeManager.getInstance(mContext).isShowChargingInRobotMode();
                    boolean isInSleepMode = RobotModeManager.getInstance(mContext).getRobotMode() == ViewModeConsts.VM_SLEEP_MODE;
                    if (isShowChargingInRobotMode && isCharging && !isInSleepMode) {
                        batteryCharging.setVisibility(View.VISIBLE);
                    } else if (showBattery && isCharging) {
                        batteryCharging.setVisibility(View.VISIBLE);
                    } else if (!showBattery && isCharging) {
                        batteryCharging.setVisibility(View.GONE);
                    } else {
                        batteryCharging.setVisibility(View.GONE);
                    }
                    GeeUILogUtils.logi(TAG, "UpdateViewHandler:handleMessage:isCharging: "+isCharging + "--showBattery:"+showBattery + "--isShowChargingInRobotMode:"+isShowChargingInRobotMode +"--isInSleepMode:"+isInSleepMode);
                    break;
            }
        }
    }

    public void showText(String text) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return;
        }
        Message message = new Message();
        message.what = SHOW_TIME;
        message.obj = text;
        mHandler.sendMessage(message);
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

    public void showBatteryStatus(boolean isShowBattery) {
        GeeUILogUtils.logi(TAG, " showBatteryStatus::isShowBattery: " + isShowBattery);
        Message message = new Message();
        message.what = UPDATE_SHOW_BATTERY;
        message.obj = isShowBattery;
        mHandler.sendMessage(message);
    }

    public void updateTime() {
        Message message = new Message();
        message.what = UPDATE_TIME;
        mHandler.sendMessage(message);
    }

    public void setAlarmText() {
        Message message = new Message();
        message.what = SET_ALARM_TEXT;
        mHandler.sendMessage(message);
    }
}
