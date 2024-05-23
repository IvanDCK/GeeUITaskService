package com.letianpai.robot.control.manager;

import static com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_BODY;
import static com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_FACE;
import static com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_HAND;

import android.content.Context;

import android.util.Log;
import com.elvishew.xlog.XLog;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.callback.RobotCommandWordsCallback;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.mode.callback.ModeChangeCallback;
import com.letianpai.robot.control.service.DispatchService;
import com.letianpai.robot.control.storage.RobotConfigManager;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.letianpai.robot.response.robotStatus.RobotStatusResponser;
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback;
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import java.util.ArrayList;

/**
 * 模式管理
 *
 * @author liujunbin
 */
public class RobotModeManager implements ViewModeConsts {

    private static String TAG = "RobotModeManager";
    private static RobotModeManager instance;
    private Context mContext;
    private int robotModeBeforeSleepMode;
    private int robotModeBeforeCharging;
    private boolean isRobotModeBeforeCharging;
    private int robotTempStatus = VM_STATIC_MODE; // 没有演示模式的
    private int robotMiddleTempStatus; //

    private int robotMode = VM_STATIC_MODE;


    /**
     * 机器人当前的模式status，可能会跟robotAppModeStatus有重合的值。主要要来区分当前模式的进入和退出。
     */
    private int robotModeStatus = 0;


    /**
     * 只在静态模式下的状态，状态用来区分各个APP
     */
    private int robotAppModeStatus;


    /**
     * 1，代表远程监控
     * 2，代表视频通话
     * 3，代表远程变声
     * 4，代表演示模式
     */
    private int robotTrtcStatus = -1;

    private boolean isAlarming;
    public ArrayList<String> modeList = new ArrayList<>();
    private boolean isShowChargingIconInRobotMode;

    //机器人是否进入悬崖
    private boolean isInTofMode;
    private boolean isInCliffMode;
    private ILetianpaiService iLetianpaiService;
    public void setiLetianpaiService(ILetianpaiService iLetianpaiService) {
        this.iLetianpaiService = iLetianpaiService;
    }

    private RobotModeManager(Context context) {
        this.mContext = context;
        init(context);
    }

    public static RobotModeManager getInstance(Context context) {
        synchronized (RobotModeManager.class) {
            if (instance == null) {
                instance = new RobotModeManager(context.getApplicationContext());
            }
            return instance;
        }
    }

    public void resetStatus(){
        robotTempStatus = VM_STATIC_MODE;
        robotMiddleTempStatus = 0;
        robotMode = VM_UNBIND_DEVICE;
        robotModeStatus = 0;
        robotAppModeStatus = 0;
        robotTrtcStatus = -1;
    }

    private void init(Context context) {
        this.mContext = context;
    }

    public int getRobotTrtcStatus() {
        return robotTrtcStatus;
    }

    public void setRobotTrtcStatus(int robotTrtcStatus) {
        this.robotTrtcStatus = robotTrtcStatus;
    }

    public void resetTrtcStatus(){
        robotTrtcStatus = -1;
    }

    public int getRobotMode() {
        return robotMode;
    }

    /**
     * @return
     */
    public boolean isRobotWakeupMode() {
        if (robotMode == VM_AUDIO_WAKEUP_MODE || robotMode == VM_AUDIO_WAKEUP_MODE_DEFAULT) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isRobotDeepSleepMode() {
        GeeUILogUtils.logi(TAG, "---isRobotDeepSleepMode ---- robotMode: " + robotMode);
        if (robotMode == VM_DEEP_SLEEP_MODE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isSleepMode() {
        if (robotMode == VM_SLEEP_MODE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isRestMode() {
        if (robotMode == VM_SLEEP_MODE) {
            return true;

        } else if (robotMode == VM_BLACK_SCREEN_SLEEP_MODE) {
            return true;

//        } else if (robotMode == VM_BLACK_SCREEN_NIGHT_SLEEP_MODE) {
//            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isCloseScreenMode() {
        if (robotMode == VM_BLACK_SCREEN_NIGHT_SLEEP_MODE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isPowerOnChargingMode() {
        if (robotMode == VM_POWER_ON_CHARGING) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isAppMode() {
        if (robotMode == VM_STATIC_MODE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isRobotSleepMode() {
        if (robotMode == VM_SLEEP_MODE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isRobotMode() {
        if (robotMode == VM_AUTO_NEW_PLAY_MODE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isCommonRobotMode() {
        GeeUILogUtils.logi("letianpai_robot_status", "---robotMode: " + robotMode);
        if (robotMode == VM_AUTO_NEW_PLAY_MODE
                || robotMode == VM_DEMOSTRATE_MODE
                || robotMode == VM_REMOTE_CONTROL_MODE
                || robotMode == VM_AUDIO_WAKEUP_MODE
                || robotMode == VM_AUDIO_WAKEUP_MODE_DEFAULT
                || robotMode == VM_HAND_REG_MODE
                || robotMode == VM_TAKE_PHOTO) {
            return true;
        } else {
            return false;
        }
    }

    public int getRobotAppModeStatus() {
        return robotAppModeStatus;
    }

    public int getRobotModeStatus() {
        return robotModeStatus;
    }

    public void setRobotModeStatus(int robotModeStatus) {
        this.robotModeStatus = robotModeStatus;
    }

    //app打开成功之后，再去记录模式
    public void switchRobotMode(int mode, int status) {
        GeeUILogUtils.logi(TAG, "----switchRobotMode: " + mode + "   " + status);
        //工厂模式或者ota的话，不允许切换
        if (RobotStatusResponser.getInstance(mContext).isNoNeedResponseMode()) {
            GeeUILogUtils.logi(TAG, "----switchRobotMode: " + RobotStatusResponser.getInstance(mContext).isNoNeedResponseMode());
            return;
        }
        //如果设备已经解绑，不响应
        if (mode == VM_UNBIND_DEVICE){
            return;
        }
        //记录模式的进入和退出，目前只用在充电响应唤醒相关的地方LetianpaiFunctionUtil.responseCharging里面
        robotModeStatus = status;

        switch (mode) {
            case VM_STANDBY_MODE:
                switchToStandbyMode();
                break;
            case VM_STATIC_MODE:
                this.robotTempStatus = VM_STATIC_MODE;
                switchToDisplayMode(status);
                break;

            case VM_CHARGING_MODE:
                switchToChargingMode();
                break;
            case VM_SLEEP_MODE:
                if (status == 1) {
                    gotoSleep();
                } else {
                    switchToPreviousPlayMode();
                }
                break;

            case VM_BLACK_SCREEN_SLEEP_MODE:
                if (status == 1) {
                    gotoBlackScreenSleep();
                } else {
                    switchToPreviousPlayMode();
                }
                break;

            case VM_BLACK_SCREEN_NIGHT_SLEEP_MODE:
                if (status == 1) {
                    gotoNightBlackScreenSleep();
                } else {
                    switchToPreviousPlayMode();
                }
                break;

            case VM_DEEP_SLEEP_MODE:
                gotoDeepSleepMode(status);
                break;

            case VM_FUNCTION_MODE:
                switchToFunctionMode();
                break;

            case VM_REMOTE_CONTROL_MODE:
                switchToRemoteControlMode(status);
                break;

            case VM_DEMOSTRATE_MODE:
                switchToDemonstrateMode(mode, status);

                break;
            case VM_AUTO_PLAY_MODE:
                switchToAutoPlayMode();
                break;

            case VM_AUTO_NEW_PLAY_MODE:
//                if (isRestMode() && ChargingUpdateCallback.getInstance().isCharging()) {
                LetianpaiFunctionUtil.updateModeStatusOnServer(mContext, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT);
                if (ChargingUpdateCallback.getInstance().isCharging()) {
                    this.robotTempStatus = VM_AUTO_NEW_PLAY_MODE;
                    this.robotMode = VM_AUTO_NEW_PLAY_MODE;
                    return;
                }
                switchToNewAutoPlayMode();
                break;

            case VM_AUDIO_WAKEUP_MODE:
                switchAudioWakeupMode(status);
                break;

            case VM_AUDIO_WAKEUP_MODE_DEFAULT:
                screenOn();
                this.robotMode = VM_AUDIO_WAKEUP_MODE_DEFAULT;
                break;

            case VM_HAND_REG_MODE:
                openHandReg(status);
                break;

            case VM_BODY_REG_MODE:
                openBodyReg(status);
                break;

            case VM_FACE_REG_MODE:
                openFaceReg(status);
                break;

            case VM_GESTURE:
                //暂时不需要做什么行为,应该是停掉左右的交互
                this.robotMode = VM_GESTURE;
                break;

            case VM_MI_IOT_MODE:
                this.robotMode = VM_MI_IOT_MODE;
                break;

            case VM_EMOTION:
                if (status == 1) {
                    this.robotMode = VM_EMOTION;
                }
                break;
            case VM_TAKE_PHOTO:
                openTakePhoto(status);
                break;

            case VM_POWER_ON_CHARGING:
                if (status == 1) {
                    this.robotMode = VM_POWER_ON_CHARGING;
                }
                responsePowerCharging();
                break;

            case VM_AUTO_CHARGING:
                if (status == 1) {
                    this.robotMode = VM_AUTO_CHARGING;
                    LetianpaiFunctionUtil.openAutoCharging(mContext);
                }
                break;
        }
    }

    private void responsePowerCharging() {
        //如果充电就走机器人动的姿态，如果不是充电直接进入静态模式
        if (ChargingUpdateCallback.getInstance().isCharging()) {
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, false);
            GestureCallback.getInstance().setGestures(GestureCenter.powerOnChargingGestureData(), RGestureConsts.GESTURE_POWER_ON_CHARGING);
        }else{
            switchToPreviousAppMode();
        }
    }

    /**
     *
     */
    private void gotoDeepSleepMode(int status) {
        GeeUILogUtils.logi("letianpai_deep_sleep", " =================================== gotoDeepSleepMode ==== 0 ===================================gstatus： " + status);
        if (status == 1) {
            GeeUILogUtils.logi("letianpai_deep_sleep", "controlSteeringEngine_========================== gotoDeepSleepMode ==== 1 ==========================");
//            LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false);
            LetianpaiFunctionUtil.changeToStand(mContext);
//            LetianpaiFunctionUtil.openTimeViewForSleep1(mContext);
            LetianpaiFunctionUtil.openTimeViewForDeepSleep(mContext);

            if (RobotConfigManager.getInstance(mContext).getSleepSoundModeSwitch()) {
                GestureCallback.getInstance().setGestures(GestureCenter.goToHibernationGesture1(), RGestureConsts.GESTURE_COMMAND_HIBERNATION);
            }
//            GestureCallback.getInstance().setGestures(GestureCenter.goToHibernationGesture1(), RGestureConsts.GESTURE_COMMAND_HIBERNATION);
            //TODO 打开Launcher的动画界面
            CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE);
            ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_DEEP_SLEEP_MODE, 1);
            GeeUILogUtils.logi("letianpai_deep_sleep", " =================================== gotoDeepSleepMode ==== 3 =================================== ");
//            SleepModeManager.getInstance(mContext).setRobotVolume(5);
            this.robotMode = VM_DEEP_SLEEP_MODE;
        } else {
            GeeUILogUtils.logi("letianpai_deep_sleep", " =================================== gotoDeepSleepMode ==== 4 =================================== ");
//            RobotModeManager.getInstance(mContext).switchToPreviousRobotMode();
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
            GestureCallback.getInstance().setGestures(GestureCenter.stopHibernationGesture(), RGestureConsts.GESTURE_COMMAND_EXIT_HIBERNATION);
            CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE);
//            SleepModeManager.getInstance(mContext).setRobotVolume(RobotConfigManager.getInstance(mContext).getRobotVolume());
        }

    }

    private void gotoSleep() {
        GeeUILogUtils.logi("letianpai_birthday", "---robotMode: " + robotMode);

        this.robotModeBeforeSleepMode = robotMode;
        this.robotMode = VM_SLEEP_MODE;
        RobotCommandWordsCallback.getInstance().showBattery(false);
        LetianpaiFunctionUtil.openTimeViewForSleep(mContext);
        // ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS, 1);
        //播放睡眠音乐
        GestureCallback.getInstance().setGestures(GestureCenter.goToHibernationGesture1(), RGestureConsts.GESTURE_COMMAND_HIBERNATION);
    }

    private void gotoBlackScreenSleep() {
        GeeUILogUtils.logi("letianpai_sleep_test_gotoBlackScreenSleep", "---robotMode: " + robotMode);
        gotoScreenSleep(VM_BLACK_SCREEN_SLEEP_MODE);
    }

    private void gotoScreenSleep(int mode) {
        GeeUILogUtils.logi("letianpai_sleep_test_repeat", "=========== 1 =======");
        GeeUILogUtils.logi("letianpai_sleep_test_letianpai_birthday", "---robotMode ==== 1 ====: " + robotMode);
//        if (robotMode == mode) {
//            Log.e("letianpai_sleep_test_letianpai_birthday", "robotMode ==== 2 ====: " + robotMode);
//            return;
//        }
        GeeUILogUtils.logi("letianpai_sleep_test_letianpai_birthday", "---robotMode ==== 3 ====: " + robotMode);
        this.robotModeBeforeSleepMode = robotMode;
        this.robotMode = mode;
//        RobotCommandWordsCallback.getInstance().showBattery(false);
        LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false);
        ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
        com.letianpai.robot.control.system.SystemFunctionUtil.goToSleep(mContext);

    }

    private void gotoNightBlackScreenSleep() {
        GeeUILogUtils.logi("letianpai_birthday", "---gotoNightBlackScreenSleep: ======");
        gotoScreenSleep(VM_BLACK_SCREEN_NIGHT_SLEEP_MODE);
    }


    private void openHandReg(int status) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 2 ============== ");
        screenOn();
        if (status == 1) {
//            ModeChangeCallback.getInstance().setModeChange(VM_HAND_REG_MODE,1);
            LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_HAND, "h0025");
            GeeUILogUtils.logi("letianpai_control", "---controlSteeringEngine_========================== 4 ==========================");
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            //TODO 增加一个姿态
//            GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_HAND_RECOGNITION);
            this.robotMode = VM_HAND_REG_MODE;
        } else if(status == 2){ //打开ai相关的activity，走手势的模式
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            this.robotMode = VM_HAND_REG_MODE;
        }else {
            switchToPreviousPlayMode();
        }
    }

    private void openBodyReg(int status) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 3 ============== ");
        screenOn();
        if (status == 1) {
            LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_BODY, "h0025");
            GeeUILogUtils.logi("letianpai_control", "---controlSteeringEngine_========================== 4 ==========================");
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            //TODO 增加一个姿态
//            GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_HAND_RECOGNITION);
            this.robotMode = VM_BODY_REG_MODE;
        } else {
            switchToPreviousPlayMode();
        }
    }

    private void openFaceReg(int status) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 4 ============== ");
        screenOn();
        if (status == 1) {
            LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_FACE, "h0025");
            GeeUILogUtils.logi("letianpai_control", "---controlSteeringEngine_========================== 4 ==========================");
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            //TODO 增加一个姿态
//            GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_HAND_RECOGNITION);
            this.robotMode = VM_FACE_REG_MODE;
        } else {
            switchToPreviousPlayMode();
        }
    }

    private void openTakePhoto(int status) {
        screenOn();
        if (status == 1) {
            if (robotMode == VM_TAKE_PHOTO) {
                return;
            }
            if (LetianpaiFunctionUtil.openTakePhoto(mContext)){
                this.robotMode = VM_TAKE_PHOTO;
            }
            // LetianpaiFunctionUtil.openRobotMode(mContext, COMMAND_VALUE_CHANGE_MODE_TAKE_PHOTO, "h0182");
            // LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            // Intent intent = new Intent();
            // intent.setComponent(new ComponentName("com.ltp.ident", "com.ltp.ident.services.TakePhotoService"));
            // mContext.startService(intent);
            // this.robotMode = VM_TAKE_PHOTO;
        } else {
            //如果是other模式，关闭speech
            GeeUILogUtils.logi(TAG, "openTakePhoto: robotAppModeStatus:"+robotAppModeStatus);
            if (robotAppModeStatus == ViewModeConsts.APP_MODE_OTHER){
                ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
            }
            switchToPreviousPlayMode();
        }
    }

    private long preTime = 0;
    public void switchToPreviousPlayMode() {
        //防止不知道那里的逻辑过快切换导致的问题
        long currentTime = System.currentTimeMillis();
        if (currentTime - preTime < 2000) return;
        preTime = currentTime;
        boolean isCharging = ChargingUpdateCallback.getInstance().isCharging();
        GeeUILogUtils.logi(TAG, "---switchToPreviousPlayMode\n" +
                "--:isCharging:"+isCharging +
                "\n---:robotMiddleTempStatus:"+robotMiddleTempStatus +
                "\n---:robotTempStatus:"+robotTempStatus +
                "\n---:robotModeStatus:"+robotModeStatus +
                "\n---:robotAppModeStatus:"+robotAppModeStatus);
        if (isCharging) {
            LetianpaiFunctionUtil.responseCharging(mContext);
        } else {
            if (robotMiddleTempStatus != 0) {
                switchRobotMode(robotMiddleTempStatus, 1);
            } else {
                if (robotTempStatus == VM_STATIC_MODE) {
                    switchRobotMode(VM_STATIC_MODE, robotAppModeStatus);
                    ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS, 1);
                } else {
                    switchRobotMode(robotTempStatus, 1);
                }
            }
        }
    }

    public boolean isPreviousModeIsAppMode() {
        if (robotMiddleTempStatus != 0) {
            return false;
        } else {
            if (robotTempStatus == VM_STATIC_MODE) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isPreviousModeIsRobotMode() {

        if (robotTempStatus == VM_AUTO_NEW_PLAY_MODE) {
            return true;
        } else {
            return false;
        }
    }


    // public void switchToPreviousPlayModeNew() {
    //     Log.e("letianpai_sleep_test", " =================================== letianpai_sleep_test ==== 1 =================================== ");
    //
    //     Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 1 =================================== robotMiddleTempStatus: " + robotMiddleTempStatus);
    //     if (robotMiddleTempStatus != 0) {
    //         Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 2 =================================== robotMiddleTempStatus: " + robotMiddleTempStatus);
    //         if (ChargingUpdateCallback.getInstance().isCharging()) {
    //             LetianpaiFunctionUtil.responseCharging(mContext);
    //         } else {
    //             switchRobotMode(robotMiddleTempStatus, 1);
    //         }
    //
    //     } else {
    //         Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 3 =================================== robotMiddleTempStatus: " + robotMiddleTempStatus);
    //         if (robotTempStatus == VM_STATIC_MODE) {
    //             Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 4 =================================== robotTempStatus: " + robotTempStatus);
    //             Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 5 =================================== robotAppModeStatus: " + robotAppModeStatus);
    //             switchRobotMode(VM_STATIC_MODE, robotAppModeStatus);
    //             // TODO killRobot
    //             ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ROBOT_IDENT_PROGRESS, 1);
    //
    //         } else {
    //             if (ChargingUpdateCallback.getInstance().isCharging()) {
    //                 LetianpaiFunctionUtil.responseCharging(mContext);
    //             } else {
    //                 Log.e("letianpai_deep_sleep", " =================================== switchToPreviousPlayMode ==== 6 =================================== robotTempStatus: " + robotTempStatus);
    //                 switchRobotMode(robotTempStatus, 1);
    //             }
    //
    //         }
    //     }
    //
    // }

    public void switchToPreviousAppMode() {
        RobotCommandWordsCallback.getInstance().showBattery(true);
        switchRobotMode(VM_STATIC_MODE, robotAppModeStatus);
    }

    private void switchToAutoPlayMode() {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 7 ============== ");
        screenOn();
//        GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_TEST0);
    }

    /**
     * 进入展示模式
     */
    private void switchToNewAutoPlayMode() {
        RobotCommandWordsCallback.getInstance().showBattery(false);
        robotMiddleTempStatus = VM_AUTO_NEW_PLAY_MODE;
        this.robotMode = VM_AUTO_NEW_PLAY_MODE;
        ModeChangeCallback.getInstance().setModeChange(VM_AUTO_NEW_PLAY_MODE, 1);
        screenOn();
        // TODO 启动机器人模式APP
//        CommandResponseCallback.getInstance().setLTPCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,new PowerMotion(3,1).toString()));
        LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
        LetianpaiFunctionUtil.openRobotMode(mContext, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT, null);
        this.robotTempStatus = VM_AUTO_NEW_PLAY_MODE;

    }

    /**
     * `
     * 停止自动
     */
    private void stopAutoPlayMode() {
        stopRobotDispatchService();
    }

    private void startRobotDispatchService() {
        // TODO 启动机器人模式APP
//        Intent intent = new Intent(mContext, RobotDispatchService.class);
//        mContext.startService(intent);
    }

    private void stopRobotDispatchService() {
        //TODO 停止机器人服务
    }

    private void screenOn() {
        SystemFunctionUtil.wakeUp(mContext);
    }

    private void switchToChargingMode() {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 11 ============== ");
        screenOn();

    }

    public void restoreRobotAppModeStatus() {
        robotAppModeStatus = 0;
    }

    public void switchToDisplayMode(int status) {
        GeeUILogUtils.logi("RobotModeManager", "---switchToDisplayMode  status:: "+status);
        screenOn();

        if (status == 0 || status == ViewModeConsts.APP_MODE_TIME) {
            //只有APP打开成功了，才应该记录这个状态值
            syncRobotStatus(status);
            LetianpaiFunctionUtil.openTime(mContext);
        } else if (status == ViewModeConsts.APP_MODE_WEATHER) {
            if(LetianpaiFunctionUtil.openWeather(mContext)){
                //只有APP打开成功了，才应该记录这个状态值
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_EVENT_COUNTDOWN) {
            if (LetianpaiFunctionUtil.openEventCountdown(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_FANS) {
            if (LetianpaiFunctionUtil.openFans(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_MESSAGE) {
            if (LetianpaiFunctionUtil.openMessage(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_NEWS) {
            if (LetianpaiFunctionUtil.openNews(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_CUSTOM) {
            if (LetianpaiFunctionUtil.openCustom(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_COMMEMORATION) {
            if(LetianpaiFunctionUtil.openCommemoration(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_STOCK) {
            if(LetianpaiFunctionUtil.openStock(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_WORD) {
            if(LetianpaiFunctionUtil.openWord(mContext)){
                syncRobotStatus(status);
            }
        } else if (status == ViewModeConsts.APP_MODE_LAMP) {
            if(LetianpaiFunctionUtil.openLamp(mContext)){
                syncRobotStatus(status);
            }
        } else if(status == ViewModeConsts.APP_MODE_EXPRESSION){
            if(LetianpaiFunctionUtil.openExpressionApp(mContext)){
                syncRobotStatus(status);
            }
        }else if(status == ViewModeConsts.APP_MODE_OTHER){
            LetianpaiFunctionUtil.resetOtherAppStatus(mContext);
            syncRobotStatus(status);
        } else if (status == ViewModeConsts.APP_MODE_MIJIA) {
            //米家不需要修改APP status
            LetianpaiFunctionUtil.openBindMijia(mContext);
        }

        if (ChargingUpdateCallback.getInstance().isCharging()) {
            RobotModeManager.getInstance(mContext).setRobotModeBeforeCharging();
        }
    }

    //同步机器人各种状态
    private void syncRobotStatus(int status){
        robotAppModeStatus = status;
        robotMiddleTempStatus = 0;
        robotTempStatus = VM_STATIC_MODE;
        robotMode = VM_STATIC_MODE;
    }

    public void ttsUninstallAppText(){
        //未安装
        String text = "";
        if (SystemFunctionUtil.isChinese()) {
            text = "未安装该应用";
        } else {
            text = "The app is not installed";
        }
        ttsContent(text);
    }

    private void ttsContent(String ttsText){
        try {
            if (iLetianpaiService != null){
                iLetianpaiService.setTTS("speakText", ttsText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchToStandbyMode() {
//        ExpressionChangeCallback.getInstance().showMainImage();
    }

    private void switchToFunctionMode() {
        GeeUILogUtils.logi("letianpai_sleep_screen", "---screenOn  ========== 14 ============== ");
        screenOn();

    }

    /**
     * 切换到语音唤醒
     */
    private void switchAudioWakeupMode(int modeStatus) {
        GeeUILogUtils.logi("RobotModeManager", "---switchAudioWakeupMode:: "+modeStatus + "---isRegMode()--"+isRegMode());
        screenOn();
        if (modeStatus == 0) {
            if (!isRegMode()) {
                switchToPreviousPlayMode();
            }
        } else if (modeStatus == 1) {
            this.robotMode = VM_AUDIO_WAKEUP_MODE;
            ExpressionChangeCallback.getInstance().showDisplayView(null);
            GeeUILogUtils.logi("uiTaskService", "---switchAudioWakeupMode ======= 1 ======= : VM_AUDIO_WAKEUP_MODE" + robotMode);
        } else if (modeStatus == 2) {
            this.robotMode = VM_AUDIO_WAKEUP_MODE;
            GeeUILogUtils.logi("uiTaskService", "---switchAudioWakeupMode ======= 1 ======= : VM_AUDIO_WAKEUP_MODE" + robotMode);
        }
    }

    public boolean isRegMode() {
        if (this.robotMode == VM_HAND_REG_MODE || this.robotMode == VM_BODY_REG_MODE || this.robotMode == VM_FACE_REG_MODE) {
            return true;
        }
        return false;
    }

    /**
     * 切换至演示模式
     */
    private void switchToDemonstrateMode(int mode, int modeStatus) {
        GeeUILogUtils.logi("letianpai_sleep_screen", "----screenOn  ========== 16 ============== ");
        screenOn();
        if (modeStatus == 1) {
            RobotModeManager.getInstance(mContext).setRobotTrtcStatus(4);
            RobotCommandWordsCallback.getInstance().showBattery(false);
            // robotMiddleTempStatus = VM_DEMOSTRATE_MODE;
            LetianpaiFunctionUtil.openRobotMode(mContext, "demo", null);
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            //关闭speech唤醒
            CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_STOP_AUDIO_SERVICE);
            this.robotMode = mode;

        } else {
            RobotModeManager.getInstance(mContext).resetTrtcStatus();
            RobotCommandWordsCallback.getInstance().showBattery(true);
            //打开speech唤醒
            CommandResponseCallback.getInstance().setRobotStatusCmdResponse(AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_START_AUDIO_SERVICE);
            // robotMiddleTempStatus = 0;
            switchToPreviousPlayMode();
        }
    }


    private void switchToRemoteControlMode(int status) {
        if (status == 1) {
            RobotCommandWordsCallback.getInstance().showBattery(false);
            robotMiddleTempStatus = VM_REMOTE_CONTROL_MODE;
            ModeChangeCallback.getInstance().setModeChange(VM_REMOTE_CONTROL_MODE, 1);
            LetianpaiFunctionUtil.openRobotMode(mContext, "demo", null);
            GeeUILogUtils.logi("letianpai_control", "---controlSteeringEngine_========================== 9 ==========================");
            LetianpaiFunctionUtil.controlSteeringEngine(mContext, true, true);
            this.robotMode = VM_REMOTE_CONTROL_MODE;
        } else {
            RobotCommandWordsCallback.getInstance().showBattery(true);
            robotMiddleTempStatus = 0;
            //TODO 退回上一个常态操作
            switchToPreviousPlayMode();
        }
    }

    public void setAlarming(boolean alarming) {
        isAlarming = alarming;
    }

    public boolean isAlarming() {
        return isAlarming;
    }


    public void setRobotModeBeforeSleepMode(int robotModeBeforeSleepMode) {
        this.robotModeBeforeSleepMode = robotModeBeforeSleepMode;
    }

    public void setRobotModeBeforeCharging() {
        this.robotModeBeforeCharging = robotMode;
//        this.robotModeBeforeCharging = robotTempStatus;
    }

    public void setRobotModeBeforeChargingOn(boolean isRobotMode) {
        this.isRobotModeBeforeCharging = isRobotMode;
//        this.robotModeBeforeCharging = robotTempStatus;
    }

    public boolean isRobotModeBeforeCharging() {
        return isRobotModeBeforeCharging;
    }

//    public int getRobotModeBeforeCharging() {
//        return robotModeBeforeCharging;
//    }

    public boolean getRobotModeBeforeChargingIsRobot() {
        return robotModeBeforeCharging == VM_AUTO_NEW_PLAY_MODE;
    }

    public boolean isShowChargingInRobotMode() {
        return isShowChargingIconInRobotMode;
    }

    public void setShowChargingIconInRobotMode(boolean showChargingIconInRobotMode) {
        isShowChargingIconInRobotMode = showChargingIconInRobotMode;
    }

    public boolean isNoExpressionMode() {
        if (LetianpaiFunctionUtil.isVideoCallOnTheTop(mContext) || LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext)
                || (LetianpaiFunctionUtil.isAutoAppOnTheTop(mContext))) {
            return true;
        }
        return false;
    }

    public void setRobotMode(int robotMode) {
        this.robotMode = robotMode;
    }

    public void setToRobotMode() {
        setRobotMode(VM_AUTO_NEW_PLAY_MODE);
    }

    public void setInCliffMode(boolean inCliffMode) {
        isInCliffMode = inCliffMode;
    }

    public boolean isInCliffMode() {
        return isInCliffMode;
    }

    public void setInTofMode(boolean inTofMode) {
        isInTofMode = inTofMode;
    }

    public boolean isInTofMode() {
        return isInTofMode;
    }

}
