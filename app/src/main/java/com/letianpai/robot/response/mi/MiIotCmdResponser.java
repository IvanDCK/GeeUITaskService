package com.letianpai.robot.response.mi;

import static com.letianpai.robot.control.mode.ViewModeConsts.VM_MI_IOT_MODE;

import android.content.Context;

import com.google.gson.Gson;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.MIIotConsts;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.gesturefactory.manager.GestureResPool;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

/**
 * @author liujunbin
 */
public class MiIotCmdResponser {
    private Gson mGson;

    private static MiIotCmdResponser instance;
    private Context mContext;
    private GestureResPool gestureResPool;

    private MiIotCmdResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static MiIotCmdResponser getInstance(Context context) {
        synchronized (MiIotCmdResponser.class) {
            if (instance == null) {
                instance = new MiIotCmdResponser(context.getApplicationContext());
            }
            return instance;
        }

    }

    private void init() {
        mGson = new Gson();
//        gestureResPool = new GestureResPool(mContext);
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        if (!SystemUtil.getRobotActivateStatus()){
            return;
        }
        if (command == null) {
            return;
        }

        switch (command) {
            case MIIotConsts.MI_SAY_HELLO:
                sayHello();
                break;
                
            case MIIotConsts.MI_FELL_COLD:
                fellCold();
                break;
                
            case MIIotConsts.MI_FELL_HOT:
                fellHot();
                break;
                
            case MIIotConsts.MI_COOKING_FINISH:
                cookingFinish();
                break;
                
            case MIIotConsts.MI_SLEEP_MODE:
                miSleepMode();
                break;
                
            case MIIotConsts.MI_SMOKE_ALARM:
                smokeAlarm();
                break;

            case MIIotConsts.MI_GAS_ALARM:
                gasAlarm();
                break;
                
            case MIIotConsts.MI_WATER_ALARM:
                waterAlarm();
                break;

            default:
                break;
        }
    }

    private void waterAlarm() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miWaterAlarm(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void gasAlarm() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miGasAlarm(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void smokeAlarm() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miSmokeAlarm(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void miSleepMode() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miSleepMode(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void cookingFinish() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miCookingFinish(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void fellHot() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miFeelHot(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void fellCold() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miFeelCold(), RGestureConsts.GESTURE_MI_IOT);
    }

    private void sayHello() {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_MI_IOT_MODE,1);
        GestureCallback.getInstance().setGestures(GestureCenter.miSayHello(), RGestureConsts.GESTURE_MI_IOT);
    }


}
