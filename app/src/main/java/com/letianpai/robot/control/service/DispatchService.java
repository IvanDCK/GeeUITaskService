package com.letianpai.robot.control.service;

import static com.letianpai.robot.control.mode.ViewModeConsts.*;
import static com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_SET_APP_MODE;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInstaller;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.letianpai.robot.alarm.receiver.AlarmCallback;
import com.letianpai.robot.components.network.nets.GeeUIStatusUploader;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.components.parser.base.BaseMessageInfo;
import com.letianpai.robot.components.parser.recharge.ReChargeInfo;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.broadcast.LauncherBroadcastReceiverManager;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.broadcast.timer.TimerKeeperCallback;
import com.letianpai.robot.control.callback.*;
import com.letianpai.robot.control.consts.AudioServiceConst;
import com.letianpai.robot.control.consts.RobotConsts;
import com.letianpai.robot.control.manager.CommandResponseManager;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.manager.SleepModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.mode.callback.ModeChangeCallback;
import com.letianpai.robot.control.nets.GeeUiNetManager;
import com.letianpai.robot.control.storage.RobotConfigManager;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.control.system.LetianpaiLightUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.letianpai.robot.letianpaiservice.LtpAppCmdCallback;
import com.letianpai.robot.letianpaiservice.LtpBleCallback;
import com.letianpai.robot.letianpaiservice.LtpIdentifyCmdCallback;
import com.letianpai.robot.letianpaiservice.LtpLongConnectCallback;
import com.letianpai.robot.letianpaiservice.LtpMiCmdCallback;
import com.letianpai.robot.letianpaiservice.LtpRobotStatusCallback;
import com.letianpai.robot.letianpaiservice.LtpSensorResponseCallback;
import com.letianpai.robot.letianpaiservice.LtpSpeechCallback;
import com.letianpai.robot.notice.receiver.NoticeCallback;
import com.letianpai.robot.ota.broadcast.PackageInstallReceiver;
import com.letianpai.robot.response.RobotFuncResponseManager;
import com.letianpai.robot.response.app.AppCmdResponser;
import com.letianpai.robot.response.ble.BleCmdResponser;
import com.letianpai.robot.response.identify.IdentifyCmdResponser;
import com.letianpai.robot.response.mi.MiIotCmdResponser;
import com.letianpai.robot.response.remote.RemoteCmdResponser;
import com.letianpai.robot.response.robotStatus.RobotStatusResponser;
import com.letianpai.robot.response.sensor.SensorCmdResponser;
import com.letianpai.robot.response.speech.SpeechCmdResponser;
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback;
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.commandlib.consts.MCUCommandConsts;
import com.renhejia.robot.commandlib.consts.PackageConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.parser.config.RobotConfig;
import com.renhejia.robot.commandlib.parser.deviceinfo.DeviceInfo;
import com.renhejia.robot.commandlib.parser.power.PowerMotion;
import com.renhejia.robot.commandlib.parser.time.ServerTime;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.gesturefactory.parser.GestureData;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author liujunbin
 */
public class DispatchService extends Service {
    private static final long UPDATE_INTERNAL_TIME = 60 * 60 * 1000;
    private ILetianpaiService iLetianpaiService;
    private boolean isConnectService;
    private static final String TAG = "DispatchService";

    private static final int SHOW_GESTURE = 1;
    private static final int SHOW_GESTURE_STR = 2;
    private static final int SHOW_GESTURES_STR = 3;
    private static final int SHOW_GESTURES_WITH_ID = 4;
    private static final int SHOW_GESTURE_STR_OBJECT = 5;
    private GestureHandler mHandler;
    private TaskHandler mTaskHandler;
    private ControlSteeringEngineHandler mControlSteeringEngineHandler;

    private int robotStatus;
    private int previousRobotStatus;
    private int mHour = -1;
    private int mMinute;
    private boolean isLastTempHigh = false;
    private Callback allConfigCallback;
    private Callback reChargeConfigCallback;

    private static final int CLOSE_SPEECH_AUDIO = 11;
    private static final int CLOSE_SPEECH_AUDIO_AND_LISTENING = 12;

    private static final int FOOT_POWER = 21;
    private static final int FOOT_SENSOR = 22;

    private static final int POWER_OFF_IDLE = 1;
    private static final int POWER_OFF_ING = 2;
    private static final int POWER_OFF_FINISH = 3;

    private int powerStatus = POWER_OFF_IDLE;
    private boolean hadPlayLowPowerAnimation;
    private CountDownTimer countDownTimer;
    private int currentBatteryPercent;
    private boolean isCharging = false;
    private PackageInstallReceiver installReceiver;
    private long uploadEnterAutoCharging;
    private long uploadExitAutoCharging;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindLPTService();
        init();
    }

    private void init() {
        mHandler = new GestureHandler(DispatchService.this);
        mTaskHandler = new TaskHandler(DispatchService.this);
        mControlSteeringEngineHandler = new ControlSteeringEngineHandler(DispatchService.this);
        addTimeKeepLister();
        LauncherBroadcastReceiverManager.getInstance(DispatchService.this);
        LetianpaiLightUtil.setScreenBrightness(DispatchService.this, 10);
        startStatusBar();
        registerAppStoreReceiver();
        addCommandListener();
        addGestureListeners();
        addModeChangeListeners();
        addTemperatureListeners();
        initCallBack();
        addChargingCallback();
        addNetworkChangeListeners();
        initCountDownTimer();
        addControlSteeringEngineListeners();
        //获取硬件码
        getEncodeInfo();
        getReChargeConfig();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GeeUILogUtils.logi(TAG, "--DispatchService onDestroy");
        unregisterAppStoreReceiver();
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.unregisterNetworkCallback(networkCallback);

        if (iLetianpaiService != null) {
            try {
                iLetianpaiService.unregisterLCCallback(ltpLongConnectCallback);
                iLetianpaiService.unregisterSpeechCallback(ltpSpeechCallback);
                iLetianpaiService.unregisterAppCmdCallback(ltpAppCmdCallback);
                iLetianpaiService.unregisterSensorResponseCallback(ltpSensorResponseCallback);
                iLetianpaiService.unregisterRobotStatusCallback(ltpRobotStatusCallback);
                iLetianpaiService.unregisterMiCmdResponseCallback(ltpMiCmdCallback);
                iLetianpaiService.unregisterIdentifyCmdCallback(identifyCmdCallback);
                iLetianpaiService.unregisterBleCmdCallback(ltpBleCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            isConnectService = false;
        }
        unbindService(serviceConnection);
    }

    private void requestData() {
        getEncodeInfo();
        getUserAppsConfig();
        getRobotStatus();
        getReChargeConfig();
    }

    private void addControlSteeringEngineListeners() {
        ControlSteeringEngineCallback.getInstance().setControlSteeringEngineListener(new ControlSteeringEngineCallback.ControlSteeringEngineListener() {
            @Override
            public void onControlSteeringEngine(boolean footSwitch, boolean sensorSwitch) {
                switchPower(footSwitch);
                switchSensor(sensorSwitch);
            }
        });
    }

    private void registerAppStoreReceiver() {
        installReceiver = new PackageInstallReceiver();
        IntentFilter installIntentFilter = new IntentFilter();
        installIntentFilter.addAction(PackageInstaller.ACTION_SESSION_COMMITTED);
        installIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        installIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        installIntentFilter.addDataScheme("package");
        registerReceiver(installReceiver, installIntentFilter);
    }

    private void unregisterAppStoreReceiver() {
        if (installReceiver != null) {
            unregisterReceiver(installReceiver);
        }
    }

    /**
     * todo: 此代码查看应该是没有用到，后期删除
     */
    private void updateTime() {
        long current = System.currentTimeMillis();
        long lastUpdateTime = RobotConfigManager.getInstance(DispatchService.this).getUpdateTime();
        long results = current - lastUpdateTime;

        if (results < UPDATE_INTERNAL_TIME) {
            return;
        }

        GeeUiNetManager.getTimeStamp(DispatchService.this, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    ServerTime serverTime = null;
                    String info = "";
                    if (response != null && response.body() != null) {
                        info = response.body().string();
                    }
                    try {
                        if (info != null) {
                            serverTime = new Gson().fromJson(info, ServerTime.class);
                            if (serverTime != null && serverTime.getData() != null && serverTime.getData().getTimestamp() != 0) {
                                updateRobotTime(serverTime);
                            }
                        }
                    } catch (Exception e) {
                        XLog.i(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void updateRobotTime(ServerTime serverTime) {
        long current = System.currentTimeMillis();
        RobotConfigManager.getInstance(DispatchService.this).setUpdateTime(current);
        RobotConfigManager.getInstance(DispatchService.this).commit();
        long result = current - serverTime.getData().getTimestamp() * 1000;
        if (Math.abs(result) > 60 * 1000) {
            changeTime(serverTime.getData().getTimestamp() * 1000);
        }
    }

    private void changeTime(long time) {
        boolean status = SystemClock.setCurrentTimeMillis(time * 1000);
    }

    private void initCallBack() {
        allConfigCallback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    RobotConfig robotConfig = null;
                    String info = "";
                    if (response != null && response.body() != null) {
                        info = response.body().string();
                    }
                    try {
                        if (info != null) {
                            robotConfig = new Gson().fromJson(info, RobotConfig.class);
                            if (robotConfig != null && robotConfig.getData() != null && robotConfig.getData().getDevice_sound_config() != null) {
                                int volume = robotConfig.getData().getDevice_sound_config().getVolume_size();
                                SleepModeManager.getInstance(DispatchService.this).setRobotVolume(volume);
                            }
                        }
                    } catch (Exception e) {
                        XLog.i(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
            }
        };
        reChargeConfigCallback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    ReChargeInfo reChargeInfo = null;
                    String info = "";
                    if (response != null && response.body() != null) {
                        info = response.body().string();
                    }
                    try {
                        if (info != null) {
                            GeeUILogUtils.logi("letianpai_recharging", "---reChargeConfigCallback_info: " + info);
                            reChargeInfo = new Gson().fromJson(info, ReChargeInfo.class);
                            GeeUILogUtils.logi("letianpai_recharging", "---reChargeInfo.getData().getAutomatic_recharge_val() : " + reChargeInfo.getData().getAutomatic_recharge_val());
                            GeeUILogUtils.logi("letianpai_recharging", "---reChargeInfo.getData().getAutomatic_recharge_switch(): " + reChargeInfo.getData().getAutomatic_recharge_switch());
                            if (reChargeInfo != null && reChargeInfo.getData() != null && reChargeInfo.getData().getAutomatic_recharge_val() != 0) {
                                RobotConfigManager.getInstance(DispatchService.this).setAutomaticRechargeSwitch(reChargeInfo.getData().getAutomatic_recharge_switch());
                                RobotConfigManager.getInstance(DispatchService.this).setAutomaticRechargeVal(reChargeInfo.getData().getAutomatic_recharge_val());
                                RobotConfigManager.getInstance(DispatchService.this).commit();
                            }
                        }
                    } catch (Exception e) {
                        XLog.i(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void getRobotStatus() {
        com.letianpai.robot.components.network.nets.GeeUiNetManager.getAllConfig(DispatchService.this, allConfigCallback);
    }

    private void getReChargeConfig() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                com.letianpai.robot.components.network.nets.GeeUiNetManager.getReChargeConfig(DispatchService.this, SystemUtil.isInChinese(), reChargeConfigCallback);
            }
        }).start();

    }

    private void getUserAppsConfig() {
        AppCmdResponser.getInstance(DispatchService.this).getUserAppsConfig();
    }

    private void getEncodeInfo() {
        if (SystemUtil.hasHardCode()) {
            return;
        }
        GeeUiNetManager.getDeviceInfo(DispatchService.this, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    DeviceInfo deviceInfo = null;
                    String info = "";
                    if (response != null && response.body() != null) {
                        info = response.body().string();
                        GeeUILogUtils.logi("letianpai_getEncodeInfo", "---info: " + info);
                    }

                    try {
                        if (info != null) {
                            deviceInfo = new Gson().fromJson(info, DeviceInfo.class);
                            if (deviceInfo != null && deviceInfo.getData() != null && !TextUtils.isEmpty(deviceInfo.getData().getClient_id()) && !TextUtils.isEmpty(deviceInfo.getData().getHard_code()) && !TextUtils.isEmpty(deviceInfo.getData().getSn())) {
                                SystemUtil.setHardCode(deviceInfo.getData().getHard_code());
                            }
                        }
                    } catch (Exception e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private ConnectivityManager.NetworkCallback networkCallback;

    private void addNetworkChangeListeners() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // 当网络可用时执行相应操作
                requestData();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private void addTemperatureListeners() {
        TemperatureUpdateCallback.getInstance().registerTemperatureUpdateListener(new TemperatureUpdateCallback.TemperatureUpdateListener() {
            @Override
            public void onTemperatureUpdate(float temp) {
                if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                    return;
                }
                if (temp > TemperatureUpdateCallback.HIGH_TEMP) {
                    if (RobotModeManager.getInstance(DispatchService.this).isRobotDeepSleepMode()) {
//                        updateRobotStatus(temp);
                        GeeUIStatusUploader.getInstance(DispatchService.this).uploadRobotStatus();
                        isLastTempHigh = true;
                        return;
                    }
                    // 进入睡眠模式
                    if (RobotModeManager.getInstance(DispatchService.this).isCommonRobotMode() ||
                            LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this) ||
                            LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this)) {
                        if (LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this) || LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this)) {
                            CommandResponseCallback.getInstance().setIdentifyCmd(AppCmdConsts.COMMAND_STOP_APP, AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL);
                        }
                        try {
                            iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT);
                            //高温模式，关闭speech【解决：高温跳舞不结束的bug】
                            closeSpeechAudio();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        RobotModeManager.getInstance(DispatchService.this).switchRobotMode(ViewModeConsts.VM_DEEP_SLEEP_MODE, ViewModeConsts.VIEW_MODE_IN);
                    }

                } else if (temp <= TemperatureUpdateCallback.TARGET_TEMP) {
                    if (isLastTempHigh) {
                        isLastTempHigh = false;
                    }
//                    updateRobotStatus(temp);
                    GeeUIStatusUploader.getInstance(DispatchService.this).uploadRobotStatus();
                    if (RobotModeManager.getInstance(DispatchService.this).isRobotDeepSleepMode()) {
                        RobotModeManager.getInstance(DispatchService.this).switchRobotMode(ViewModeConsts.VM_DEEP_SLEEP_MODE, ViewModeConsts.VIEW_MODE_OUT);
                    }
                }
            }
        });
    }

    private void stopAudioService() {
        try {
            iLetianpaiService.setRobotStatusCmd(AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAudioService() {
        try {
            iLetianpaiService.setRobotStatusCmd(AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startStatusBar() {
        LetianpaiFunctionUtil.showFloatingView(DispatchService.this);
    }

    private void bindLPTService() {
        connectService();
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GeeUILogUtils.logi(TAG, "---乐天派 ControlService 完成AIDLService服务");
            iLetianpaiService = ILetianpaiService.Stub.asInterface(service);
            RobotModeManager.getInstance(DispatchService.this).setiLetianpaiService(iLetianpaiService);
            try {
                iLetianpaiService.registerLCCallback(ltpLongConnectCallback);
                iLetianpaiService.registerSpeechCallback(ltpSpeechCallback);
                iLetianpaiService.registerAppCmdCallback(ltpAppCmdCallback);
                iLetianpaiService.registerSensorResponseCallback(ltpSensorResponseCallback);
                iLetianpaiService.registerRobotStatusCallback(ltpRobotStatusCallback);
                iLetianpaiService.registerMiCmdResponseCallback(ltpMiCmdCallback);
                iLetianpaiService.registerIdentifyCmdCallback(identifyCmdCallback);
                iLetianpaiService.registerBleCmdCallback(ltpBleCallback);
//              iLetianpaiService.registerExpressionCallback(expressionCallback);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
            isConnectService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            GeeUILogUtils.logi(TAG, "---乐天派 ControlService 无法绑定aidlserver的AIDLService服务");
            isConnectService = false;
        }
    };

    //链接服务端
    private void connectService() {
        Intent intent = new Intent();
        intent.setPackage("com.renhejia.robot.letianpaiservice");
        intent.setAction("android.intent.action.LETIANPAI");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final LtpLongConnectCallback.Stub ltpLongConnectCallback = new LtpLongConnectCallback.Stub() {
        @Override
        public void onLongConnectCommand(String command, String data) throws RemoteException {
            GeeUILogUtils.logi("onLongConnectCommand", "---command: " + command + " /data: " + data);
            GeeUILogUtils.logi("onLongConnectCommand", "---onLongConnectCommand: " + "  当前机器人模式：" + RobotModeManager.getInstance(DispatchService.this).getRobotMode());
            if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                return;
            }
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_REMOTE, iLetianpaiService, command, data, false);
        }
    };

    private final LtpSpeechCallback.Stub ltpSpeechCallback = new LtpSpeechCallback.Stub() {
        @Override
        public void onSpeechCommandReceived(String command, String data) throws RemoteException {
            if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                return;
            }
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_SPEECH, iLetianpaiService, command, data, false);
        }
    };

    private final LtpAppCmdCallback.Stub ltpAppCmdCallback = new LtpAppCmdCallback.Stub() {
        @Override
        public void onAppCommandReceived(String command, String data) throws RemoteException {
            if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                return;
            }
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_APP, iLetianpaiService, command, data, false);
        }
    };

    private final LtpIdentifyCmdCallback.Stub identifyCmdCallback = new LtpIdentifyCmdCallback.Stub() {
        @Override
        public void onIdentifyCommandReceived(String command, String data) throws RemoteException {
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_IDENTIFY, iLetianpaiService, command, data, false);
        }


    };

    private final LtpMiCmdCallback.Stub ltpMiCmdCallback = new LtpMiCmdCallback.Stub() {
        @Override
        public void onMiCommandReceived(String command, String data) throws RemoteException {
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_MI_IOT, iLetianpaiService, command, data, false);
        }
    };

    private final LtpRobotStatusCallback.Stub ltpRobotStatusCallback = new LtpRobotStatusCallback.Stub() {
        @Override
        public void onRobotStatusChanged(String command, String data) throws RemoteException {
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_ROBOT_STATUS, iLetianpaiService, command, data, false);
        }
    };

    private final LtpBleCallback.Stub ltpBleCallback = new LtpBleCallback.Stub() {
        @Override
        public void onBleCmdReceived(String command, String data, boolean isNeedResponse) throws RemoteException {
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_BLE, iLetianpaiService, command, data, isNeedResponse);
        }
    };

    private final LtpSensorResponseCallback.Stub ltpSensorResponseCallback = new LtpSensorResponseCallback.Stub() {
        @Override
        public void onSensorResponse(String command, String data) throws RemoteException {
            GeeUILogUtils.logi("letianpai_sensor", "---command: " + command + "data: " + data);
            if (RobotModeManager.getInstance(DispatchService.this).getRobotMode() == VM_UNBIND_DEVICE) {
                return;
            }
            // TODO 设置成传感器触发模式
            responseRobotCommand(RobotConsts.ROBOT_COMMAND_TYPE_SENSOR, iLetianpaiService, command, data, false);
        }
    };

    /**
     * @param commandType
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void responseRobotCommand(int commandType, ILetianpaiService iLetianpaiService, String command, String data, boolean isNeedResponse) {
//        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this)) {
//            Log.e("letianpai_tap", "responseRobotCommand === 1");
//            stopAudioService();
//            return;
//        }
        GeeUILogUtils.logi(TAG, "---responseRobotCommand:  commandType: " + commandType + "---command::" + command + "---data:" + data);

        boolean noNeedRes = RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode();
        GeeUILogUtils.logi(TAG, "---responseRobotCommand_data: ============ C ============noNeedRes: " + noNeedRes);
        if (noNeedRes) {
            if (command.equals(COMMAND_SET_APP_MODE)) {
                //需要放行
            } else if (command.equals(AppCmdConsts.COMMAND_TYPE_SET_ROBOT_MODE) &&
                    (data.equals(AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_OUT)
                            || data.equals(AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_IN)
                            || data.equals(AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_OUT)
                            || data.equals(AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_IN))) {
            } else {
                return;
            }
        }

        if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_SENSOR) {
            GeeUILogUtils.logi("letianpai_remind_notice", "---responseRobotCommand_data: ============ ROBOT_COMMAND_TYPE_SENSOR============");
            responseRobotSensorCommand(commandType, iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_REMOTE) {
            //如果触发悬崖不执行
            GeeUILogUtils.logi(TAG, "---responseRobotCommand_data: isInCliffMode: " + RobotModeManager.getInstance(DispatchService.this).isInCliffMode());
            if (RobotModeManager.getInstance(DispatchService.this).isInCliffMode()) {
                return;
            }

            GeeUILogUtils.logi("letianpai_remind_notice", "---responseRobotCommand_data: ============ I ============");
            responseRobotRemoteCommand(commandType, iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_SPEECH) {
            GeeUILogUtils.logi("letianpai_remind_notice", "---responseRobotCommand_data: ============ J ============");
            responseRobotSpeechCommand(commandType, iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_APP) {
            AppCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_MI_IOT) {
            MiIotCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_ROBOT_STATUS) {
            RobotStatusResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_IDENTIFY) {
            IdentifyCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_BLE) {
            BleCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data, isNeedResponse);

        } else if (commandType == RobotConsts.ROBOT_COMMAND_TYPE_AUTO) {

        } else {
            //RemoteCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
        }

//        SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService,command, data);

    }

    private void addChargingCallback() {
        ChargingUpdateCallback.getInstance().registerChargingStatusUpdateListener(new ChargingUpdateCallback.ChargingUpdateListener() {
            @Override
            public void onChargingUpdateReceived(boolean changingStatus, int percent) {
                currentBatteryPercent = percent;
                isCharging = changingStatus;
                if (!changingStatus
                        && (percent < RobotConfigManager.getInstance(DispatchService.this).getAutomaticRechargeVal())
                        && RobotConfigManager.getInstance(DispatchService.this).getAutomaticRechargeSwitch()
                        && (LetianpaiFunctionUtil.isAutoAppCanBeLaunched(DispatchService.this))// TODO 此处先写成本地逻辑，稍后服务端提供接口提供后，更新为使用服务端的逻辑
                        && percent >= ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD
                        && !LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this)) {
                    //启动自动回充
                    RobotModeManager.getInstance(DispatchService.this).switchRobotMode(ViewModeConsts.VM_AUTO_CHARGING, 1);
                }

                if (!changingStatus && percent < ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD) {
                    if (powerStatus == POWER_OFF_IDLE) {
                        countDownTimer.start();
                        powerStatus = POWER_OFF_ING;
                    } else if (powerStatus == POWER_OFF_ING) {
                    } else if (powerStatus == POWER_OFF_FINISH) {
                        SystemFunctionUtil.shutdownRobot(DispatchService.this);
                    }
                } else if (changingStatus) {
                    powerStatus = POWER_OFF_IDLE;
                    countDownTimer.cancel();
                    hadPlayLowPowerAnimation = false;
                } else if ((percent < ChargingUpdateCallback.LOW_BATTERY_NOTICE) && RobotModeManager.getInstance(DispatchService.this).isRobotMode()) {
                    //如果自动回充的话，不显示下面的提示
                    if (LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this)) return;

                    if (!hadPlayLowPowerAnimation) {
                        hadPlayLowPowerAnimation = true;
                        ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_DEMOSTRATE_MODE, 1);
                        GestureCallback.getInstance().setGestures(GestureCenter.getLowBatteryNoticeGesture(), RGestureConsts.GESTURE_ROBOT_LOW_BATTERY_NOTICE);
                    }
                }
            }

            @Override
            public void onChargingUpdateReceived(boolean changingStatus, int percent, int chargePlug) {
            }
        });
    }

    private void initCountDownTimer() {

        countDownTimer = new CountDownTimer(10 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                powerStatus = POWER_OFF_FINISH;
                if (!isCharging && currentBatteryPercent <= ChargingUpdateCallback.LOW_BATTERY_SHUTDOWN_STANDARD) {
                    SystemFunctionUtil.shutdownRobot(DispatchService.this);
                }
            }
        };
    }


    /**
     * @param commandType
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void responseRobotSensorCommand(int commandType, ILetianpaiService iLetianpaiService, String command, String data) {
        boolean robotActivate = SystemUtil.getRobotActivateStatus();
        boolean isAppMode = RobotModeManager.getInstance(DispatchService.this).isAppMode();
        boolean isAlarm = LetianpaiFunctionUtil.isAlarmOnTheTop(DispatchService.this);
        boolean isVideoCall = LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this);
        boolean isVideoCallService = LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this);
        GeeUILogUtils.logi(TAG, "---responseRobotSensorCommand::robotActivate:" + robotActivate + "--isAppMode::" + isAppMode + "--isAlarm:" + isAlarm + "--isVideoCall::" + isVideoCall + "--isVideoCallService::" + isVideoCallService);
        GeeUILogUtils.logi(TAG, "---responseRobotSensorCommand::command:" + command + "--data::" + data);

        // if (!(robotActivate || (isAppMode && !isAlarm) || (isAppMode && isVideoCall) || (isAppMode && isVideoCallService))){
        //     return;
        // }

        if (!SystemUtil.getRobotActivateStatus()
                || ((RobotModeManager.getInstance(DispatchService.this).isAppMode()))
                && (!LetianpaiFunctionUtil.isAlarmOnTheTop(DispatchService.this))
                && (!LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this))
                && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))
        ) {
            return;
        }
        // Log.e("letianpai_tap", "command: " + command);
        // Log.e("letianpai_tap", "data: " + data);
        if (ChargingUpdateCallback.getInstance().isCharging() && (!command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA))) {
            return;
        }

        int robotMode = RobotModeManager.getInstance(DispatchService.this).getRobotMode();
        boolean robotWakeupMode = RobotModeManager.getInstance(DispatchService.this).isRobotWakeupMode();
        boolean isRobotOnTheTop = LetianpaiFunctionUtil.isRobotOnTheTop(DispatchService.this);

        if (LetianpaiFunctionUtil.isRobotOnTheTop(DispatchService.this) && !robotWakeupMode) {
            if (RobotModeManager.getInstance(DispatchService.this).isRobotDeepSleepMode()) {
                if (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA)) {
                    SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
                }
            } else {
                SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
            }
        } else if (LetianpaiFunctionUtil.isSpeechOnTheTop(DispatchService.this) || LetianpaiFunctionUtil.isLexOnTheTop(DispatchService.this)) {
            SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (LetianpaiFunctionUtil.isSpeechRunning(DispatchService.this) || LetianpaiFunctionUtil.isLexRunning(DispatchService.this)) {
            SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (RobotModeManager.getInstance(DispatchService.this).isRestMode() && (!ChargingUpdateCallback.getInstance().isCharging())) {
            SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else if (RobotModeManager.getInstance(DispatchService.this).isCloseScreenMode() && (!ChargingUpdateCallback.getInstance().isCharging())) {
            SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

        } else {
            if (LetianpaiFunctionUtil.isAlarmRunning(DispatchService.this) && (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA))) {
                SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);

            } else if ((LetianpaiFunctionUtil.isSpeechOnTheTop(DispatchService.this) || LetianpaiFunctionUtil.isLexOnTheTop(DispatchService.this)) && ((command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA)) || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_DOUBLE_TAP_DATA)))) {
                SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
            } else if ((command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_BACKEND))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_FORWARD))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_LEFT))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_RIGHT))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_START_DATA))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_STOP_DATA))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_START_DATA))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_STOP_DATA))
                    || (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_TOF))
            ) {
                SensorCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
            }
        }
    }

    /**
     * @param commandType
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void responseRobotSpeechCommand(int commandType, ILetianpaiService iLetianpaiService, String command, String data) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return;
        }
        this.previousRobotStatus = this.robotStatus;
        this.robotStatus = commandType;
        SpeechCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
    }

    /**
     * @param commandType
     * @param command
     * @param data
     */
    private void responseRobotRemoteCommand(int commandType, ILetianpaiService iLetianpaiService, String command, String data) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return;
        }
        this.previousRobotStatus = this.robotStatus;
        this.robotStatus = commandType;

        //高温模式下，仅响应关机和重启
        if (TemperatureUpdateCallback.getInstance().isInHighTemperature()) {
            GeeUILogUtils.logi("letianpai_test", "---TemperatureUpdateCallback.getInstance().isInHighTemperature()1: " + TemperatureUpdateCallback.getInstance().isInHighTemperature());
            if ((!TextUtils.isEmpty(command)) && (command.equals(RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN) || command.equals(RobotRemoteConsts.COMMAND_TYPE_REBOOT))) {
                RemoteCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
            } else if ((!TextUtils.isEmpty(command)) && (command.equals(MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC)
                    || command.equals(MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_MONITOR)
                    || command.equals(MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_TRANSFORM))) {
                RemoteCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
            } else {
//                updateRobotStatus(TemperatureUpdateCallback.getInstance().getTemp());
                GeeUIStatusUploader.getInstance(DispatchService.this).uploadRobotStatus();
            }
        } else {
            RemoteCmdResponser.getInstance(DispatchService.this).commandDistribute(iLetianpaiService, command, data);
        }
    }

    /**
     * 恢复到上一次机器人状态
     */
    public void responseRobotStatus() {
        this.robotStatus = this.previousRobotStatus;
        this.previousRobotStatus = 0;
    }

    public void showGesture(String gesture) {
        Message message = new Message();
        message.what = SHOW_GESTURE_STR;
        message.obj = gesture;
        mHandler.sendMessage(message);
    }

    public void showGesture(String gesture, int gId) {
        Message message = new Message();
        message.what = SHOW_GESTURES_WITH_ID;
        message.obj = gesture;
        message.arg1 = gId;
        mHandler.sendMessage(message);
    }

    public void showGestures(ArrayList<GestureData> list, int taskId) {
        Message message = new Message();
        message.what = SHOW_GESTURES_STR;
        message.obj = list;
        message.arg1 = taskId;
        mHandler.sendMessage(message);
    }

    public void showGesture(GestureData gestureData) {
        Message message = new Message();
        message.what = SHOW_GESTURE_STR_OBJECT;
        message.obj = gestureData;
        mHandler.sendMessage(message);
    }


    private class GestureHandler extends Handler {

        private final WeakReference<Context> context;

        public GestureHandler(Context context) {
            this.context = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == SHOW_GESTURE_STR) {
                if (msg.obj != null) {
                    CommandResponseManager.getInstance(DispatchService.this).responseGestures(((String) msg.obj), iLetianpaiService);
                }

            } else if (msg.what == SHOW_GESTURES_STR) {
                if (msg.obj != null) {
                    CommandResponseManager.getInstance(DispatchService.this).responseGestures(((ArrayList<GestureData>) msg.obj), msg.arg1, iLetianpaiService);
                }

            } else if (msg.what == SHOW_GESTURE_STR_OBJECT) {
                if (msg.obj != null) {
                    CommandResponseManager.getInstance(DispatchService.this).responseGesture(((GestureData) msg.obj), iLetianpaiService);
                }

            } else if (msg.what == SHOW_GESTURES_WITH_ID) {
                if (msg.obj != null && msg.arg1 != 0) {
                    CommandResponseManager.getInstance(DispatchService.this).responseGestures(((String) msg.obj), msg.arg1, iLetianpaiService);
                }

            }
        }
    }


    public void switchPower(boolean powerStatus) {
        Message message = new Message();
        message.what = FOOT_POWER;
        message.obj = powerStatus;
        mControlSteeringEngineHandler.sendMessage(message);
    }

    public void switchSensor(boolean sensorStatus) {
        Message message = new Message();
        message.what = FOOT_SENSOR;
        message.obj = sensorStatus;
        mControlSteeringEngineHandler.sendMessageDelayed(message, 50);
    }


    private class ControlSteeringEngineHandler extends Handler {

        private final WeakReference<Context> context;

        public ControlSteeringEngineHandler(Context context) {
            this.context = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            boolean status = (boolean) (msg.obj);
            if (msg.what == FOOT_POWER) {
                if (status) {
                    CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(3, 1).toString());
                } else {
                    CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(3, 0).toString());
                }

            } else if (msg.what == FOOT_SENSOR) {
                if (status) {
                    CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(5, 1).toString());
                } else {
                    CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(5, 0).toString());
                }
            }
        }
    }

    public void closeSpeechAudio() {
        Message message = new Message();
        message.arg1 = CLOSE_SPEECH_AUDIO;
        mTaskHandler.sendMessageDelayed(message, 100);
    }

    public void closeSpeechAudioAndListening(GestureData gestureData) {
        Message message = new Message();
        message.arg1 = CLOSE_SPEECH_AUDIO_AND_LISTENING;
        mTaskHandler.sendMessage(message);
    }

    private class TaskHandler extends Handler {

        private final WeakReference<Context> context;

        public TaskHandler(Context context) {
            this.context = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == CLOSE_SPEECH_AUDIO) {
                if (msg.arg1 != 0) {
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService);
                }

            } else if (msg.arg1 == CLOSE_SPEECH_AUDIO_AND_LISTENING) {
                if (msg.arg1 != 0) {
                    CommandResponseManager.getInstance(DispatchService.this).responseGestures(((ArrayList<GestureData>) msg.obj), msg.arg1, iLetianpaiService);
                }
            }
        }
    }

    private void addCommandListener() {

        CommandResponseCallback.getInstance().setLTPCommandResponseListener(new CommandResponseCallback.LTPCommandResponseListener() {
            @Override
            public void onLTPCommandReceived(String command, String data) {
                try {
                    iLetianpaiService.setMcuCommand(command, data);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        CommandResponseCallback.getInstance().setLTPRobotStatusCmdResponseListener(new CommandResponseCallback.LTPRobotStatusCmdResponseListener() {
            @Override
            public void onRobotStatusCmdResponse(String command, String data) {
                try {
                    iLetianpaiService.setRobotStatusCmd(command, data);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        CommandResponseCallback.getInstance().setLTPIdentifyCommandResponseListener(new CommandResponseCallback.LTPIdentifyCommandResponseListener() {
            @Override
            public void onIdentifyCmdResponse(String command, String data) {
                try {
                    iLetianpaiService.setIdentifyCmd(AppCmdConsts.COMMAND_STOP_APP, AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });


        GestureCallback.getInstance().setGestureListener(new GestureCallback.GestureResponseListener() {
            @Override
            public void onGestureReceived(String gesture) {
                GeeUILogUtils.logi("RemoteCmdResponser", "---startPrecipice: 悬空姿态开始 === 2");
                showGesture(gesture);
            }

            @Override
            public void onGestureReceived(String gesture, int gestureId) {
                GeeUILogUtils.logi("letianpai_task", "---gesture: " + gesture + "gestureId: " + gestureId);
                showGesture(gesture, gestureId);
            }

            @Override
            public void onGesturesReceived(ArrayList<GestureData> list, int taskId) {
                GeeUILogUtils.logi("letianpai_task", "---gesture_list: " + list.toString() + "taskId: : " + taskId);
                showGestures(list, taskId);
            }

            @Override
            public void onGesturesReceived(GestureData gestureData) {
                GeeUILogUtils.logi("RemoteCmdResponser", "---startPrecipice: 悬空姿态开始 === 5");
                showGesture(gestureData);
            }
        });

        ExpressionChangeCallback.getInstance().setExpressionChangeListener(new ExpressionChangeCallback.ExpressionChangeListener() {
            @Override
            public void onExpressionChanged(String expression) {
                GeeUILogUtils.logi("test_fukun", "---expression: " + expression);
                try {
                    iLetianpaiService.setExpression(RobotRemoteConsts.COMMAND_TYPE_FACE, expression);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMainImageShow() {

            }

            @Override
            public void onDisplayViewShow(String viewName) {
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_SHOW_TIME, RobotRemoteConsts.COMMAND_TYPE_SHOW_TIME);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onChatGptView() {

            }

            @Override
            public void onRemoteControlViewShow() {

            }

            @Override
            public void onShowImage() {

            }

            @Override
            public void onShowText(String text) {

            }

            @Override
            public void onShowBlack(boolean isShow) {

            }

            @Override
            public void onShutdown() {

            }

            @Override
            public void onCountDownShow(String time) {

            }
        });

        AlarmCallback.getInstance().registerAlarmTimeListener(new AlarmCallback.AlarmTimeListener() {
            @Override
            public void onAlarmTimeOut(int hour, int minute) {
                if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                    return;
                }
                if (LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this) || LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this)) {
                    return;
                }
                GeeUILogUtils.logi("letianpai_timer", "---updateAlarm: " + hour + "_" + minute);
//                RobotModeManager.getInstance(DispatchService.this).setAlarming(true);
                String mHour, mMinute;
                if (hour < 10) {
                    mHour = "0" + hour;
                } else {
                    mHour = "" + hour;
                }

                if (minute < 10) {
                    mMinute = "0" + minute;
                } else {
                    mMinute = "" + minute;
                }
                LetianpaiFunctionUtil.openClock(DispatchService.this, mHour + ":" + mMinute);
            }
        });

        NoticeCallback.getInstance().registerNoticeTimeListener(new NoticeCallback.NoticeTimeListener() {
            @Override
            public void onNoticeTimeOut(int hour, int minute, String title) {
                GeeUILogUtils.logi("letianpai_notice", "---hour: " + hour);
                GeeUILogUtils.logi("letianpai_notice", "---minute: " + minute);
                GeeUILogUtils.logi("letianpai_notice", "---title: " + title);
                if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                    return;
                }
                if (LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this) || LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this)) {
                    return;
                }
                String mHour, mMinute;
                if (hour < 10) {
                    mHour = "0" + hour;
                } else {
                    mHour = "" + hour;
                }

                if (minute < 10) {
                    mMinute = "0" + minute;
                } else {
                    mMinute = "" + minute;
                }
                LetianpaiFunctionUtil.openNotices(DispatchService.this, mHour + ":" + mMinute, title);
            }
        });
    }

    private void addGestureListeners() {
        GestureCallback.getInstance().setGestureCompleteListener(new GestureCallback.GestureCompleteListener() {
            @Override
            public void onGestureCompleted(String gesture, int taskId) {
                GeeUILogUtils.logi("onGestureCompleted_00", "---gesture010--" + gesture + "--taskId: " + taskId);
                int robotMode = RobotModeManager.getInstance(DispatchService.this).getRobotMode();
                GeeUILogUtils.logi("onGestureCompleted_11", "---robotMode: " + robotMode);
                if (robotMode == ViewModeConsts.VM_STATIC_MODE) {
                    return;
                }
                GeeUILogUtils.logi("onGestureCompleted", "---gesture--" + gesture + "--taskId: " + taskId);
                GeeUILogUtils.logi("RemoteCmdResponser", "---startPrecipice: 悬空姿态开始 ======= 99");
                switch (taskId) {
                    case RGestureConsts.GESTURE_ID_TIME_KEEPER:
                        if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                            RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
                        }
                        break;
                    case RGestureConsts.GESTURE_ID_DANGLING_START: //悬空反馈开始
                        GeeUILogUtils.logi("letianpai_test_control999", "---RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_START_DATA ======== 1 end ========");
                        GestureCallback.getInstance().setGestures(GestureCenter.danglingGestureData(), RGestureConsts.GESTURE_ID_DANGLING_START);
                        break;

                    case RGestureConsts.GESTURE_ID_DANGLING_END: //悬空反馈结束./
                        GeeUILogUtils.logi("letianpai_test_control999", "---RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_STOP_DATA ======== 2 end ========");
                        backToPreviousPlayMode();
                        break;

                    case RGestureConsts.GESTURE_ID_CLIFF_FORWARD://悬崖往前end
                    case RGestureConsts.GESTURE_ID_CLIFF_BACKEND://悬崖往后end
                    case RGestureConsts.GESTURE_ID_CLIFF_LEFT://悬崖往左end
                    case RGestureConsts.GESTURE_ID_CLIFF_RIGHT://悬崖往右end
                        RobotModeManager.getInstance(DispatchService.this).setInCliffMode(false);
                        GeeUILogUtils.logi("DispatchService", "--HANDLE--CLIFF---END--");
                        backToPreviousPlayMode();
                        break;

                    case RGestureConsts.GESTURE_ID_TAP: //单击
                    case RGestureConsts.GESTURE_ID_FALLDOWN_END: //倒下结束
                    case RGestureConsts.GESTURE_ID_DOUBLE_TAP: //双击
                    case RGestureConsts.GESTURE_ID_LONG_PRESS: //长按
                        backToPreviousPlayMode0();
                        break;

                    case RGestureConsts.GESTURE_ID_Tof: //避障完成
                        //切换回机器人模式
                        RobotModeManager.getInstance(DispatchService.this).setInTofMode(false);
                        GeeUILogUtils.logi(TAG, "---避障完成:" + RobotModeManager.getInstance(DispatchService.this).isInTofMode());

                        // responseRobotStatus();
                        // if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                        //     RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
                        // }
                        backToPreviousPlayMode();
                        break;

                    case RGestureConsts.GESTURE_ID_FALLDOWN_START:
                        GestureCallback.getInstance().setGestures(GestureCenter.danglingGestureData(), RGestureConsts.GESTURE_ID_FALLDOWN_START);
                        break;

                    //24小时姿态完成
                    case RGestureConsts.GESTURE_ID_24_HOUR:
                        if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                            RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
                        }
                        break;

//                    case RGestureConsts.GESTURE_ID_HAPPY:
//                    case RGestureConsts.GESTURE_ID_SAD:
//                        RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
//                        break;

//                    case RGestureConsts.GESTURE_COMMAND_SPEECH_MOVE:
//                        RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
//                        break;

                    case RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY:
                        RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
                        break;

                    case RGestureConsts.GESTURE_COMMAND_GO_TO_SLEEP:
                        LetianpaiFunctionUtil.openTimeViewForSleep(DispatchService.this);
                        break;

                    case RGestureConsts.GESTURE_COMMAND_HIBERNATION:
                        if (RobotModeManager.getInstance(DispatchService.this).isRobotDeepSleepMode()
                                || RobotModeManager.getInstance(DispatchService.this).isSleepMode()) {
                            GestureCallback.getInstance().setGestures(GestureCenter.goToHibernationGesture1(), RGestureConsts.GESTURE_COMMAND_HIBERNATION);
                        }
                        // else if (RobotModeManager.getInstance(DispatchService.this).isSleepMode()) {
                        //     if (iLetianpaiService == null) {
                        //         return;
                        //     }
                        //     try {
                        //         iLetianpaiService.setRobotStatusCmd(RobotRemoteConsts.COMMAND_TYPE_ROBOT_STATUS, RobotRemoteConsts.COMMAND_VALUE_GO_TO_SLEEP);
                        //     } catch (RemoteException e) {
                        //
                        //     }
                        // }
                        break;

                    case RGestureConsts.GESTURE_MI_IOT:
                        if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                            RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
                        }
                        break;

                    // 第一次开机，动作执行完成之后，切换到静态模式
                    case RGestureConsts.GESTURE_POWER_ON_CHARGING:
                        RobotCommandWordsCallback.getInstance().showBattery(true);
                        RobotModeManager robotModeManager = RobotModeManager.getInstance(DispatchService.this);
                        if (robotModeManager.getRobotTrtcStatus() == -1) {
                            robotModeManager.switchToPreviousAppMode();
                        }
                        break;

                    case RGestureConsts.GESTURE_ROBOT_LOW_BATTERY_NOTICE:
                        RobotCommandWordsCallback.getInstance().showBattery(true);
                        if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                            RobotModeManager.getInstance(DispatchService.this).switchToPreviousAppMode();
                        }
                        break;

                    case RGestureConsts.GESTURE_ID_REMIND_WATER:
                    case RGestureConsts.GESTURE_ID_REMIND_SED:
                    case RGestureConsts.GESTURE_ID_REMIND_SITE:
                    case RGestureConsts.GESTURE_ID_REMIND_KEEP:
                        if ((!LetianpaiFunctionUtil.isVideoCallRunning(DispatchService.this)) && (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                            RobotModeManager.getInstance(DispatchService.this).switchToPreviousAppMode();
                        }
                        break;

                    default:
                        break;
                }
            }
        });
    }

    private void backToPreviousPlayMode0() {
        //切换回机器人模式
        responseRobotStatus();
        int currentMode = RobotModeManager.getInstance(this).getRobotMode();//VM_DEMOSTRATE_MODE
        if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this))
                || (LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))
                || (LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this))) {
            return;
        }
        if (currentMode == VM_DEMOSTRATE_MODE) {
            return;
        }
        RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
    }

    private void backToPreviousPlayMode() {
        RobotModeManager.getInstance(this).setInCliffMode(false);
        responseRobotStatus();
        int currentMode = RobotModeManager.getInstance(this).getRobotMode();//VM_DEMOSTRATE_MODE
        GeeUILogUtils.logi(TAG, "---backToPreviousPlayMode currentMode:" + currentMode);

//         if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this))
//                 || (LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))){
//             return;
//         }
        if ((LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this))) {
            return;
        }

        if (currentMode == VM_DEMOSTRATE_MODE) {
            try {
                iLetianpaiService.setExpression(RobotRemoteConsts.COMMAND_TYPE_FACE, "h0059");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        if (RobotModeManager.getInstance(DispatchService.this).getRobotTrtcStatus() != -1) {
            return;
        }
        RobotModeManager.getInstance(DispatchService.this).switchToPreviousPlayMode();
    }


    private void addTimeKeepLister() {
        TimerKeeperCallback.getInstance().registerTimerKeeperUpdateListener(new TimerKeeperCallback.TimerKeeperUpdateListener() {
            @Override
            public void onTimerKeeperUpdateReceived(int hour, int minute) {
                updateTime();
                uploadAutoChargingStatus();
                checkService();
                if (RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode()) {
                    return;
                }
                if (!SystemUtil.getRobotActivateStatus()) {
                    return;
                }
                if ((!LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this)) || (!LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                    timeKeeperGesture(hour, minute);
                }
                checkRobotTemp();
                if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(DispatchService.this)) || (LetianpaiFunctionUtil.isVideoCallServiceRunning(DispatchService.this))) {
                    return;
                }

                if (!RobotStatusResponser.getInstance(DispatchService.this).isNoNeedResponseMode() && !RobotModeManager.getInstance(DispatchService.this).isRobotDeepSleepMode()) {
                    if (RobotModeManager.getInstance(DispatchService.this).isRobotWakeupMode()) {
                        return;
                    } else if (RobotModeManager.getInstance(DispatchService.this).isAppMode() && (System.currentTimeMillis() - RobotStatusResponser.getInstance(DispatchService.this).getTapTime() > 60 * 1000)) {
                        LetianpaiFunctionUtil.updateRobotSleepStatus(DispatchService.this, hour, minute);

                    } else if (!RobotModeManager.getInstance(DispatchService.this).isAppMode()) {
                        LetianpaiFunctionUtil.updateRobotSleepStatus(DispatchService.this, hour, minute);
                    }
                }
            }
        });
    }

    private void uploadAutoChargingStatus() {
        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(DispatchService.this)) {
            if ((System.currentTimeMillis() - uploadEnterAutoCharging) > 1000 * 60 * 5) {
                com.letianpai.robot.components.network.nets.GeeUiNetManager.uploadEnterAutoCharging(DispatchService.this, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response != null && response.body() != null) {
                            String info = "";
                            if (response != null && response.body() != null) {
                                info = response.body().string();
                            }
                            BaseMessageInfo baseMessageInfo;
                            if (info != null) {
                                uploadEnterAutoCharging = System.currentTimeMillis();
                                GeeUILogUtils.logi("letianpai_uploadEnterAutoCharging", "currentTime === info === 1 === " + info);
                            }
                        }
                    }
                });
            }

        } else {
            if ((System.currentTimeMillis() - uploadExitAutoCharging) > 1000 * 60 * 5) {
                com.letianpai.robot.components.network.nets.GeeUiNetManager.uploadExitAutoCharging(DispatchService.this, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {

                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response != null && response.body() != null) {
                            String info = "";
                            if (response != null && response.body() != null) {
                                info = response.body().string();
                            }
                            BaseMessageInfo baseMessageInfo;
                            if (info != null) {
                                uploadExitAutoCharging = System.currentTimeMillis();
                                GeeUILogUtils.logi("letianpai_uploadExitAutoCharging", "currentTime === info === 2 === " + info);
                            }
                        }
                    }
                });
            }

        }
    }

    private void checkService() {
        if (!LetianpaiFunctionUtil.isAppStoreServiceRunning(DispatchService.this)) {
            LetianpaiFunctionUtil.startAppStoreService(DispatchService.this);
        }
        if (!LetianpaiFunctionUtil.isAlarmServiceRunning(DispatchService.this)) {
            GeeUILogUtils.logi("letianpai_alarm_taskservice", "============= create clock2 ============");
            LetianpaiFunctionUtil.startAlarmService(DispatchService.this, null, null);
        }
    }


    /**
     * 检查机器人状态
     */
    private void checkRobotTemp() {
        float temp = LetianpaiFunctionUtil.getCpuThermal();
        TemperatureUpdateCallback.getInstance().setTemperature(temp);
    }

    /**
     * 24小时姿态
     *
     * @param hour
     * @param minute
     */
    private void timeKeeperGesture(int hour, int minute) {
        if (!RobotModeManager.getInstance(DispatchService.this).isRobotMode()) {
            return;
        }
        if (mHour != hour) {
            mHour = hour + 1;
            Random random = new Random();
            mMinute = random.nextInt(59) + 1;
        }
        if (minute == mMinute) {
            RobotModeManager.getInstance(DispatchService.this).switchRobotMode(ViewModeConsts.VM_GESTURE, 1);
            ArrayList<GestureData> data = GestureCenter.hourGestureData(hour);
            if (data != null && data.size() > 0 && data.get(0) != null && data.get(0).getExpression() != null) {
                data.get(0).getExpression().setIs24HourGesture(true);
            }
            GestureCallback.getInstance().setGestures(data, RGestureConsts.GESTURE_ID_24_HOUR);
        }
    }

    private void addModeChangeListeners() {
        ModeChangeCallback.getInstance().setViewModeChangeListener(new ModeChangeCallback.ModeChangeListener() {
            @Override
            public void onViewModeChanged(int viewMode, int modeStatus) {
                GeeUILogUtils.logi(TAG, "addModeChangeListeners()-setViewModeChangeListener: viewMode:" + viewMode + "---:modeStatus:" + modeStatus);
                if (viewMode == ViewModeConsts.VM_DISPLAY_MODE) {
                    // TODO 启动Launcher

                } else if (viewMode == VM_REMOTE_CONTROL_MODE && modeStatus == 1) {
                    try {
                        iLetianpaiService.setRobotStatusCmd(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO);
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO);
                        GeeUILogUtils.logi(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM);
                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if (viewMode == VM_DEMOSTRATE_MODE && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM);
                        GeeUILogUtils.logi(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO);
                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if (viewMode == VM_AUDIO_WAKEUP_MODE && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SPEECH);
                        GeeUILogUtils.logi(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SPEECH);
                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if (viewMode == ViewModeConsts.VM_24_HOUR_PLAY && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_24HOUR);
                        GeeUILogUtils.logi(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_24HOUR);
                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if (viewMode == VM_AUTO_NEW_PLAY_MODE && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT);
                        GeeUILogUtils.logi(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT);
                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if (viewMode == VM_SLEEP_MODE && modeStatus == 1) {
//                    try {
////                        iLetianpaiService.setRobotStatusCmd(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT,RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT);
//                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP);
//                        Log.e(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
                    try {
                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT);
                        GeeUILogUtils.logi(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT);
                    } catch (Exception e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if (viewMode == ViewModeConsts.VM_DEEP_SLEEP_MODE && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP);
                        GeeUILogUtils.logi(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP);

                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if ((viewMode == VM_CHARGING || viewMode == ViewModeConsts.VM_KILL_ROBOT_PROGRESS) && modeStatus == 1) {
                    mTaskHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //延迟是为了等待下一个APP起来之后，再去杀死前一个APP
                                iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME);
                                GeeUILogUtils.logi(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME);
                            } catch (Exception e) {
                                GeeUILogUtils.logi(Log.getStackTraceString(e));
                                e.printStackTrace();
                            }
                        }
                    }, 1000);
                } else if ((viewMode == ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS) && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT);
                    } catch (Exception e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if ((viewMode == ViewModeConsts.VM_KILL_IDENT_PROGRESS) && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.PACKAGE_NAME_IDENT);
                        GeeUILogUtils.logi(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.PACKAGE_NAME_IDENT);
                    } catch (Exception e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if ((viewMode == ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE) && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME + PackageConsts.PACKAGE_NAME_SPLIT + PackageConsts.PACKAGE_NAME_IDENT);
                        String speechCurStatus = SpeechCmdResponser.getInstance(DispatchService.this).getSpeechCurrentStatus();
                        if (!Objects.equals(speechCurStatus, AudioServiceConst.ROBOT_STATUS_SILENCE)) {
                            closeSpeechAudio();
                        }
                    } catch (Exception e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                } else if ((viewMode == ViewModeConsts.VM_TRTC_MONITOR) && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM);
                        iLetianpaiService.setExpression(RobotRemoteConsts.COMMAND_TYPE_FACE, "h0227");
                    } catch (Exception e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }

                } else if ((viewMode == ViewModeConsts.VM_TRTC_TRANSFORM) && modeStatus == 1) {
                    try {
                        iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM);
                        iLetianpaiService.setExpression(RobotRemoteConsts.COMMAND_TYPE_FACE, "h0227");
                    } catch (RemoteException e) {
                        GeeUILogUtils.logi(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                } else if (viewMode == ViewModeConsts.VM_KILL_SPEECH_SERVICE && modeStatus == 1) {
                    String speechCurStatus = SpeechCmdResponser.getInstance(DispatchService.this).getSpeechCurrentStatus();
                    if (!Objects.equals(speechCurStatus, AudioServiceConst.ROBOT_STATUS_SILENCE)) {
                        closeSpeechAudio();
                    }
                }
            }
        });
    }

}
