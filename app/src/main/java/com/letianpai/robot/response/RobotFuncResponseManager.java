package com.letianpai.robot.response;

import static com.letianpai.robot.control.mode.ViewModeConsts.VM_BODY_REG_MODE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import android.util.Log;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.renhejia.robot.commandlib.consts.PackageConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.consts.SpeechConst;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

/**
 * @author liujunbin
 */
public class RobotFuncResponseManager {

    private static RobotFuncResponseManager instance;
    private Context mContext;
    public static final String OPEN_TYPE_SPEECH = "voice";
    public static final String OPEN_TYPE_REMOTE = "remote";


    private RobotFuncResponseManager(Context context) {
        this.mContext = context;
        init(context);
    }

    public static RobotFuncResponseManager getInstance(Context context) {
        synchronized (RobotModeManager.class) {
            if (instance == null) {
                instance = new RobotFuncResponseManager(context.getApplicationContext());
            }
            return instance;
        }
    }


    private void init(Context context) {
        this.mContext = context;


    }

    /**
     * @param openType
     */
    public void openCommemoration(String openType) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_COMMEMORATION);
    }

    /**
     * @param openType
     */
    public void openPeopleSearch(String openType) {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_BODY_REG_MODE, 1);
        Intent intent = new Intent();
        ComponentName cn = new ComponentName(PackageConsts.PACKAGE_NAME_IDENT, PackageConsts.SERVICE_NAME_IDENT);
        intent.setComponent(cn);
        mContext.startService(intent);
    }

    /**
     * @param openType
     */
    public void closePeopleSearch(String openType) {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_BODY_REG_MODE, 0);
        Intent intent = new Intent();
        ComponentName cn = new ComponentName(PackageConsts.PACKAGE_NAME_IDENT, PackageConsts.SERVICE_NAME_IDENT);
        intent.setComponent(cn);
        mContext.stopService(intent);
    }

    /**
     * 打开机器人模式
     *
     * @param openTypeSpeech
     */
    public void openRobotMode(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1);
        if (ChargingUpdateCallback.getInstance().isCharging()) {
            try {
                LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LetianpaiFunctionUtil.responseCharging(mContext);
        }
    }

    /**
     * 打开机器人模式
     *
     * @param openTypeSpeech
     */
    public void closeRobotMode(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 0);
    }

    public void openWeather(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_WEATHER);
    }

    public void openSleepMode(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1);

    }

    public void closeSleepMode(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 0);

    }

    public void openEventCountdown(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_EVENT_COUNTDOWN);
    }

    public void openNews(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_NEWS);
    }

    public void openMessage(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_MESSAGE);
    }

    public void openStock(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_STOCK);
    }

    public void openCustom(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_CUSTOM);
    }

    public void openSwitchApp(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchToPreviousAppMode();
    }

    public void openWord(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_WORD);
    }

    public void openLamp(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_LAMP);
    }

    public void openTime(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_TIME);
    }

    public void openFans(String openTypeSpeech) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_FANS);
    }

    public void openPetsMode(String openTypeSpeech) {
        openRobotMode(openTypeSpeech);
    }

    public void openUpgrade(String openTypeSpeech) {
        LetianpaiFunctionUtil.startGeeUIOtaService(mContext);
    }

    public static void closeSpeechAudioAndListen(ILetianpaiService iLetianpaiService) {
        Log.i("<<<", "closeSpeechAudioAndListen");
        try {
            iLetianpaiService.setTTS(SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO_AND_LISTENING, SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeSpeechAudio(ILetianpaiService iLetianpaiService) {
        Log.d("<<<", "closeSpeechAudio");
        try {
            iLetianpaiService.setTTS(SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO, SpeechConst.COMMAND_CLOSE_SPEECH_AUDIO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeApp(ILetianpaiService iLetianpaiService) {
        try {
            iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP, RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public static void stopRobot(ILetianpaiService iLetianpaiService) {
        try {
            iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_EXIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void responseSensorEvent(ILetianpaiService iLetianpaiService) {
        try {
            iLetianpaiService.setAppCmd(PackageConsts.ROBOT_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_SENSOR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
