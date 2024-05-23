package com.letianpai.robot.response.speech;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.letianpai.robot.control.mode.ViewModeConsts.*;
import static com.letianpai.robot.response.RobotFuncResponseManager.OPEN_TYPE_SPEECH;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import com.google.gson.Gson;
import com.letianpai.robot.alarm.GeeUIAlarmManager;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.components.utils.TimeUtils;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.consts.AudioServiceConst;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.manager.SleepModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.storage.SPUtils;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.letianpai.robot.response.RobotFuncResponseManager;
import com.letianpai.robot.response.app.AppCmdResponser;
import com.letianpai.robot.taskservice.R;
import com.letianpai.robot.taskservice.audio.parser.AudioCommand;
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback;
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback;
import com.letianpai.robot.taskservice.entity.EnterAISpeechEntity;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.commandlib.consts.MCUCommandConsts;
import com.renhejia.robot.commandlib.consts.PackageConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.consts.SpeechConst;
import com.renhejia.robot.commandlib.parser.motion.Motion;
import com.renhejia.robot.commandlib.parser.power.PowerMotion;
import com.renhejia.robot.commandlib.parser.wakeup.WakeUp;
import com.renhejia.robot.commandlib.parser.word.Word;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.gesturefactory.parser.GestureData;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import java.util.ArrayList;
import java.util.List;


/**
 * @author liujunbin
 */
public class SpeechCmdResponser {
    private Gson mGson;
    private static String TAG = "SpeechCmdResponser";
    private static SpeechCmdResponser instance;
    private Context mContext;
    private String currentBgPackageName = null;
    private String previousCmd = null;
    private String speechCurrentStatus;

    public String getSpeechCurrentStatus() {
        return speechCurrentStatus;
    }

    public void setSpeechCurrentStatus(String speechCurrentStatus) {
        this.speechCurrentStatus = speechCurrentStatus;
    }

    private SpeechCmdResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static SpeechCmdResponser getInstance(Context context) {
        synchronized (SpeechCmdResponser.class) {
            if (instance == null) {
                instance = new SpeechCmdResponser(context.getApplicationContext());
            }
            return instance;
        }
    }

    private void init() {
        mGson = new Gson();
//        handler = new UpdateViewHandler(mContext);
        addGestureCompleteListener();
    }

    private void addGestureCompleteListener() {
        GestureCallback.getInstance().setGestureCompleteListener(new GestureCallback.GestureCompleteListener() {
            @Override
            public void onGestureCompleted(String gesture, int geTaskId) {
                if (geTaskId == RGestureConsts.GESTURE_COMMAND_SPEECH_MOVE
                        || geTaskId == RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY) {
                    makeCurrentApp(null, null);
                }
            }
        });
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        GeeUILogUtils.logi(TAG,"---commandDistribute:command-- " + command + "  data:" + data + "--previousCmd::"+previousCmd);
        if (TextUtils.isEmpty(command)) {
            return;
        }
        //ai游戏
        if (command.equals("rhj.controller.ai.enter")){
            //解析data；
            EnterAISpeechEntity enterAISpeechEntity = mGson.fromJson(data, EnterAISpeechEntity.class);
            enterAIReg(enterAISpeechEntity);
        }else if(command.equals("rhj.controller.ai.exit")){
            //退出AI游戏，回到上一个模式
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
        }

        if (!command.equals(SpeechConst.COMMAND_WAKE_UP_STATUS)) {
            this.previousCmd = command;
        }

        switch (command) {
            case SpeechConst.COMMAND_WAKE_UP_STATUS:
                responseWakeup(iLetianpaiService, command, data);
                break;

            case SpeechConst.COMMAND_WAKE_UP_DOA:
                responseDOA(iLetianpaiService, command, data);
                break;

            case SpeechConst.COMMAND_ENTER_CHAT_GPT:
                enterChatGpt(iLetianpaiService, command, data);
                break;

//            case SpeechConst.COMMAND_SAD:
//                motionSad(iLetianpaiService, command, data);
//                break;
//
//            case SpeechConst.COMMAND_HAPPY:
//                motionHappy(iLetianpaiService, command, data);
//                break;

//            case SpeechConst.COMMAND_ADD_CLOCK:
//                addClock(iLetianpaiService, command, data);
////                if (!LetianpaiFunctionUtil.isAlarmServiceRunning(mContext)) {
////                    Log.e("letianpai_alarm_taskservice", "============= create clock2 ============");
////                    LetianpaiFunctionUtil.startAlarmService(mContext, data);
////                    Log.e("letianpai_alarm_taskservice", "============= create clock3 ============");
////                }
//                break;
//            case SpeechConst.COMMAND_REMOVE_CLOCK:
//                cancelClock(iLetianpaiService, command, data);
//                break;
//
//            case SpeechConst.COMMAND_ADD_REMINDER:
//                addReminder(iLetianpaiService, command, data);
//                break;
//            case SpeechConst.COMMAND_ADD_NOTICE:
//                Log.e("letianpai_timer", "SpeechConst.COMMAND_ADD_NOTICE");
//                addNotice(iLetianpaiService, command, data);
//                break;
            case SpeechConst.COMMAND_ADD_CLOCK:
            case SpeechConst.COMMAND_REMOVE_CLOCK:
            case SpeechConst.COMMAND_ADD_REMINDER:
            case SpeechConst.COMMAND_ADD_NOTICE:
                GeeUILogUtils.logi("letianpai_timer","----SpeechConst.COMMAND_ADD_CLOCK_command: "+ command);
                GeeUILogUtils.logi("letianpai_timer", "----SpeechConst.COMMAND_ADD_CLOCK_data: "+ data);
                if (!LetianpaiFunctionUtil.isAlarmServiceRunning(mContext)) {
                    GeeUILogUtils.logi("letianpai_alarm_taskservice", "============= create clock2 ============");
                    LetianpaiFunctionUtil.startAlarmService(mContext,command, data);
                }
                break;

            case SpeechConst.COMMAND_TURN:
                responseTurn(data, iLetianpaiService);
                break;

            // case SpeechConst.COMMAND_BIRTHDAY:
            //     makeCurrentApp("sibichi", "222");
            //     happyBirthday();
            //     break;

            case SpeechConst.ShutDown:
                shutDown();
                break;
            case SpeechConst.Reboot:
                rebootRobot();
                break;

            case SpeechConst.COMMAND_HAND_ENTER:
                // enterHandReg();
                break;

            case SpeechConst.COMMAND_HAND_EXIT:
                // extHandReg();
                break;

            case SpeechConst.COMMAND_FINGER_GUEESS_ENTER:
                enterFingerGuess();
                break;

            case SpeechConst.COMMAND_FINGER_GUEESS_EXIT:
                // exitFingerGuess();
                break;


            case SpeechConst.COMMAND_TAKE_PHOTO:
                LetianpaiFunctionUtil.takePhoto(mContext);
                break;

            case SpeechConst.COMMAND_OPEN_FOLLOW_ME:
                break;

            case SpeechConst.COMMAND_OPEN_APP:
                GeeUILogUtils.logi("letianpai_sleep_test", "---- =================================== openRobotMode ====  000 --- SpeechConst.COMMAND_OPEN_APP: =================================== ");
                openApp(iLetianpaiService, data);
                break;

            case SpeechConst.COMMAND_CLOSE_APP:
                closeApp(iLetianpaiService, data);
                break;

            case SpeechConst.COMMAND_BODY_ENTER:
                RobotFuncResponseManager.getInstance(mContext).openPeopleSearch(OPEN_TYPE_SPEECH);
                break;

            case SpeechConst.COMMAND_BODY_EXIT:
                exitPeopleSearch();
                break;

            case SpeechConst.COMMAND_SEARCH_PEOPLE:
                // 人脸识别
                if (data.equals("1")) {
                    openSearchFace();
                } else if (data.equals("0")) {
                    exitSearchFace();
                }
                break;
            case SpeechConst.SetVolume:
                if (data.equals(SpeechConst.VOLUME_UP)) {
                    volumeUp(iLetianpaiService);
                } else if (data.equals(SpeechConst.VOLUME_DOWN)) {
                    volumeDown(iLetianpaiService);
                } else if (data.equals(SpeechConst.VOLUME_MAX)) {
                    volumeMax(iLetianpaiService);
                } else if (data.equals(SpeechConst.VOLUME_MIN)) {
                    volumeMin(iLetianpaiService);
                } else if (data.contains(SpeechConst.VOLUME_PERCENTAGE) || data.matches("-?\\d+(\\.\\d+)?")) {
                    volumeChange(iLetianpaiService, data);
                }
                break;
            default:
        }
    }

//     private void addNotice(ILetianpaiService iLetianpaiService, String command, String data) {
//         XLog.i("letianpai_speech"+ "----letianpai_speech_command: " + command + "--data::"+data);
//         RobotFuncResponseManager.stopRobot(iLetianpaiService);
//         String[] noticeInfo = data.split("-");
//         if (noticeInfo == null || noticeInfo.length < 2) {
//             return;
//         }
//         if (TextUtils.isEmpty(noticeInfo[0]) || TextUtils.isEmpty(noticeInfo[1])) {
//             return;
//         }
//         String time = TimeUtil.getNoticeTime((Long.parseLong(noticeInfo[0])) * 1000);
//         String dayTime = TimeUtil.getNoticeDayTime((Long.parseLong(noticeInfo[0])) * 1000);
//         String timekey = TimeUtil.getNoticeKeyTime((Long.parseLong(noticeInfo[0])) * 1000);
//
//         int year = Integer.parseInt(dayTime.substring(0, 4));
//         int month = Integer.parseInt(dayTime.substring(4, 6));
//         int day = Integer.parseInt(dayTime.substring(6, 8));
//
//         RobotConfigManager.getInstance(mContext).setNoticeTitle(timekey, noticeInfo[1]);
//         RobotConfigManager.getInstance(mContext).commit();
// //
// //        Log.e("letianpai_notice", " RobotConfigManager.getInstance(mContext).getNoticeTitle(timekey): " + RobotConfigManager.getInstance(mContext).getNoticeTitle(timekey));
// //
// //        Log.e("letianpai_notice", "time: " + time);
// //        Log.e("letianpai_notice", "current: " + System.currentTimeMillis());
//         String title = null;
//         if (noticeInfo[1] != null) {
//             title = noticeInfo[1];
//         }
//         if (!TextUtils.isEmpty(time)) {
//             String times[] = time.split(":");
//             if (times != null && times.length == 2) {
// //                Log.e("letianpai_notice", "updateClockData ==== 3 ");
// //                Log.e("letianpai_notice", "updateClockData ==== 4 Integer.valueOf(times[0]): " + Integer.valueOf(times[0]));
// //                Log.e("letianpai_notice", "updateClockData ==== 5 Integer.valueOf(times[1]): " + Integer.valueOf(times[1]));
// //                Log.e("letianpai_notice", "updateClockData ==== 6 title: " + title);
//                 GeeUINoticeManager.getInstance(mContext).createNotice(year, month, day, Integer.valueOf(times[0]), Integer.valueOf(times[1]), title);
//             }
//         }
//
//     }

    private void playTTS(ILetianpaiService iLetianpaiService, String ttsString) {
        Word word = new Word();
        word.setWord(ttsString);
        try {
            iLetianpaiService.setLongConnectCommand(RobotRemoteConsts.COMMAND_TYPE_CONTROL_SEND_WORD, word.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void playTTSVolume(ILetianpaiService iLetianpaiService){
        //未安装
        if (SystemFunctionUtil.isChinese()) {
            playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
        } else {
            playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
        }
    }

    private void volumeChange(ILetianpaiService iLetianpaiService, String data) {
        String newData = data.replace(SpeechConst.VOLUME_PERCENTAGE, "");
        int intData = 0;
        try {
            intData = Integer.valueOf(newData);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (intData != 0) {
            SleepModeManager.getInstance(mContext).setRobotVolume((15 * intData) / 100);
            if (intData >= 100) {
//                playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_max));
                playTTSVolume(iLetianpaiService);
                // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
            } else if (intData >= 0 && intData < 100) {
//                playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_to) + "70%");
//                playTTS(iLetianpaiService,String.format(mContext.getResources().getString(R.string.volume_to), data));
                playTTSVolume(iLetianpaiService);
                // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
            }
//        }

    }

    private void volumeDown(ILetianpaiService iLetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeDown();
//        playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_down));
        playTTSVolume(iLetianpaiService);
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    private void volumeUp(ILetianpaiService iLetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeUp();
//        playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_up));
        playTTSVolume(iLetianpaiService);
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    private void volumeMax(ILetianpaiService iLetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeMax();
        playTTSVolume(iLetianpaiService);
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    private void volumeMin(ILetianpaiService iLetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeMin();
        playTTSVolume(iLetianpaiService);
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    public void openSearchFace() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_FACE_REG_MODE, 1);
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.IdentFaceService");
        intent.setComponent(cn);
        mContext.startService(intent);
    }

    public void openRemindSearchFace() {
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.IdentFaceService");
        intent.setComponent(cn);
        mContext.startService(intent);
    }

    public void exitSearchFace() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_FACE_REG_MODE, 0);
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.IdentFaceService");
        intent.setComponent(cn);
        mContext.stopService(intent);
    }

    private void exitPeopleSearch() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_BODY_REG_MODE, 0);
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.BodyService");
        intent.setComponent(cn);
        mContext.stopService(intent);
    }

    private void openApp(ILetianpaiService iLetianpaiService, String data) {
        if (data.equals(mContext.getString(R.string.cmd_commemoration)) || data.equals(mContext.getString(R.string.cmd_commemoration_en))) {
            RobotFuncResponseManager.getInstance(mContext).openCommemoration(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_people_reg))) {
            RobotFuncResponseManager.getInstance(mContext).openPeopleSearch(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_robot)) || data.equals(mContext.getString(R.string.cmd_robot_en))) {
            RobotFuncResponseManager.getInstance(mContext).openRobotMode(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_weather)) || data.equals(mContext.getString(R.string.cmd_weather_en))) {
            RobotFuncResponseManager.getInstance(mContext).openWeather(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_sleep)) || data.equals(mContext.getString(R.string.cmd_sleep_en))) {
            RobotFuncResponseManager.getInstance(mContext).openSleepMode(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_countdown)) || data.equals(mContext.getString(R.string.cmd_countdown_en))) {
            RobotFuncResponseManager.getInstance(mContext).openEventCountdown(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_news)) || data.equals(mContext.getString(R.string.cmd_news_en))) {
            RobotFuncResponseManager.getInstance(mContext).openNews(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_message)) || data.equals(mContext.getString(R.string.cmd_message_en))) {
            RobotFuncResponseManager.getInstance(mContext).openMessage(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_stock)) || data.equals(mContext.getString(R.string.cmd_stock_en))) {
            RobotFuncResponseManager.getInstance(mContext).openStock(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_custom)) || data.equals(mContext.getString(R.string.cmd_custom_en))) {
            RobotFuncResponseManager.getInstance(mContext).openCustom(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_lamp)) || data.equals(mContext.getString(R.string.cmd_lamp_en))) {
            RobotFuncResponseManager.getInstance(mContext).openLamp(OPEN_TYPE_SPEECH);

//        } else if (data.equals(mContext.getString(R.string.cmd_tech))) {
//            LetianpaiFunctionUtil.openStock(mContext);

        } else if (data.equals(mContext.getString(R.string.cmd_switch_app))) {
            RobotFuncResponseManager.getInstance(mContext).openSwitchApp(OPEN_TYPE_SPEECH);

        }  else if (data.equals(mContext.getString(R.string.cmd_words))) {
            RobotFuncResponseManager.getInstance(mContext).openWord(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_time)) || data.equals(mContext.getString(R.string.cmd_time_en))) {
            RobotFuncResponseManager.getInstance(mContext).openTime(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_fans)) || data.equals(mContext.getString(R.string.cmd_fans_en))) {
            RobotFuncResponseManager.getInstance(mContext).openFans(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_pets))) {
            RobotFuncResponseManager.getInstance(mContext).openPetsMode(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_upgrade))) {
            RobotFuncResponseManager.getInstance(mContext).openUpgrade(OPEN_TYPE_SPEECH);

        } else if (data.equals(mContext.getString(R.string.cmd_screen))) {
            if (RobotModeManager.getInstance(mContext).getRobotMode() == VM_BLACK_SCREEN_SLEEP_MODE || RobotModeManager.getInstance(mContext).getRobotMode() == VM_BLACK_SCREEN_NIGHT_SLEEP_MODE) {
                RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 0);
            }
        } else if (data.equals(mContext.getString(R.string.auto_charging))) {
            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUTO_CHARGING, 1);
        } else if (data.equals(mContext.getString(R.string.bluetooth_box))) {
            startBluetoothDiscover();
        }else{
            GeeUILogUtils.logi("speech open others app.appName::data::"+data );
            LetianpaiFunctionUtil.openUniversalApp(mContext,data, iLetianpaiService);
            //走通用配置
            // UserAppsConfigModel userAppsConfigModel = AppCmdResponser.getInstance(mContext).getUserAppsConfigModel();
            // if (userAppsConfigModel == null){
            //     AppCmdResponser.getInstance(mContext).getUserAppsConfig();
            // }
            // Log.i("----", "speech open others app.appName::" +userAppsConfigModel.getData().size());
            // if (userAppsConfigModel!=null && !userAppsConfigModel.getData().isEmpty()){
            //     List<UserAppsConfig> list = userAppsConfigModel.getData().stream().filter(item -> item.appName.equals(data)).collect(Collectors.toList());
            //     Log.i("----", "data:" +data + "---list:"+list.size());
            //     if (!list.isEmpty()){
            //         UserAppsConfig userAppsConfig = list.stream().findFirst().get();
            //         if (SystemFunctionUtil.isAppInstalled(mContext, userAppsConfig.appPackageName)){
            //             if (userAppsConfig.appPackageName.equals("com.letianpai.robot.expression")){
            //                 RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_EXPRESSION);
            //             }else {
            //                 RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_OTHER);
            //             }
            //             LetianpaiFunctionUtil.openApp(mContext,userAppsConfig.appPackageName, userAppsConfig.openContent, userAppsConfig.appPackageName );
            //             return;
            //         }
            //         // RobotModeManager.getInstance(mContext).ttsUninstallAppText();
            //         Log.i("----", "speech open others app.appName::" + userAppsConfig.appName + "--appPackageName::"+userAppsConfig.appPackageName + "--openContent::"+userAppsConfig.openContent);
            //     }
            // }
        }
    }


    private void closeApp(ILetianpaiService iLetianpaiService, String data) {
        if (data.equals(mContext.getString(R.string.cmd_screen))) {
            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 1);
        }
        // RemoteCmdResponser.getInstance(mContext).closeUniversalApp(data, iLetianpaiService);
    }


    private void closeWeather(ILetianpaiService iLetianpaiService) {
//        COMMAND_TYPE_CLOSE_TARGET_APP
        closeTargetApp(iLetianpaiService, PackageConsts.WEATHER_PACKAGE_NAME);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeStock(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.STOCK_PACKAGE_NAME);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeMessage(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_MESSAGE);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeNews(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_NEWS);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeFans(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_FANS);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeCountdown(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_COUNT_DOWN);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeWords(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_WORDS);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void closeCommemoration(ILetianpaiService iLetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_COMMEMORATION);
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode();
    }

    private void rebootRobot() {
        SystemFunctionUtil.reboot(mContext);
    }

    // private void exitFingerGuess() {
    //     enterHandReg();
    // }

    private void enterFingerGuess() {
        GeeUILogUtils.logi("letianpai_hand", "enterFingerGuess");
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_HAND_REG_MODE, 1);
        Intent intent = new Intent();
        intent.putExtra("type", "finger");
        ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.HandService");
        intent.setComponent(cn);
        mContext.startService(intent);
    }

    /**
     * 退出手势识别
     */
    // private void extHandReg() {
    //     Intent intent = new Intent();
    //     ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.HandService");
    //     intent.setComponent(cn);
    //     mContext.stopService(intent);
    // }

    /**
     * 进入手势识别
     */
    // private void enterHandReg() {
    //     Log.e("letianpai_hand", "enterHandReg");
    //     RobotModeManager.getInstance(mContext).switchRobotMode(VM_HAND_REG_MODE, 1);
    //     Intent intent = new Intent();
    //     intent.putExtra("type", "hand");
    //     ComponentName cn = new ComponentName("com.ltp.ident", "com.ltp.ident.services.HandService");
    //     intent.setComponent(cn);
    //     mContext.startService(intent);
    // }

    /**
     * 进入AI相关程序
     */
    private void enterAIReg(EnterAISpeechEntity enterAISpeechEntity) {
        if (enterAISpeechEntity!=null){
            GeeUILogUtils.logi(TAG, "enterAISpeechEntity--"+enterAISpeechEntity.getPackageName());

            //判断是否安装
            if (SystemFunctionUtil.isAppInstalled(mContext, enterAISpeechEntity.getPackageName())){
                GeeUILogUtils.logi(TAG, "enterAIProgram");
                if (enterAISpeechEntity.getIntentType()!=null && enterAISpeechEntity.getIntentType().equals("activity")){
                    RobotModeManager.getInstance(mContext).switchRobotMode(VM_HAND_REG_MODE, 2);
                    Intent intent = new Intent();
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("type", enterAISpeechEntity.getType());
                    ComponentName cn = new ComponentName(enterAISpeechEntity.getPackageName(), enterAISpeechEntity.getClazz());
                    intent.setComponent(cn);
                    mContext.startActivity(intent);
                }else{
                    RobotModeManager.getInstance(mContext).switchRobotMode(VM_HAND_REG_MODE, 1);
                    Intent intent = new Intent();
                    intent.putExtra("type", enterAISpeechEntity.getType());
                    ComponentName cn = new ComponentName(enterAISpeechEntity.getPackageName(), enterAISpeechEntity.getClazz());
                    intent.setComponent(cn);
                    mContext.startService(intent);
                }
            }else{
                RobotModeManager.getInstance(mContext).ttsUninstallAppText();
            }
        }
    }

    private void makeCurrentApp(String packageName, String data) {
        currentBgPackageName = packageName;
    }

    private void shutDown() {
        ExpressionChangeCallback.getInstance().showShutDown();
        SystemFunctionUtil.shutdownRobot(mContext);
    }

//     private void happyBirthday() {
//         //TODO 添加happyBirthday
//         Log.e("SpeechCmdResponser_1", "happyBirthday ======= 1 ： ");
// //        GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_BIRTHDAY,RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY);
//         Log.e("letianpai_birthday", " ============= 1 ==============");
//         GestureCallback.getInstance().setGestures(GestureCenter.birthdayGestureData(), RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY);
// //        CommandResponseManager.getInstance(instance.mContext).responseGestures(GestureConsts.GESTURE_BIRTHDAY, iLetianpaiService);
//     }

//    private void chatGptSpeaking(ILetianpaiService iLetianpaiService, String command, String data) {
//        GestureCallback.getInstance().setGestures(GestureCenter.getSpeakingAiGesture(), RGestureConsts.GESTURE_GPT_LISTENING);
//    }
//
//    private void chatGptListening(ILetianpaiService iLetianpaiService, String command, String data) {
//        GestureCallback.getInstance().setGestures(GestureCenter.getWakeupAiGesture(), RGestureConsts.GESTURE_GPT_LISTENING);
//    }

    private void motionHappy(ILetianpaiService iLetianpaiService, String command, String data) {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_EMOTION, 1);
//        GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_MOTION_HAPPY, RGestureConsts.GESTURE_ID_HAPPY);
        GestureCallback.getInstance().setGestures(GestureCenter.getHappyGesture(), RGestureConsts.GESTURE_ID_HAPPY);
    }

    private void motionSad(ILetianpaiService iLetianpaiService, String command, String data) {
        RobotModeManager.getInstance(mContext).switchRobotMode(VM_EMOTION, 1);
//        GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_MOTION_SAD, RGestureConsts.GESTURE_ID_SAD);
        GestureCallback.getInstance().setGestures(GestureCenter.getSadGesture(), RGestureConsts.GESTURE_ID_SAD);
    }

    private void responseTurn(String data, ILetianpaiService iLetianpaiService) {
//        AudioCommand audioCommand = mGson.fromJson(data, AudioCommand.class);
//        responseMove(audioCommand,iLetianpaiService);
    }

    private void responseMove(String data, ILetianpaiService iLetianpaiService) {
        Log.e("SpeechCmdResponser_1", "responseMove： " + data);
        AudioCommand audioCommand = mGson.fromJson(data, AudioCommand.class);
        responseMove(audioCommand, iLetianpaiService);

    }

    private void closeTargetApp(ILetianpaiService iLetianpaiService, String packageName) {
        try {
            iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_CLOSE_TARGET_APP, packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 响应唤醒
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void responseWakeup(ILetianpaiService iLetianpaiService, String command, String data) {
        updateWakeupState(iLetianpaiService, data);
    }

    /**
     * 响应角度
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void responseDOA(ILetianpaiService iLetianpaiService, String command, String data) {

    }

    /**
     * 进入chatgpt
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void enterChatGpt(ILetianpaiService iLetianpaiService, String command, String data) {
        GestureCallback.getInstance().setGestures(GestureCenter.getWakeupGesture(), RGestureConsts.GESTURE_WAKE_UP);

    }

    /**
     * 退出chatgpt
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void quiteChatGpt(ILetianpaiService iLetianpaiService, String command, String data) {

    }

    /**
     * 添加闹钟
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void addClock(ILetianpaiService iLetianpaiService, String command, String data) {
        Log.e("letianpai_speech", "command: " + command);
        Log.e("letianpai_speech", "data: " + data);
        RobotFuncResponseManager.stopRobot(iLetianpaiService);
        String[] time = data.split("-");
        if (time == null && time.length < 2) {
            return;
        }
        String date = time[1];
        int year = Integer.valueOf(date.substring(0, 4));
        int month = Integer.valueOf(date.substring(4, 6));
        int day = Integer.valueOf(date.substring(6, 8));

        Log.e("letianpai_speech", "year: " + year);
        Log.e("letianpai_speech", "month: " + month);
        Log.e("letianpai_speech", "day: " + day);

        String[] times = data.split(":");
        if (times == null && times.length < 2) {
            return;
        }
        //TODO 获取当前时间，进行比对

        Log.e("letianpai_1234", "updateClockData ==== 3 ");
        Log.e("letianpai_1234", "updateClockData ==== 4 Integer.valueOf(times[0]): " + Integer.valueOf(times[0]));
        Log.e("letianpai_1234", "updateClockData ==== 5 Integer.valueOf(times[1]): " + Integer.valueOf(times[1]));
//        GeeUIAlarmManager.getInstance(mContext).createAlarm(Integer.valueOf(times[0]), Integer.valueOf(times[1]));

        int hour = Integer.valueOf(times[0]);
        int minute = Integer.valueOf(times[1]);

        boolean isNeedToNextDay = isNeedToChangeToNextDay(year,month,day,hour,minute);
        if (isNeedToNextDay){
            GeeUIAlarmManager.getInstance(mContext).createAlarmNew(year, month, day+ 1, hour, minute);
        }else{
            GeeUIAlarmManager.getInstance(mContext).createAlarmNew(year, month, day, hour, minute);
        }
    }

    private boolean isNeedToChangeToNextDay(int year, int month, int day, int hour, int minute) {
        long currentTime = System.currentTimeMillis();
        int currentHour = TimeUtils.get24HourTime(currentTime);
        int currentMinute = TimeUtils.getMinTime(currentTime);
        int currentDay = TimeUtils.getDayTime(currentTime);
        int currentMon = TimeUtils.getMonTime(currentTime);
        int currentYear = TimeUtils.getYearTime(currentTime);

        Log.e("letianpai_1234", "currentYear: " + currentYear);
        Log.e("letianpai_1234", "year: " + year);
        Log.e("letianpai_1234", "currentMon: " + currentMon);
        Log.e("letianpai_1234", "month: " + month);
        Log.e("letianpai_1234", "currentDay: " + currentDay);
        Log.e("letianpai_1234", "day: " + day);
        Log.e("letianpai_1234", "currentHour: " + currentHour);
        Log.e("letianpai_1234", "hour: " + hour);
        Log.e("letianpai_1234", "currentMinute: " + currentMinute);
        Log.e("letianpai_1234", "minute: " + minute);

        if (day > currentDay){
            return false;
        }else if (day == currentDay && currentHour >hour){
            return true;
        }else if (day == currentDay && currentHour == hour && currentMinute > minute){
            return true;
        }

        return false;
    }

    /**
     * 取消闹钟
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void cancelClock(ILetianpaiService iLetianpaiService, String command, String data) {
        RobotFuncResponseManager.stopRobot(iLetianpaiService);
        String[] times = data.split(":");
        if (times == null && times.length < 2) {
            return;
        }
        GeeUIAlarmManager.getInstance(mContext).cancelAlarm(Integer.valueOf(times[0]), Integer.valueOf(times[1]));
    }

    /**
     * 添加提醒
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private void addReminder(ILetianpaiService iLetianpaiService, String command, String data) {
        Log.e("letianpai_speech", "command1: " + command);
        Log.e("letianpai_speech", "data1: " + data);
        makeCurrentApp("com.letianpai.robot.alarm", data);
        LetianpaiFunctionUtil.openCountdown(mContext, data);
        RobotFuncResponseManager.stopRobot(iLetianpaiService);
    }


    /**
     * 更新唤醒词和音色设置
     *
     * @param data {"xiaole_switch":0,"xiaopai_switch":1,"selected_voice_id":"yyqiaf","selected_voice_name":"臻品女声悦悦",
     *             "boy_voice_switch":0,"girl_voice_switch":1,"boy_child_voice_switch":0,"girl_child_voice_switch":0,"robot_voice_switch":0}
     */
    private void updateWakeup(String data) {
        WakeUp wakeUp = mGson.fromJson(data, WakeUp.class);
        GeeUILogUtils.logd("RemoteCmdResponser", "updateWakeup: " + wakeUp);
        //TODO 此部分逻辑需要在思必驰内响应，暂时注释掉此部分逻辑
//        RhjAudioManager.getInstance().setSpeaker(mContext, wakeUp.getSelectedVoiceId());
//        RhjAudioManager.getInstance().setWakeupWord(mContext, wakeUp.getXiaoleSwitch() == 1, wakeUp.getXiaopaiSwitch() == 1);
    }

    private void updateWakeupState(ILetianpaiService iLetianpaiService, String data) {
        speechCurrentStatus = data;
        GeeUILogUtils.logd("LTPAudioService", "updateWakeupState: " + data + "---previousCmd::"+previousCmd + "---currentBgPackageName::"+currentBgPackageName);
        switch (data) {
            case AudioServiceConst.ROBOT_STATUS_SILENCE:
                //语音结束的时候，判断是否是自动切换app
                float isAutoShow = SPUtils.getInstance(mContext).getInt("isAutoShow");
                AppCmdResponser.getInstance(mContext).setAutoSwitchApp(isAutoShow == 1);//1,自动切换，0,手动切换

              if (previousCmd != null && (previousCmd.equals(SpeechConst.COMMAND_OPEN_APP)
                      || previousCmd.equals(SpeechConst.COMMAND_CLOSE_APP)
                      || previousCmd.equals(SpeechConst.COMMAND_ADD_REMINDER))) {
                    previousCmd = null;
                    // return;
                }
                if (TextUtils.isEmpty(currentBgPackageName)) {
                    Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 5  LetianpaiFunctionUtil.isVideoCallRunning(mContext): " + LetianpaiFunctionUtil.isVideoCallRunning(mContext));
                    Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 5  LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext): " + LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext));
                    Log.e("letianpai_timer", "RobotMode:-------- " + RobotModeManager.getInstance(mContext).getRobotMode());
                    int robotMode = RobotModeManager.getInstance(mContext).getRobotMode();
                    if (robotMode == VM_EMOTION ||
                            robotMode == VM_TAKE_PHOTO ||
                            robotMode == VM_DEMOSTRATE_MODE ||
                            robotMode == VM_AUTO_CHARGING ||
                            LetianpaiFunctionUtil.isAlarmOnTheTop(mContext) ||
                            LetianpaiFunctionUtil.isNewAlarmOnTheTop(mContext) ||
                            LetianpaiFunctionUtil.isVideoCallRunning(mContext) ||
                            LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext) ||
                            RobotModeManager.getInstance(mContext).getRobotTrtcStatus()!=-1 ) {
                        //关闭思必驰
                        RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService);
                        return;
                    }
                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 0);
                }
                break;

            case AudioServiceConst.ROBOT_STATUS_LISTENING:
                AppCmdResponser.getInstance(mContext).setAutoSwitchApp(false);//1,自动切换，0,手动切换
                //机器人起来的时候，currentPackageName 值为空，不然切换会有问题 yujianbin
                LetianpaiFunctionUtil.currentPackageName = "";
                Log.e("---"+TAG, " ===== 001  ====== AudioServiceConst.ROBOT_STATUS_LISTENING");
                try {
                    iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS, PackageConsts.ROBOT_PACKAGE_NAME);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 1 currentBgPackageName: " + currentBgPackageName);
                makeCurrentApp(null, null);
                Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 1.1 currentBgPackageName: " + currentBgPackageName);
                if (LetianpaiFunctionUtil.isOtherRobotAppOnTheTop(mContext)) {
                    Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 1.2 LetianpaiFunctionUtil.isOtherRobotAppOnTheTop(mContext) " + LetianpaiFunctionUtil.isOtherRobotAppOnTheTop(mContext));
                    shutdownAudioService(iLetianpaiService);

//                    try {
//                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP, RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
                    RobotFuncResponseManager.closeApp(iLetianpaiService);
                    Log.e("letianpai_screen_test", "black_screen_========== 1 ==========: ");
                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT, 1);
                } else {
                    Log.e("letianpai_screen_test", "black_screen_========== 2 ==========: ");
                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 1);
                }
                break;

            case AudioServiceConst.ROBOT_STATUS_MUSIC:
                AppCmdResponser.getInstance(mContext).setAutoSwitchApp(false);//1,自动切换，0,手动切换
//                Log.i(TAG, "updateWakeupState: " + "理解中...");
                //showWakeupState("理解中");
//                GestureCallback.getInstance().setGestures(GestureCenter.wakeupUnderstandGesture(),0);
//                GestureCallback.getInstance().setGesture(GestureConsts.GESTURE_ASSISTANT);
                break;
            case AudioServiceConst.ROBOT_STATUS_SPEAKING:
                Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_SPEAKING ===== 2");

                if (TextUtils.isEmpty(currentBgPackageName)) {
                    RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 2);
                }
//                Log.i(TAG, "updateWakeupState: " + "播放语音中...");
//                GestureCallback.getInstance().setGestures(GestureCenter.wakeupSpeakGesture(),0);
                //showWakeupState("播放语音中");
                break;
            default: {
//                Log.i(TAG, "updateWakeupState: without identify state");
                //showWakeupState("updateWakeupState: without identify state");
            }
        }
    }

    private void shutdownAudioService(ILetianpaiService iLetianpaiService) {
        if (!LetianpaiFunctionUtil.isAlarmRunning(mContext)) {
            return;
        }
        try {
            iLetianpaiService.setRobotStatusCmd(AppCmdConsts.COMMAND_TYPE_SHUT_DOWN_AUDIO_SERVICE, AppCmdConsts.COMMAND_TYPE_SHUT_DOWN_AUDIO_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * @param commandData
     * @param iLetianpaiService
     */
    public static void responseMove(Object commandData, ILetianpaiService iLetianpaiService) {
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(3, 1).toString());
        //TODO 给MCU发消息，打开悬崖，悬空上爆棚
        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(5, 1).toString());
        if (commandData instanceof AudioCommand) {
            String direction = ((AudioCommand) commandData).getDirection();
            int directions = 0;
            String number = ((AudioCommand) commandData).getNumber();
            int numberInt = 0;
            int internal = 0;
            if (number != null) {
                numberInt = Integer.parseInt(number);
                Log.e("SpeechCmdResponser_1", "numberInt： " + numberInt);
            }
            if (number != null && numberInt > 0 && direction != null) {
                if (direction.equals("前")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_FORWARD;
                    internal = (numberInt - 1) * 300 * 6 + 8 * 300;
                    directions = 63;
                } else if (direction.equals("后")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND;
                    directions = 64;
                    internal = (numberInt - 1) * 300 * 6 + 8 * 300;
                } else if (direction.equals("左")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND;
                    directions = 5;
                    internal = numberInt * 300 * 4;
                } else if (direction.equals("右")) {
                    direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND;
                    directions = 6;
                    internal = numberInt * 300 * 4;
                }


//                try {
                Log.e("SpeechCmdResponser_1", "responseMove： ===== iLetianpaiService.setMcuCommand");
//                    iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, new Motion(direction, numberInt).toString());

                ArrayList<GestureData> list = new ArrayList<>();
                GestureData data = new GestureData();
                data.setFootAction(new Motion(null, directions, numberInt));
                data.setInterval(internal);
                list.add(data);
                GestureCallback.getInstance().setGestures(list, RGestureConsts.GESTURE_COMMAND_SPEECH_MOVE);
//                    iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, new Motion(null, directions,numberInt).toString());

//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }

    private void startBluetoothDiscover() {
        //启动修改蓝牙可见性的Intent
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置蓝牙可见性的时间，方法本身规定最多可见300秒
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        if (mContext != null) {
            if (ActivityCompat.checkSelfPermission(mContext, "android.permission.BLUETOOTH_ADVERTISE") != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mContext.startActivity(intent);
        }
    }

    private void startCountDownTimer(String data) {
//        Message message = new Message();
//        message.what = UPDATE_COUNT_DOWN_TIMER;
//        message.obj = data;
//        handler.sendMessage(message);
    }

    private void moveAppToTop(String packageName) {

        // 获取 ActivityManager
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        // 获取最近的任务列表
        List<ActivityManager.RecentTaskInfo> recentTasks = activityManager.getRecentTasks(20, ActivityManager.RECENT_WITH_EXCLUDED);

        // 找到需要切换到前台的应用程序的任务
        for (ActivityManager.RecentTaskInfo recentTask : recentTasks) {
            if (recentTask.baseIntent.getComponent().getPackageName().equals(packageName)) {
                // 找到了需要切换到前台的应用程序的任务
                int taskId = recentTask.persistentId;
                activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
                break;
            }
        }
    }

}
