package com.letianpai.robot.response.robotStatus;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;


/**
 * @author liujunbin
 */
public class RobotStatusResponser {
    private Gson mGson;

    private static RobotStatusResponser instance;
    private Context mContext;
    private boolean isOtaMode = false;
    private boolean isFactoryMode = false;
    private long tapTime;
    private long wakeUpTime;

    private RobotStatusResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static RobotStatusResponser getInstance(Context context) {
        synchronized (RobotStatusResponser.class) {
            if (instance == null) {
                instance = new RobotStatusResponser(context.getApplicationContext());
            }
            return instance;
        }

    }

    private void init() {
        mGson = new Gson();
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        XLog.i("commandDistribute"+"----command: " + command + " /data: " + data);

        if (command == null || data == null) {
            return;
        }

        switch (command) {

            case AppCmdConsts.COMMAND_TYPE_SET_ROBOT_MODE:

                if (data.equals(AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_IN)){
                    openFactoryMode();

                }else if (data.equals(AppCmdConsts.COMMAND_VALUE_FACTORY_MODE_OUT)){
                    closeFactoryMode();

                }else if (data.equals(AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_IN)){
                    openOTAMode();

                }else if (data.equals(AppCmdConsts.COMMAND_VALUE_UPDATE_MODE_OUT)){
                    closeOTAMode();

                }else if (data.equals(AppCmdConsts.COMMAND_VALUE_TO_PREVIOUS_MODE)){
                    if ((LetianpaiFunctionUtil.isVideoCallRunning(mContext))
                            || (LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext))){
                        return;
                    }
                    RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();

                }else if (data.equals(AppCmdConsts.COMMAND_VALUE_CLOCK_START)){
                    GestureCallback.getInstance().setGestures(GestureCenter.clockGestureData(), RGestureConsts.GESTURE_COMMAND_CLOCK_START);

                }else if (data.equals(AppCmdConsts.COMMAND_VALUE_CLOCK_STOP)){
                    GestureCallback.getInstance().setGestures(GestureCenter.closeClockGestureData(), RGestureConsts.GESTURE_COMMAND_CLOCK_STOP);
                   new Handler(mContext.getMainLooper()).postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           //舵机卸力
                           if (RobotModeManager.getInstance(mContext).getRobotMode() == ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT){
                               RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 0);
                           }
                           if(RobotModeManager.getInstance(mContext).isAppMode()){
                               LetianpaiFunctionUtil.changeToStand(mContext);
                           }
                       }
                   }, 2000);
                }
                break;

            default:
        }

    }

    private void closeOTAMode() {
        isOtaMode = false;
    }

    private void openOTAMode() {
        isOtaMode = true;
        GestureCallback.getInstance().setGestures(GestureCenter.stopSoundEffectData(), RGestureConsts.GESTURE_ID_CLOSE_SOUND_EFFECT);
    }

    private void closeFactoryMode() {
        isFactoryMode = false;
    }

    public boolean isFactoryMode() {
        return isFactoryMode;
    }

    private void openFactoryMode() {
        isFactoryMode = true;

    }

    public boolean isFactoryMode1() {
        return LetianpaiFunctionUtil.isFactoryOnTheTop(mContext);
    }

    public boolean isOtaMode1(){
        return  LetianpaiFunctionUtil.isOtaOnTheTop(mContext);
    }

    public boolean isOtaMode() {
        return isOtaMode;
    }

    public boolean isNoNeedResponseMode() {
         if (isOtaMode1()){
            return true;
        }else if (isFactoryMode1()){
            return true;
        }else{
            return false;
        }
    }

    public long getTapTime() {
        return tapTime;
    }

    public void setTapTime() {
        this.tapTime = System.currentTimeMillis();
    }

}
