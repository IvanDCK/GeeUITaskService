package com.letianpai.robot.response.remote;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.letianpai.robot.response.RobotFuncResponseManager.OPEN_TYPE_REMOTE;
import static com.letianpai.robot.response.RobotFuncResponseManager.OPEN_TYPE_SPEECH;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.letianpai.robot.alarm.GeeUIAlarmManager;
import com.letianpai.robot.components.network.nets.GeeUIStatusUploader;
import com.letianpai.robot.components.parser.recharge.ReChargeData;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.manager.SleepModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.mode.callback.ModeChangeCallback;
import com.letianpai.robot.control.storage.RobotConfigManager;
import com.letianpai.robot.control.storage.SPUtils;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.control.system.LetianpaiLightUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.letianpai.robot.response.RobotFuncResponseManager;
import com.letianpai.robot.response.app.AppCmdResponser;
import com.letianpai.robot.response.robotStatus.RobotStatusResponser;
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback;
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.commandlib.consts.MCUCommandConsts;
import com.renhejia.robot.commandlib.consts.RobotExpressionConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.parser.DisplayMode.DisplayMode;
import com.renhejia.robot.commandlib.parser.automode.AutoMode;
import com.renhejia.robot.commandlib.parser.battery.BatterySwitch;
import com.renhejia.robot.commandlib.parser.ble.BleConfig;
import com.renhejia.robot.commandlib.parser.changemode.ModeChange;
import com.renhejia.robot.commandlib.parser.clock.ClockInfos;
import com.renhejia.robot.commandlib.parser.config.UserAppsConfig;
import com.renhejia.robot.commandlib.parser.config.UserAppsConfigModel;
import com.renhejia.robot.commandlib.parser.face.Face;
import com.renhejia.robot.commandlib.parser.light.LightControl;
import com.renhejia.robot.commandlib.parser.showmode.ChangeShowModule;
import com.renhejia.robot.commandlib.parser.sleepmode.SleepModeConfig;
import com.renhejia.robot.commandlib.parser.sound.Sound;
import com.renhejia.robot.commandlib.parser.svolume.VolumeControl;
import com.renhejia.robot.commandlib.parser.timeformat.TimeFormat;
import com.renhejia.robot.commandlib.parser.trtc.TRTC;
import com.renhejia.robot.commandlib.parser.word.Word;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;
import com.renhejia.robot.commandlib.parser.timezone.TimeZone;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author liujunbin
 */
public class RemoteCmdResponser {

    private static String TAG = "RemoteCmdResponser";
    private Gson mGson;
    private static RemoteCmdResponser instance;
    private Context mContext;
    private RemoteCmdResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static RemoteCmdResponser getInstance(Context context) {
        synchronized (RemoteCmdResponser.class) {
            if (instance == null) {
                instance = new RemoteCmdResponser(context.getApplicationContext());
            }
            return instance;
        }
    }

    private void init() {
        mGson = new Gson();
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        //自动切换APP
        if (command.equals("updateAppAutoShow")){
            //保存状态到文件中
            Gson gson = new Gson();
            try {
               Map<String, Object> map = gson.fromJson(data, new TypeToken<HashMap<String, Object>>() {
                }.getType());
               if (map!=null){
                   double isAutoShow =(double) map.get("is_auto_show");
                   //设置内存状态
                    AppCmdResponser.getInstance(mContext).setAutoSwitchApp(isAutoShow == 1);//1,自动切换，0,手动切换
                   SPUtils.getInstance(mContext).putDouble("isAutoShow", isAutoShow);
               }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        XLog.i(TAG+ "---commandDistribute:command " + command + "  data:" + data);
        int robotMode = RobotModeManager.getInstance(mContext).getRobotMode();
        Log.e("---" + TAG, "robotMode_AAA: " + robotMode );
        switch (command) {
            case RobotRemoteConsts.COMMAND_TYPE_MOTION:
            case RobotRemoteConsts.COMMAND_TYPE_ANTENNA_LIGHT:
            case RobotRemoteConsts.COMMAND_TYPE_ANTENNA_MOTION:
                responseRemoteControl(iLetianpaiService, command, data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_SOUND:
                if (LetianpaiFunctionUtil.isRobotOnTheTop(mContext)) {
                    responseSound(iLetianpaiService, data);
                }
                break;

            case RobotRemoteConsts.COMMAND_TYPE_RESET_DEVICE_INFO:
                // 恢复出厂设置网络请求
                LetianpaiFunctionUtil.resetRobot(mContext);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_FACE:
                responseFace(data);
                break;

            case MCUCommandConsts.COMMAND_TYPE_TRTC:
                responseTRTC(data);
                break;

            case MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC:
                responseExitTRTC(data);
                break;

            case MCUCommandConsts.COMMAND_TYPE_TRTC_MONITOR:
                responseTRTCMonitor(data);
                break;

            case MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_MONITOR:
                responseExitTRTCMonitor(data);
                break;

            case MCUCommandConsts.COMMAND_TYPE_TRTC_TRANSFORM:
                responseTRTCTransform(data);
                break;

            case MCUCommandConsts.COMMAND_TYPE_EXIT_TRTC_TRANSFORM:
                responseExitTRTCTransform(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_OTA:
//                responseOTA(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_SEND_WORD:
                responseRemoteWord(iLetianpaiService, data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_DISPLAY_MODE:
                responseDisplayViewControl(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_SOUND_VOLUME:
                responseSoundVolumeControl(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_AUTO_MODE:
                responseControlAutoMode(data, iLetianpaiService);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CHANGE_MODE:
                setRobotMode(iLetianpaiService, data);
                break;

            // 小程序控制
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_CLOCK_DATA:
                if (!LetianpaiFunctionUtil.isAlarmServiceRunning(mContext)) {
                    LetianpaiFunctionUtil.startAlarmService(mContext, data);
                }
//                updateClockData(data);
                break;

            // 日期和时间控制
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_DATE_CONFIG:
                updateSystemTime(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CHANGE_SHOW_MODULE:
                responseChangeApp(iLetianpaiService, data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_AWAKE_CONFIG:
                break;

            // 更新通用配置（原显示模式-时间）
            case RobotRemoteConsts.COMMAND_TYPE_APP_DISPLAY_SWITCH_CONFIG:
                // RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
                break;

            // 1. 更新通用配置（原显示模式-时间）
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_GENERAL_CONFIG:
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_UPDATE_GENERAL_CONFIG, RobotRemoteConsts.COMMAND_TYPE_UPDATE_GENERAL_CONFIG);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
                break;

            // 2. 更新天气配置（原显示模式配置）
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_WEATHER_CONFIG:
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_UPDATE_WEATHER_CONFIG, RobotRemoteConsts.COMMAND_TYPE_UPDATE_WEATHER_CONFIG);
                    // RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            // 3. 更新通用配置（原显示模式配置）
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_CALENDAR_CONFIG:
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_UPDATE_CALENDAR_CONFIG, RobotRemoteConsts.COMMAND_TYPE_UPDATE_CALENDAR_CONFIG);
                    // RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            // 4. 更新通用配置（原显示模式配置）
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_FANS_CONFIG:
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_UPDATE_FANS_CONFIG, RobotRemoteConsts.COMMAND_TYPE_UPDATE_FANS_CONFIG);
                    // RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            // 5. 更新通用配置（原显示模式配置）
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_COUNT_DOWN_CONFIG:
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_UPDATE_COUNT_DOWN_CONFIG, RobotRemoteConsts.COMMAND_TYPE_UPDATE_COUNT_DOWN_CONFIG);
                    // RemoteCmdCallback.getInstance().setRemoteCmd(command, data);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_BATTERY_MODE_CONFIG:
                updateBatteryModeConfig(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_BRIGHTNESS:
                updateRobotBrightness(mContext, data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_REBOOT:
                responseReboot();
                break;

            case RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN:
                responseShutdown();
                break;

            case RobotRemoteConsts.COMMAND_TYPE_START_BIND_MIJIA:
                startMijia(iLetianpaiService);
                break;

            case RobotRemoteConsts.COMMAND_OPEN_AE:
                openAudioEffect(iLetianpaiService);
                break;

            case RobotRemoteConsts.COMMAND_CLOSE_AE:
                closeAudioEffect(iLetianpaiService);
                break;

            case RobotRemoteConsts.COMMAND_SHOW_CHARGING_ICON:
                showChargingIcon();
                break;

            case RobotRemoteConsts.COMMAND_HIDE_CHARGING_ICON:
                hideChargingIcon();
                break;

            case RobotRemoteConsts.COMMAND_CONTROL_TAKE_PHOTO:
                takePhoto();
                break;

            case RobotRemoteConsts.COMMAND_CONTROL_OPEN_DEVELOP_OPTIONS:
                openDevelopOptions();
                break;

            case RobotRemoteConsts.COMMAND_CONTROL_CLOSE_DEVELOP_OPTIONS:
                closeDevelopOptions();
                break;

            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_SLEEP_MODE_CONFIG:
                updateSleepModeInfo(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_BLE_CONFIG:
                responseBleConfigChange(data);
                break;

            case RobotRemoteConsts.COMMAND_VALUE_REMOVE_DEVICE:
                unbindDevice();
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_GYROSCOPE:
                controlGyroscope(data);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_UPDATE_DEVICE_APP_MODE:
                updateLocalApp(iLetianpaiService, data);
                break;

            case RobotRemoteConsts.COMMAND_UPDATE_DEVICE_TIME_ZONE:
                updateTimeZone(iLetianpaiService, data);
                break;

            case RobotRemoteConsts.COMMAND_UPDATE_REAL_BATTERY:
                GeeUIStatusUploader.getInstance(mContext).uploadRobotStatusData();
//                uploadStatus(iLetianpaiService, data);
                break;

            case RobotRemoteConsts.COMMAND_UPDATE_AUTOMATIC_RECHARGE_CONFIG:
                updateAutoChargingConfig(data);
                break;

            //关闭APP
            case RobotRemoteConsts.COMMAND_VALUE_CLOSE_APP:
                ChangeShowModule changeShowModule = mGson.fromJson((String) data, ChangeShowModule.class);
                if (changeShowModule != null) {
                    String packageName = changeShowModule.getSelect_module_tag_list()[0];
                    Log.i("---" + TAG, "close app packagename:" + packageName);
                    if (packageName != null && !packageName.equals("")) {
                        LetianpaiFunctionUtil.closeAppByPackageName(packageName, mContext);
                        LetianpaiFunctionUtil.currentPackageName = "";
                        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
                    }
                }
                break;
            default:
        }
    }

    private void updateAutoChargingConfig(String data) {
        ReChargeData reChargeData = mGson.fromJson(data, ReChargeData.class);
        if (reChargeData != null && reChargeData.getAutomatic_recharge_val() != 0){
            RobotConfigManager.getInstance(mContext).setAutomaticRechargeVal(reChargeData.getAutomatic_recharge_val());
            RobotConfigManager.getInstance(mContext).setAutomaticRechargeSwitch(reChargeData.getAutomatic_recharge_switch());
            RobotConfigManager.getInstance(mContext).commit();
        }
    }

    private void updateTimeZone(ILetianpaiService iLetianpaiService, String data) {
        TimeZone timeZone = mGson.fromJson(data, TimeZone.class);
        if (timeZone != null && !TextUtils.isEmpty(timeZone.getZone())){
            ((AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE)).setTimeZone(timeZone.getZone());
            //设置系统不自动更新时区
            // ContentResolver contentResolver = mContext.getContentResolver();
            // Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME, 0);
            // Settings.Global.putInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 0);
            // String currentTimeZone = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE);
            // Log.d("Timezone", "Current timezone: " + currentTimeZone);
        }
    }

    private void updateLocalApp(ILetianpaiService iLetianpaiService, String data) {

    }

    private void controlGyroscope(String data) {
        CommandResponseCallback.getInstance().setLTPCommand(RobotRemoteConsts.COMMAND_TYPE_CONTROL_GYROSCOPE, data);
    }

    private void responseExitTRTCTransform(String data) {
        RobotModeManager.getInstance(mContext).resetTrtcStatus();
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
        CommandResponseCallback.getInstance().setIdentifyCmd(AppCmdConsts.COMMAND_STOP_APP, AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL);
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE, MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 远程变声
     *
     * @param data
     */
    private void responseTRTCTransform(String data) {
        RobotModeManager.getInstance(mContext).setRobotTrtcStatus(3);
        LetianpaiFunctionUtil.changeToStand(mContext, true);
        LetianpaiFunctionUtil.hideStatusBar();
        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_KILL_IDENT_PROGRESS, 1);
        LetianpaiFunctionUtil.openRobotMode(mContext, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_TRANSFORM, "h0227");
        CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE);

        TRTC trtc = mGson.fromJson(data, TRTC.class);
        if (trtc != null) {
            new Thread(() -> {
                try {
                    //延迟是为了释放资源，防止滋滋声
                    Thread.sleep(1500);
                    openVideoService(trtc.getRoom_id(), trtc.getUser_id(), trtc.getUser_sig());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE, MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE);
    }

    private void responseExitTRTCMonitor(String data) {
        RobotModeManager.getInstance(mContext).resetTrtcStatus();
        // long currentTime = System.currentTimeMillis();
        // LetianpaiFunctionUtil.setExitLongTrtcTime(currentTime);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
        CommandResponseCallback.getInstance().setIdentifyCmd(AppCmdConsts.COMMAND_STOP_APP, AppCmdConsts.VALUE_COMMAND_STOP_VIDEO_CALL);
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE, MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE);
        XLog.i(TAG+"----responseExitTRTCMonitor----");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    XLog.i("letianpai_trtc", "================ 1 ===============");
                    Thread.sleep(2000);
                    CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 视频监控
     *
     * @param data
     */
    private void responseTRTCMonitor(String data) {
        RobotModeManager.getInstance(mContext).setRobotTrtcStatus(1);
        LetianpaiFunctionUtil.changeToStand(mContext, true);
        LetianpaiFunctionUtil.hideStatusBar();
        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_KILL_IDENT_PROGRESS, 1);
        LetianpaiFunctionUtil.openRobotMode(mContext, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_MONITOR, "h0227");
        CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE);

        TRTC trtc = mGson.fromJson(data, TRTC.class);
        if (trtc != null) {
            new Thread(() -> {
                try {
                    //延迟是为了释放资源，防止滋滋声
                    Thread.sleep(1500);
                    openVideoService(trtc.getRoom_id(), trtc.getUser_id(), trtc.getUser_sig());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE, MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE);
    }

    private void responseExitTRTC(String data) {
        RobotModeManager.getInstance(mContext).resetTrtcStatus();
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
        CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE);
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE, MCUCommandConsts.COMMAND_TYPE_STOP_GYROSCOPE);
    }

    private void unbindDevice() {
        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
        GestureCallback.getInstance().setGestures(GestureCenter.stopSoundEffectData(), RGestureConsts.GESTURE_ID_CLOSE_SOUND_EFFECT);
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_UNBIND_DEVICE, 0);
        RobotModeManager.getInstance(mContext).resetStatus();
        //停止服务
        // Intent intent1 = new Intent(mContext, DispatchService.class);
        // mContext.stopService(intent1);
        // new Handler(mContext.getMainLooper()).postDelayed(new Runnable() {
        //     @Override
        //     public void run() {
        //         //解除绑定,重置状态
        //         System.exit(0);
        //     }
        // },3000);
    }

    private void responseBleConfigChange(String data) {
        BleConfig bleConfig = mGson.fromJson((String) data, BleConfig.class);
        if (bleConfig == null) {
            return;
        }
        if (bleConfig.getBle_switch() == 1) {
            LetianpaiFunctionUtil.startBleService(mContext);
        } else if (bleConfig.getBle_switch() == 0) {
            LetianpaiFunctionUtil.stopBleService(mContext);
        }
    }

    /**
     * 更新睡眠模式信息
     */
    private void updateSleepModeInfo(String data) {
        SleepModeConfig sleepModeConfig = mGson.fromJson((String) data, SleepModeConfig.class);
        if (sleepModeConfig == null && TextUtils.isEmpty(sleepModeConfig.getStart_time()) || TextUtils.isEmpty(sleepModeConfig.getEnd_time())) {
            return;
        }
//        if (sleepModeConfig.getSleep_mode_switch() == 1){
        if (sleepModeConfig.getClose_screen_mode_switch() == 1) {
            RobotConfigManager.getInstance(mContext).setCloseScreenModeSwitch(true);
        } else {
            RobotConfigManager.getInstance(mContext).setCloseScreenModeSwitch(false);
        }

        if (sleepModeConfig.getSleep_sound_mode_switch() == 1) {
            RobotConfigManager.getInstance(mContext).setSleepSoundModeSwitch(true);
        } else {
            RobotConfigManager.getInstance(mContext).setSleepSoundModeSwitch(false);
        }

        RobotConfigManager.getInstance(mContext).setSleepTimeStatusModeSwitch(sleepModeConfig.getSleep_time_status_mode_switch());

        int startTime = LetianpaiFunctionUtil.getCompareTime(sleepModeConfig.getStart_time());
        int endTime = LetianpaiFunctionUtil.getCompareTime(sleepModeConfig.getEnd_time());

        XLog.i("letianpai_sleep"+"---sleep_status: " + RobotConfigManager.getInstance(mContext).getSleepModeStatus());
        XLog.i("letianpai_sleep"+ "---startTime: " + startTime);
        XLog.i("letianpai_sleep"+ "---endTime: " + endTime);

        RobotConfigManager.getInstance(mContext).setSleepStartTime(startTime);
        RobotConfigManager.getInstance(mContext).setSleepEndTime(endTime);
        RobotConfigManager.getInstance(mContext).commit();

        updateRobotSleepStatus();
    }

//    private void updateRobotSleepStatus() {
//        Calendar calendar = Calendar.getInstance();
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        int minute = calendar.get(Calendar.MINUTE);
//        int currentTime = LetianpaiFunctionUtil.getCompareTime(hour,minute);
//
//        Log.e("letianpai_sleep","hour: "+ hour);
//        Log.e("letianpai_sleep","minute: "+ minute);
//        Log.e("letianpai_sleep","currentTime: "+ currentTime);
//
//        boolean isInRange = LetianpaiFunctionUtil.isTimeInSleepRange(mContext,hour,minute);
//        boolean isSleepModeOn = RobotConfigManager.getInstance(mContext).getSleepModeStatus();
//        int currentMode = RobotModeManager.getInstance(mContext).getRobotMode();
//        Log.e("letianpai_sleep","isInRange: "+ isInRange);
//        if (isInRange && isSleepModeOn ){
//            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE,1);
//        }else if ((!isInRange || !isSleepModeOn) && (currentMode == ViewModeConsts.VM_SLEEP_MODE) ){
//            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
//        }
//
//    }


    private void updateRobotSleepStatus() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int currentTime = LetianpaiFunctionUtil.getCompareTime(hour, minute);
        XLog.i(TAG + "---hour: " + hour + "---minute::" + minute + "---currentTime::" + minute);
        LetianpaiFunctionUtil.updateRobotSleepStatus(mContext, hour, minute);
    }

    /**
     * 关闭开发者模式
     */
    private void closeDevelopOptions() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        int status1 = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        int status2 = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        XLog.i( TAG+ "---closeDevelopOptions: === 1 === status1: " + status1 + "-----status2::" + status2);
    }

    /**
     * 打开开发者模式
     */
    private void openDevelopOptions() {
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
        int status1 = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        int status2 = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        XLog.i(TAG+ "---openDevelopOptions: === 1 === status1: " + status1 + "-----status2::" + status2);
    }

    private void takePhoto() {
        LetianpaiFunctionUtil.takePhoto(mContext);
    }

    private void hideChargingIcon() {
        RobotModeManager.getInstance(mContext).setShowChargingIconInRobotMode(false);
    }

    private void showChargingIcon() {
        RobotModeManager.getInstance(mContext).setShowChargingIconInRobotMode(true);
    }

    private void closeAudioEffect(ILetianpaiService iLetianpaiService) {
        try {
            iLetianpaiService.setAudioEffect(RobotRemoteConsts.COMMAND_CLOSE_AE, RobotRemoteConsts.COMMAND_CLOSE_AE);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void openAudioEffect(ILetianpaiService iLetianpaiService) {
        try {
            iLetianpaiService.setAudioEffect(RobotRemoteConsts.COMMAND_OPEN_AE, RobotRemoteConsts.COMMAND_OPEN_AE);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void responseShutdown() {
        SystemFunctionUtil.shutdownRobot(mContext);
    }

    private void startMijia(ILetianpaiService iLetianpaiService) {
        LetianpaiFunctionUtil.startMIotService(iLetianpaiService, mContext);
    }

    private void responseReboot() {
        SystemFunctionUtil.reboot(mContext);
    }

    private void updateRobotBrightness(Context mContext, String data) {
        LightControl lightControl = mGson.fromJson(data, LightControl.class);
        if (lightControl != null) {
            LetianpaiLightUtil.setScreenBrightness(mContext, lightControl.getVolume_size());
        }
    }

    private void updateBatteryModeConfig(String data) {
        BatterySwitch batterySwitch = mGson.fromJson(data, BatterySwitch.class);
        if (batterySwitch == null) {
            return;
        }
        int status = batterySwitch.getGeneral_battery_switch();
        XLog.i( TAG+ "---updateBatteryModeConfig---status: " + status);

        if (status == 1) {
            RobotConfigManager.getInstance(mContext).setGeneralBatterySwitchStatus(true);
            RobotConfigManager.getInstance(mContext).commit();
        } else if (status == 0) {
            RobotConfigManager.getInstance(mContext).setGeneralBatterySwitchStatus(false);
            RobotConfigManager.getInstance(mContext).commit();
        }
    }

    private void responseSound(ILetianpaiService iLetianpaiService, String data) {
        Sound sound = mGson.fromJson(data, Sound.class);
        if (sound == null) {
            return;
        }
        String sounds = sound.getSound();
        XLog.i( TAG+ "---responseSound---sounds::" + sounds);

        if (TextUtils.isEmpty(sounds)) {
            return;
        }
        try {
            iLetianpaiService.setAudioEffect(RobotRemoteConsts.COMMAND_TYPE_SOUND, sounds);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void responseRemoteControl(ILetianpaiService iLetianpaiService, String command, String data) {
        if (iLetianpaiService != null && (!TextUtils.isEmpty(command))) {
            try {
                iLetianpaiService.setMcuCommand(command, data);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void responseChangeApp(ILetianpaiService iLetianpaiService, Object data) {
        //test code start
        LetianpaiFunctionUtil.getTopActivity(mContext);
        //test code end

        RobotStatusResponser.getInstance(mContext).setTapTime();
        ChangeShowModule changeShowModule = mGson.fromJson((String) data, ChangeShowModule.class);
        if (changeShowModule != null) {
            String[] changeShowModules = changeShowModule.getSelect_module_tag_list();
            if (changeShowModules != null && changeShowModules.length > 0) {
                String changeModule = changeShowModule.getSelect_module_tag_list()[0];
                switch (changeModule) {
                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_EVENT: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_EVENT_COUNTDOWN);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WEATHER: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_WEATHER);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }
                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_TIME);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }
                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_FANS: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_FANS);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_MESSAGE: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_MESSAGE);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_CUSTOM: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_CUSTOM);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_NEWS: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_NEWS);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_COMMEMORATION: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_COMMEMORATION);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_STOCK: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_STOCK);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WORD: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_WORD);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }
                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_LAMP: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_LAMP);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_SLEEP: {
                        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1);
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        break;
                    }

                    case RobotRemoteConsts.COMMAND_VALUE_CHANGE_ROBOT: {
                        RobotFuncResponseManager.getInstance(mContext).openRobotMode(OPEN_TYPE_REMOTE);
                        break;
                    }
                    default: {
                        LetianpaiFunctionUtil.openUniversalApp(mContext,changeModule, iLetianpaiService);
                    }
                }
            }
        }
    }

    public void closeUniversalApp(String name,ILetianpaiService iLetianpaiService) {
        UserAppsConfigModel userAppsConfigModel = AppCmdResponser.getInstance(mContext).getUserAppsConfigModel();
        if (userAppsConfigModel != null && !userAppsConfigModel.getData().isEmpty()) {
            List<UserAppsConfig> list;
            if (name.contains(".")){//包名过滤
                list = userAppsConfigModel.getData().stream().filter(item -> item.appPackageName.equals(name)).collect(Collectors.toList());
            }else{
                list = userAppsConfigModel.getData().stream().filter(item -> item.appName.equals(name)).collect(Collectors.toList());
            }
            if (!list.isEmpty()) {
                UserAppsConfig userAppsConfig = list.stream().findFirst().get();
                XLog.i(TAG+ "---open others app.appName::" + userAppsConfig.appName + "--appPackageName::" + userAppsConfig.appPackageName + "--openContent::" + userAppsConfig.openContent);

                LetianpaiFunctionUtil.closeAppByPackageName(userAppsConfig.appPackageName, mContext);
                RobotModeManager.getInstance(mContext).switchToPreviousAppMode();
            }
        }
    }

    private void responseSoundVolumeControl(String data) {
        VolumeControl volumeControl = mGson.fromJson(data, VolumeControl.class);
        if (volumeControl != null) {
            SleepModeManager.getInstance(mContext).setRobotVolume(volumeControl.getVolume_size());

//            if (volumeControl.getVolume_size() > 12){
//                LetianpaiLightUtil.setScreenBrightness(mContext,250);
//            }else{
//                LetianpaiLightUtil.setScreenBrightness(mContext,120);
//            }
        }

    }

    private void updateSystemTime(String data) {
        TimeFormat timeFormat = mGson.fromJson(data, TimeFormat.class);
        //TODO 需要Launcher响应系统时间变化
        if (timeFormat != null) {
            if (timeFormat.getHour_24_switch() == 1) {
                SystemFunctionUtil.set24HourFormat(mContext);
            } else {
                SystemFunctionUtil.set12HourFormat(mContext);
            }
        }
    }

    private void updateClockData(String data) {
//        {"clock_id":20,"action":"add","clock_info":{"clock_id":20,"clock_hour":23,"clock_min":39,"clock_time":"23:39","clock_title":"测试一下","is_on":1,"repeat_method":[1,4],"repeat_method_label":"星期一 星期四"}}
        XLog.i(TAG+ "---updateClockData::: " + data);
        ClockInfos clockInfos = mGson.fromJson(data, ClockInfos.class);
        if (clockInfos == null || clockInfos.getClock_id() == 0 || clockInfos.getClock_info() == null || clockInfos.getClock_info().getClock_time() == null) {
            return;
        }
        String[] times = clockInfos.getClock_info().getClock_time().split(":");
        if (times == null && times.length < 2) {
            return;
        }
        int times0 = Integer.parseInt(times[0]);
        int times1 = Integer.parseInt(times[1]);
        XLog.i( TAG+ "---updateClockData---times0:" + times0 + "---times1::" + times1);
        GeeUIAlarmManager.getInstance(mContext).createAlarm(times0, times1);
//        LetianpaiFunctionUtil.openClock(mContext,clockInfos.getClock_info().getClock_time());
//        GeeUIAlarmManager.getInstance(mContext).createAlarm(mContext,Integer.valueOf(times[0]), Integer.valueOf(times[1]));

    }

    /**
     * 响应自动演示模式
     *
     * @param data
     */
    private void responseControlAutoMode(String data, ILetianpaiService iLetianpaiService) {
        XLog.i(TAG+ "---responseControlAutoMode:::响应自动演示模式: " + data);
        AutoMode autoMode = mGson.fromJson(data, AutoMode.class);
        if (autoMode == null || autoMode.getCmd_tag() == null) {
            return;
        }
        String cmd_tag = autoMode.getCmd_tag();
        if (cmd_tag.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_FOLLOW)) {
            openPeopleFollow();

        } else if (cmd_tag.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW)) {
            exitPeopleFollow(iLetianpaiService);
        }
    }


    private void exitPeopleFollow(ILetianpaiService iLetianpaiService) {
        if (iLetianpaiService != null) {
            try {
//                iLetianpaiService.setCommand(new LtpCommand(RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW, null));
                //TODO 第二个参数没用
                iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW, RobotRemoteConsts.COMMAND_VALUE_CONTROL_AUTO_MODE_EXT_FOLLOW);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void responseRemoteWord(ILetianpaiService iLetianpaiService, String data) {
        Word word = mGson.fromJson(data, Word.class);
        if (word != null && (word.getWord() != null)) {
//            ExpressionChangeCallback.getInstance().showRemoteText(word.getWord());
            try {
//                iLetianpaiService.setTTS("speakText",word.getWord());
                if (word.getWord().equals(RobotRemoteConsts.COMMAND_OPEN_AE)) {
                    openAudioEffect(iLetianpaiService);
                } else if (word.getWord().equals(RobotRemoteConsts.COMMAND_CLOSE_AE)) {
                    closeAudioEffect(iLetianpaiService);
                } else {
                    iLetianpaiService.setTTS("speakText", word.getWord());
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置机器人模式
     *
     * @param data
     */
    private void setRobotMode(ILetianpaiService iLetianpaiService, String data) {
        XLog.i( TAG+ "---setRobotMode 1---data::" + data);

        ModeChange modeChange = mGson.fromJson(data, ModeChange.class);
        if (modeChange == null || modeChange.getMode() == null) {
            return;
        }
        String mode = modeChange.getMode();
        XLog.i(TAG+ "----setRobotMode 2---mode::" + mode);

        int modeStatus = modeChange.getMode_status();
        switch (mode) {
            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRANSFORM:
                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_REMOTE_CONTROL_MODE, modeStatus);

                break;

            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SHOW:
            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_STATIC:
//                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DISPLAY_MODE, mode_status);
                if (ChargingUpdateCallback.getInstance().isCharging() && (!RobotModeManager.getInstance(mContext).isAppMode())) {
                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DISPLAY_MODE, RobotModeManager.getInstance(mContext).getRobotAppModeStatus());
                } else {
                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DISPLAY_MODE, RobotModeManager.getInstance(mContext).getRobotAppModeStatus());
                }

                RobotFuncResponseManager.stopRobot(iLetianpaiService);
                break;

            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SLEEP:
                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, modeStatus);
                break;

            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_AUTO:
                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_MODE, modeStatus);
                break;

            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO:
                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DEMOSTRATE_MODE, modeStatus);
                break;

            case RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT:

//                if (ChargingUpdateCallback.getInstance().isCharging() && (!RobotModeManager.getInstance(mContext).isRobotSleepMode())) {
//                    RobotModeManager.getInstance(mContext).switchRobotMode(com.letianpai.robot.control.mode.ViewModeConsts.VM_SLEEP_MODE, 1);
//                } else {
//                    Log.e("letianpai_test_control", "switchToNewAutoPlayMode === COMMAND_VALUE_CHANGE_MODE_ROBOT ======= 4 ============");
//                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, mode_status);
//                }

                RobotFuncResponseManager.getInstance(mContext).openRobotMode(OPEN_TYPE_SPEECH);
                break;
        }

    }

    /**
     * 设置远程控制
     *
     * @param data
     */
    private void responseDisplayViewControl(String data) {
        XLog.i(TAG+ "---responseDisplayViewControl---设置远程控制: " + data);
        DisplayMode displayMode = mGson.fromJson(data, DisplayMode.class);
        if (displayMode == null || displayMode.getCmd_tag() == null) {
            return;
        }
        String name = displayMode.getCmd_tag();
        String localName = "";
        if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_TIME)) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_TIME;

        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_WEATHER)) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_WEATHER;

        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_COUNTDOWN)) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_COUNTDOWN;

        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_FANS)) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_FANS;

        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_SCHEDULE)) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_SCHEDULE;

        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_EMPTY)) {
            localName = RobotRemoteConsts.LOCAL_COMMAND_VALUE_CONTROL_DISPLAY_EMPTY;

        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_BLACK)) {
            ExpressionChangeCallback.getInstance().showBlackView(true);
        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CONTROL_DISPLAY_EXT_BLACK)) {
            ExpressionChangeCallback.getInstance().showBlackView(false);
        }

        if (!TextUtils.isEmpty(localName)) {
            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DEMOSTRATE_MODE, ViewModeConsts.VIEW_MODE_IN);
            ExpressionChangeCallback.getInstance().showDisplayView(localName);
        } else if (name.equals(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_RESET)) {
            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_DEMOSTRATE_MODE, ViewModeConsts.VIEW_MODE_OUT);
        }
    }

    /**
     * 收到TRTC 的消息，音视频通话
     *
     * @param data
     */
    private void responseTRTC(String data) {
        RobotModeManager.getInstance(mContext).setRobotTrtcStatus(2);
        LetianpaiFunctionUtil.changeToStand(mContext, true);
        LetianpaiFunctionUtil.hideStatusBar();
        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
        CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE);
        TRTC trtc = mGson.fromJson(data, TRTC.class);
        if (trtc != null) {
            new Thread(() -> {
                try {
                    //延迟是为了释放资源，防止滋滋声
                    Thread.sleep(1500);
                    openVideos(trtc.getRoom_id(), trtc.getUser_id(), trtc.getUser_sig());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE, MCUCommandConsts.COMMAND_TYPE_START_GYROSCOPE);
    }

    private void openVideos(int room_id, String user_id, String user_sig) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.rhj.aduioandvideo", "com.tencent.trtc.videocall.VideoCallingActivity"));
        intent.putExtra("roomId", room_id);
        intent.putExtra("userId", user_id);
        intent.putExtra("userSig", user_sig);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

    private void openVideoService(int room_id, String user_id, String user_sig) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.rhj.aduioandvideo", "com.tencent.trtc.videocall.VideoService"));
        intent.putExtra("roomId", room_id);
        intent.putExtra("userId", user_id);
        intent.putExtra("userSig", user_sig);
        mContext.startService(intent);
    }

    /**
     * 打开人脸跟随
     */
    private void openPeopleFollow() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.rockchip.gpadc.yolodemo", "com.rockchip.gpadc.demo.MainActivity"));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void responseFace(String data) {
        XLog.i(TAG+ "---responseFace--解析人脸: " + data);
        Face robotFace = mGson.fromJson(data, Face.class);
        String face = robotFace.getFace();
        if (TextUtils.isEmpty(face)) {
            return;
        }
        if (face.equals(RobotExpressionConsts.L_FACE_MAIN_IMAGE)) {

        } else {
            ExpressionChangeCallback.getInstance().setExpression(face);
        }

    }

}
