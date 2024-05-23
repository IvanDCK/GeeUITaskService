package com.letianpai.robot.control.manager;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;

import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.mode.callback.ModeChangeCallback;
import com.letianpai.robot.control.storage.RobotConfigManager;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.letianpai.robot.taskservice.audio.Const;
import com.letianpai.robot.taskservice.audio.parser.AudioCommand;
import com.letianpai.robot.control.callback.GestureCallback;
import com.renhejia.robot.commandlib.consts.ATCmdConsts;
import com.renhejia.robot.commandlib.consts.MCUCommandConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.parser.motion.Motion;
import com.renhejia.robot.gesturefactory.manager.GestureManager;
import com.renhejia.robot.gesturefactory.parser.GestureData;
import com.renhejia.robot.gesturefactory.util.GestureConsts;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import java.util.ArrayList;

/**
 * 语音命令执行单元
 *
 * @author liujunbin
 */
public class CommandResponseManager {

    private static CommandResponseManager instance;
    private Context mContext;

    private CommandResponseManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
    }

    public static CommandResponseManager getInstance(Context context) {
        synchronized (CommandResponseManager.class) {
            if (instance == null) {
                instance = new CommandResponseManager(context.getApplicationContext());
            }
            return instance;
        }
    }

    public static void commandResponse(Context context, String commandFrom, String commandType, Object commandData, ILetianpaiService iLetianpaiService) {
        if (commandFrom == null) {
            return;
        }

        if (commandType.equals(Const.RhjController.move)) {
            responseMove(commandData, iLetianpaiService);

        } else if (commandType.equals(Const.RhjController.congraturationBirthday)) {
            if (instance == null) {
                instance = new CommandResponseManager(context.getApplicationContext());
            }
            CommandResponseManager.getInstance(instance.mContext).responseGestures(GestureConsts.GESTURE_BIRTHDAY, iLetianpaiService);

        } else if (commandType.equals(Const.DUIController.ShutDown)) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SystemFunctionUtil.shutdownRobot(context);
                }
            }, 2000);


        } else if (commandType.equals(Const.RhjController.turn)) {


        } else if (commandType.equals(RobotRemoteConsts.COMMAND_TYPE_SOUND)) {


        } else if (commandType.equals(MCUCommandConsts.COMMAND_AUDIO_TURN_AROUND)) {
            turnDirection(iLetianpaiService, ATCmdConsts.AT_STR_MOVEW_LOCAL_ROUND_LEFT, (int) commandData);
        }
    }

    private static void turnDirection(ILetianpaiService iLetianpaiService, String direction, int number) {
        try {
            iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, new Motion(direction, number).toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param commandData
     * @param iLetianpaiService
     */
    public static void responseMove(Object commandData, ILetianpaiService iLetianpaiService) {
        if (commandData instanceof AudioCommand) {
            String direction = ((AudioCommand) commandData).getDirection();
            String number = ((AudioCommand) commandData).getNumber();
            if (number != null && direction != null) {
                if (direction.equals("前")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_FORWARD;
                } else if (direction.equals("后")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND;
                } else if (direction.equals("左")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND;
                } else if (direction.equals("右")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND;
                }
                int numberInt = Integer.parseInt(number);
                try {
//                    iLetianpaiService.setCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_MOTION, (new Motion(direction, numberInt)).toString()));
                    iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, new Motion(direction, numberInt).toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    int i = 0;

    public void responseGesture(GestureData gestureData, ILetianpaiService iLetianpaiService) {
        responseGestureData(mContext, gestureData, iLetianpaiService);
    }

    public void responseGestures(String gesture, ILetianpaiService iLetianpaiService) {
        ArrayList<GestureData> list = GestureManager.getInstance(mContext).getRobotGesture(gesture);
        responseGestures(list, -1, iLetianpaiService);
    }

    public void responseGestures(String gesture, int gestureId, ILetianpaiService iLetianpaiService) {
        GeeUILogUtils.logi("AudioCmdResponseManager", "responseGestures: gesture=" + gesture + " gestureId: " + gestureId);
        ArrayList<GestureData> list = GestureManager.getInstance(mContext).getRobotGesture(gesture);
        responseGestures(list, gestureId, iLetianpaiService);
    }

    public void responseGestures(ArrayList<GestureData> list, int taskId, ILetianpaiService iLetianpaiService) {
        GestureDataThreadExecutor.getInstance().execute(() -> {
            GeeUILogUtils.logi("AudioCmdResponseManager", "run start: taskId:" + taskId);
            for (GestureData gestureData : list) {
                responseGesture(gestureData, iLetianpaiService);
                try {
                    if (gestureData.getInterval() == 0) {
                        Thread.sleep(2000);
                    } else {
                        Thread.sleep(gestureData.getInterval());
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            GeeUILogUtils.logi("AudioCmdResponseManager", "run end: taskId:" + taskId);
            GestureCallback.getInstance().setGesturesComplete("list", taskId);
        });
    }

    public static void responseGestureData(Context context, GestureData gestureData, ILetianpaiService iLetianpaiService) {

        logGestureData(gestureData);

        if (gestureData == null) {
            return;
        }

        try {
            if (gestureData.getTtsInfo() != null) {
                //响应单元在Launcher
                iLetianpaiService.setTTS("speakText", gestureData.getTtsInfo().getTts());
            }


            if (!RobotModeManager.getInstance(context).isNoExpressionMode() && gestureData.getExpression() != null) {
//            if (gestureData.getExpression() != null) {
                //响应单元在Launcher
//                iLetianpaiService.setCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_FACE, ((gestureData.getExpression()).toString())));
                // TODO 增加对表情判断
                // TODO && LetianpaiFunctionUtil.isRobotRunning(context)
                if (!LetianpaiFunctionUtil.isRobotOnTheTop(context) && LetianpaiFunctionUtil.isRobotRunning(context) ){
                    LetianpaiFunctionUtil.openRobotFace(context,GestureConsts.GESTURE_DEMO,gestureData.getExpression().getFace());
                }else if (!LetianpaiFunctionUtil.isRobotOnTheTop(context) && !LetianpaiFunctionUtil.isRobotRunning(context)){
                    LetianpaiFunctionUtil.openRobotFace(context,GestureConsts.GESTURE_DEMO,gestureData.getExpression().getFace());
                }else{
                    if (gestureData != null && gestureData.getExpression() != null && gestureData.getExpression().getFace() != null){
                        if (gestureData.getExpression().isIs24HourGesture()){
                            ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_24_HOUR_PLAY,1);
                            LetianpaiFunctionUtil.controlSteeringEngine(context,true,true);
                        }
                        iLetianpaiService.setExpression(RobotRemoteConsts.COMMAND_TYPE_FACE, (gestureData.getExpression().getFace()));
                    }
                }
            }
            if (gestureData.getAntennalight() != null) {
                //响应单元在MCUservice
                iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_ANTENNA_LIGHT, (gestureData.getAntennalight()).toString());
            }
            if (gestureData.getSoundEffects() != null) {
                //如果是睡眠模式，并且有打开呼噜声音的话
                if (RobotModeManager.getInstance(context).isSleepMode()){
                    if (RobotConfigManager.getInstance(context).getSleepSoundModeSwitch()){
                        iLetianpaiService.setAudioEffect(RobotRemoteConsts.COMMAND_TYPE_SOUND,gestureData.getSoundEffects().getSound());
                    }
                }else{
                    iLetianpaiService.setAudioEffect(RobotRemoteConsts.COMMAND_TYPE_SOUND,gestureData.getSoundEffects().getSound());
                }
            }

            if (gestureData.getFootAction() != null && ((!ChargingUpdateCallback.getInstance().isCharging()) || (RobotModeManager.getInstance(instance.mContext).isPowerOnChargingMode())) ) {
                //响应单元在MCUservice
                iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, (gestureData.getFootAction()).toString());
            }

            if (gestureData.getEarAction() != null) {
                //响应单元在MCUservice
                iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_ANTENNA_MOTION, (gestureData.getEarAction()).toString());
            } else {
                //天线
//                AntennaMotion antennaMotion=new AntennaMotion("sturn");
//                iLetianpaiService.setCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_ANTENNA_MOTION, (gestureData.getEarAction()).toString()));
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }



    private static void logGestureData(GestureData gestureData) {
        GeeUILogUtils.logi("AudioCmdResponseManager", "解析给实际执行单元 " + gestureData);
    }


}
