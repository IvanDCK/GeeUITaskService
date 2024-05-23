package com.letianpai.robot.response.sensor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.callback.RobotCommandWordsCallback;
import com.letianpai.robot.control.callback.TemperatureUpdateCallback;
import com.letianpai.robot.control.consts.AudioServiceConst;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.mode.callback.ModeChangeCallback;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.response.RobotFuncResponseManager;
import com.letianpai.robot.response.robotStatus.RobotStatusResponser;

import com.letianpai.robot.response.speech.SpeechCmdResponser;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.gesturefactory.manager.GestureResPool;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author liujunbin
 */
public class SensorCmdResponser {

    private static String TAG = "SensorCmdResponser";
    private Gson mGson;
    private static SensorCmdResponser instance;
    private Context mContext;
    private GestureResPool gestureResPool;
    Intent intent;
    private String preCommand;
    private long preCommandTime;


    private SensorCmdResponser(Context context) {
        this.mContext = context;
    }

    public static SensorCmdResponser getInstance(Context context) {
        synchronized (SensorCmdResponser.class) {
            if (instance == null) {
                instance = new SensorCmdResponser(context.getApplicationContext());
            }
            return instance;
        }
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        GeeUILogUtils.logi(TAG, "commandDistribute:command: " + command + "  data: " + data);

        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext)){
            GeeUILogUtils.logi(TAG, "commandDistribute:command: LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext): "  + LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext));
            return;
        }
        switch (command) {
            //悬空
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_START_DATA:
                if (!RobotModeManager.getInstance(mContext).isRobotMode()) {
                    return;
                }
                if (fillCurrentCmdInfo(command)) {
                    startPrecipice();
                }
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_PRECIPICE_STOP_DATA:
                if (!RobotModeManager.getInstance(mContext).isRobotMode()) {
                    return;
                }
                if (fillCurrentCmdInfo(command)) {
                    stopPrecipice();
                }
                break;

             //悬崖开始
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_FORWARD:
                if (fillCurrentCmdInfo(command)) {
                    if (!ChargingUpdateCallback.getInstance().isCharging() && !RobotModeManager.getInstance(mContext).isRestMode()) {
                        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_DEMOSTRATE_MODE, 1);
                        fallForward();
                    }
                }
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_LEFT:
                if (fillCurrentCmdInfo(command)) {
                    if (!ChargingUpdateCallback.getInstance().isCharging() && !RobotModeManager.getInstance(mContext).isRestMode()) {
                        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_DEMOSTRATE_MODE, 1);
                        fallLeft();
                    }
                }
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_RIGHT:
                if (fillCurrentCmdInfo(command)) {
                    if (!ChargingUpdateCallback.getInstance().isCharging() && !RobotModeManager.getInstance(mContext).isRestMode()) {
                        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_DEMOSTRATE_MODE, 1);
                        fallRight();
                    }
                }
                break;

            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_BACKEND:
                if (fillCurrentCmdInfo(command)) {
                    if (!ChargingUpdateCallback.getInstance().isCharging() && !RobotModeManager.getInstance(mContext).isRestMode()) {
                        ModeChangeCallback.getInstance().setModeChange(com.letianpai.robot.control.mode.ViewModeConsts.VM_DEMOSTRATE_MODE, 1);
                        fallBackend();
                    }
                }
                break;
            //悬崖结束
            //倒下反馈开始
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_START_DATA:
                if (fillCurrentCmdInfo(command)) {
                    if (!ChargingUpdateCallback.getInstance().isCharging()
                            && !RobotModeManager.getInstance(mContext).isRestMode()
                            && !RobotModeManager.getInstance(mContext).isAppMode()
                            && !RobotModeManager.getInstance(mContext).isRegMode()) {
                        startFallDown();
                    }
                }
                break;

            //倒下反馈结束
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_FALL_DOWN_STOP_DATA:
                if (fillCurrentCmdInfo(command)) {
                    if (!ChargingUpdateCallback.getInstance().isCharging()
                            && !RobotModeManager.getInstance(mContext).isRestMode()
                            && !RobotModeManager.getInstance(mContext).isAppMode()
                            && !RobotModeManager.getInstance(mContext).isRegMode()) {
                        stopFallDown();
                    }
                }
                break;
            // -----------------------------------------触摸反馈 start
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA:
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_DOUBLE_TAP_DATA:
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_LONG_PRESS_DATA:
                responseFunctionTap(iLetianpaiService, command, data);
                break;
            // -----------------------------------------触摸反馈 end
            //摇晃
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_WAGGLE:
                fillCurrentCmdInfo(command);
                if (!ChargingUpdateCallback.getInstance().isCharging() && !RobotModeManager.getInstance(mContext).isRestMode()) {
                    waggle(iLetianpaiService);
                }
                break;

            //避障
            case RobotRemoteConsts.COMMAND_TYPE_CONTROL_TOF:
                fillCurrentCmdInfo(command);
                if (!ChargingUpdateCallback.getInstance().isCharging()
                        && !RobotModeManager.getInstance(mContext).isRestMode()
                        && (!LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext))
                        && (RobotModeManager.getInstance(mContext).getRobotMode() != com.letianpai.robot.control.mode.ViewModeConsts.VM_POWER_ON_CHARGING)
                        && !RobotModeManager.getInstance(mContext).isRegMode()) {
                    tofSensorHand(iLetianpaiService);
                }
                break;
            default:
                break;
        }

    }

    private boolean fillCurrentCmdInfo(String command) {
        if (command == null) {
            return false;
        }
        long current = System.currentTimeMillis();
        if (command.equals(preCommand)) {

            if ((current - preCommandTime) > 400) {
                preCommand = command;
                preCommandTime = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }
        } else {
            preCommand = command;
            preCommandTime = System.currentTimeMillis();
            return true;
        }

    }

    private void responseFunctionTap(ILetianpaiService iLetianpaiService, String command, String data) {
        boolean isHigh = TemperatureUpdateCallback.getInstance().isInHighTemperature();
        boolean isAlarmRunning = LetianpaiFunctionUtil.isAlarmRunning(mContext);
        int trtcStatus = RobotModeManager.getInstance(mContext).getRobotTrtcStatus();
        GeeUILogUtils.logi(TAG, "responseFunctionTap: command:"+command +"----data:"+data
                + "\n--isHigh:"+isHigh
                + "\n--isAlarmRunning:"+isAlarmRunning
                + "\n--trtcStatus:"+trtcStatus);

        if (LetianpaiFunctionUtil.isSpeechRunning(mContext) || LetianpaiFunctionUtil.isLexRunning(mContext)) {
            String speechStatus = SpeechCmdResponser.getInstance(mContext).getSpeechCurrentStatus();
            //只有音乐和跳舞的时候，才需要拍头关闭speech。其他的时候拍头是唤醒speech
            if (speechStatus.equals(AudioServiceConst.ROBOT_STATUS_MUSIC)){
                //如果充电的时候，播放音乐的时候，拍头，直接关闭speech。需要把逻辑从原来的方法里面慢慢提出来，不然后面会越乱
                if (ChargingUpdateCallback.getInstance().isCharging()){
                    //关闭两次的原因是，第一次关闭，speech会发送silence，
                    // 但是思必驰并不会关闭界面。第二次关闭的时候，speech才会关闭界面。
                    // 也就是speech在发出slience之后，收到关闭的指令，才会关闭界面。
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService);
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService);
                }else{
                    //先拉起机器人再关闭speech
                    RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
                    RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService);
                }
            }else{
                RobotFuncResponseManager.closeSpeechAudioAndListen(iLetianpaiService);
            }
            return;
        }

        if (isHigh) {
            GestureCallback.getInstance().setGestures(GestureCenter.getHighTempTTSGestureData(), RGestureConsts.GESTURE_COMMAND_HIGH_TEMP_TTS);
        } else if (isAlarmRunning) {
            RobotFuncResponseManager.closeApp(iLetianpaiService);
        }else if ( trtcStatus != -1) {
            return;
        }
        // else if ((LetianpaiFunctionUtil.isVideoCallOnTheTop(mContext)) || (LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext))) {
        //     Log.i(TAG, "responseFunctionTap: isVideoCallOnTheTop:"+LetianpaiFunctionUtil.isVideoCallOnTheTop(mContext) );
        //     Log.i(TAG, "responseFunctionTap: isVideoCallServiceRunning:"+LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext));
        //     return;
        // }
        else if (RobotModeManager.getInstance(mContext).isRestMode() && (!ChargingUpdateCallback.getInstance().isCharging())) {
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();

        } else if (RobotModeManager.getInstance(mContext).isCloseScreenMode() && (!ChargingUpdateCallback.getInstance().isCharging())) {
            RobotStatusResponser.getInstance(mContext).setTapTime();
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();

        } else {
            if (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_TAP_DATA)) {
                RobotFuncResponseManager.stopRobot(iLetianpaiService);
                tap();

            } else if (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_DOUBLE_TAP_DATA)) {
                RobotFuncResponseManager.stopRobot(iLetianpaiService);
                doubleTap();

            } else if (command.equals(RobotRemoteConsts.COMMAND_TYPE_CONTROL_LONG_PRESS_DATA)) {
                RobotFuncResponseManager.stopRobot(iLetianpaiService);
                longPressTap();
            }
        }
    }

    private void startPrecipice() {
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "startPrecipice");
        GestureCallback.getInstance().setGestures(GestureCenter.danglingGestureData(), RGestureConsts.GESTURE_ID_DANGLING_START);
    }

    private void stopPrecipice() {
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "stopPrecipice");
        GestureCallback.getInstance().setGestures(GestureCenter.getFallGroundGesture(), RGestureConsts.GESTURE_ID_DANGLING_END);
    }

    /**
     * 倒下姿态开始
     */
    private void startFallDown() {
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "startPrecipice: 倒下姿态开始");
        GestureCallback.getInstance().setGestures(GestureCenter.getFallDownGesture(), RGestureConsts.GESTURE_ID_FALLDOWN_START);
    }

    /**
     * 倒下姿态结束
     */
    private void stopFallDown() {
        hideStatusBar();
        RobotModeManager.getInstance(mContext).setInTofMode(false);
        RobotModeManager.getInstance(mContext).setInCliffMode(false);
        GeeUILogUtils.logi(TAG, "stopFallDown: 倒下姿态结束");
        GestureCallback.getInstance().setGestures(GestureCenter.getFallGroundGesture(), RGestureConsts.GESTURE_ID_FALLDOWN_END);
    }

    //-------------------------------触摸反馈start
    private void tap() {
        GeeUILogUtils.logi(TAG, "单击---");
        hideStatusBar();
        RobotModeManager.getInstance(mContext).setInTofMode(false);
        RobotModeManager.getInstance(mContext).setInCliffMode(false);
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1);
        GestureCallback.getInstance().setGestures(GestureCenter.getTapGesture(), RGestureConsts.GESTURE_ID_TAP);
    }

    private void doubleTap() {
        GeeUILogUtils.logi(TAG, "双击---");
        hideStatusBar();
        RobotModeManager.getInstance(mContext).setInTofMode(false);
        RobotModeManager.getInstance(mContext).setInCliffMode(false);
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1);
        GestureCallback.getInstance().setGestures(GestureCenter.getdoubleTapGesture(), RGestureConsts.GESTURE_ID_DOUBLE_TAP);
    }

    private void longPressTap() {
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "长按---");
        RobotModeManager.getInstance(mContext).setInTofMode(false);
        RobotModeManager.getInstance(mContext).setInCliffMode(false);
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1);
        GestureCallback.getInstance().setGestures(GestureCenter.getLongPressGesture(), RGestureConsts.GESTURE_ID_LONG_PRESS);
    }

    //-------------------------------触摸反馈end

    /**
     * 防跌落
     */
    private void fallForward() {
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "fallForward: 防跌落前进");
        RobotModeManager.getInstance(mContext).setInCliffMode(true);
        GestureCallback.getInstance().setGestures(GestureCenter.fallForwardGestureData(), RGestureConsts.GESTURE_ID_CLIFF_FORWARD);
    }

    private void fallBackend() {

        hideStatusBar();
        GeeUILogUtils.logi(TAG, "fallBackend: 防跌落后退");
        RobotModeManager.getInstance(mContext).setInCliffMode(true);
        GestureCallback.getInstance().setGestures(GestureCenter.fallBackendGestureData(), RGestureConsts.GESTURE_ID_CLIFF_BACKEND);
    }

    private void fallLeft() {

        hideStatusBar();
        GeeUILogUtils.logi(TAG, "fallLeft: 防跌落往左");
        RobotModeManager.getInstance(mContext).setInCliffMode(true);
        GestureCallback.getInstance().setGestures(GestureCenter.fallLeftGestureData(), RGestureConsts.GESTURE_ID_CLIFF_LEFT);
    }

    private void fallRight() {
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "fallRight: 防跌落往右");
        RobotModeManager.getInstance(mContext).setInCliffMode(true);
        GestureCallback.getInstance().setGestures(GestureCenter.fallRightGestureData(), RGestureConsts.GESTURE_ID_CLIFF_RIGHT);
    }

    /**
     * 摇晃
     */
    private void waggle(ILetianpaiService iLetianpaiService) {
        RobotFuncResponseManager.stopRobot(iLetianpaiService);
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "waggle: 摇晃");
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1);
        GestureCallback.getInstance().setGestures(GestureCenter.getWaggleGesture(), RGestureConsts.GESTURE_ID_WAGGLE);
    }


    private final Object lock = new Object(); // 锁对象
    private final AtomicInteger tofCount = new AtomicInteger(0);
    private final AtomicLong startTofTime = new AtomicLong(0);
    private boolean isIgnoreTof = false;
    private boolean isTofTrigger = false;

    private void resetTofVariables() {
        isTofTrigger = false;
        isIgnoreTof = false;
        tofCount.set(0);
        startTofTime.set(0);
    }

    //这里的为临时逻辑
    // private void tofSensor(ILetianpaiService iLetianpaiService) {
    //     long currentTime = System.currentTimeMillis();
    //     synchronized (lock) {
    //         if (startTofTime.get() != 0) {
    //             long diffTime = currentTime - startTofTime.get();
    //             if (diffTime >= 1000 * 60 * 10) { // 超过10分钟后处理避障
    //                 resetTofVariables();
    //             }
    //         }
    //
    //         if (isIgnoreTof) {
    //             return;
    //         }
    //
    //         if (tofCount.get() >= 5) {
    //             resetTofVariables();
    //             LogUtils.logd(TAG, "关闭避障");
    //             // 关闭避障
    //             isIgnoreTof = true;
    //             startTofTime.set(System.currentTimeMillis());
    //         } else if (!isTofTrigger) {
    //             // 触发了避障
    //             isTofTrigger = true;
    //             // 记录开始时间
    //             startTofTime.set(System.currentTimeMillis());
    //         } else {
    //             tofCount.incrementAndGet();
    //         }
    //
    //         //处理避障
    //         tofSensorHand(iLetianpaiService);
    //     }
    // }

    private void tofSensorHand(ILetianpaiService iLetianpaiService) {
        RobotFuncResponseManager.stopRobot(iLetianpaiService);
        hideStatusBar();
        GeeUILogUtils.logi(TAG, "sensor tof: 避障");

        if (RobotModeManager.getInstance(mContext).isRobotWakeupMode() ||
                RobotModeManager.getInstance(mContext).isSleepMode() ||
                RobotModeManager.getInstance(mContext).isRobotDeepSleepMode()

        ) {
            return;
        }

        boolean isInCliffMode = RobotModeManager.getInstance(mContext).isInCliffMode();
        boolean isInTofMode = RobotModeManager.getInstance(mContext).isInTofMode();
        if (isInCliffMode || isInTofMode) {
            GeeUILogUtils.logi(TAG, "isInCliffMode:::"+isInCliffMode +"-----isInTofMode:::"+isInTofMode);
            return;
        }

        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_ONESHOT_MODE, 1);
        RobotModeManager.getInstance(mContext).setInTofMode(true);
        GestureCallback.getInstance().setGestures(GestureCenter.getTofGesture(), RGestureConsts.GESTURE_ID_Tof);

    }

    private void hideStatusBar() {
        RobotCommandWordsCallback.getInstance().showBattery(false);
    }


}
