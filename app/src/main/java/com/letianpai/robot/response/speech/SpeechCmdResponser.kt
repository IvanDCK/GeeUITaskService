package com.letianpai.robot.response.speech

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.letianpai.robot.alarm.GeeUIAlarmManager
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.components.utils.TimeUtils
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.consts.AudioServiceConst
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.manager.SleepModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.storage.SPUtils
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.response.RobotFuncResponseManager
import com.letianpai.robot.response.app.AppCmdResponser
import com.letianpai.robot.taskservice.R
import com.letianpai.robot.taskservice.audio.parser.AudioCommand
import com.letianpai.robot.taskservice.dispatch.command.CommandResponseCallback
import com.letianpai.robot.taskservice.dispatch.expression.ExpressionChangeCallback
import com.letianpai.robot.taskservice.entity.EnterAISpeechEntity
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.commandlib.consts.MCUCommandConsts
import com.renhejia.robot.commandlib.consts.PackageConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.consts.SpeechConst
import com.renhejia.robot.commandlib.parser.motion.Motion
import com.renhejia.robot.commandlib.parser.power.PowerMotion
import com.renhejia.robot.commandlib.parser.wakeup.WakeUp
import com.renhejia.robot.commandlib.parser.word.Word
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.gesturefactory.parser.GestureData
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class SpeechCmdResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null
    private var currentBgPackageName: String? = null
    private var previousCmd: String? = null
    @JvmField
    var speechCurrentStatus: String? = null
    private var gestureCompleteListener: GestureCallback.GestureCompleteListener? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
        //        handler = new UpdateViewHandler(mContext);
        addGestureCompleteListener()
    }

    private fun addGestureCompleteListener() {
        gestureCompleteListener = GestureCallback.GestureCompleteListener { gesture, geTaskId ->
            if (geTaskId == RGestureConsts.GESTURE_COMMAND_SPEECH_MOVE
                || geTaskId == RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY
            ) {
                makeCurrentApp(null, null)
            }
        }
        GestureCallback.instance.setGestureCompleteListener(gestureCompleteListener)
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        GeeUILogUtils.logi(
            TAG,
            "---commandDistribute:command-- $command  data:$data--previousCmd::$previousCmd"
        )
        if (TextUtils.isEmpty(command)) {
            return
        }
        //ai游戏
        if (command == "rhj.controller.ai.enter") {
            //解析data；
            val enterAISpeechEntity = mGson!!.fromJson(
                data,
                EnterAISpeechEntity::class.java
            )
            enterAIReg(enterAISpeechEntity)
        } else if (command == "rhj.controller.ai.exit") {
            //退出AI游戏，回到上一个模式
            RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
        }

        if (command != SpeechConst.COMMAND_WAKE_UP_STATUS) {
            this.previousCmd = command
        }

        when (command) {
            SpeechConst.COMMAND_WAKE_UP_STATUS -> responseWakeup(iLetianpaiService, command, data)
            SpeechConst.COMMAND_WAKE_UP_DOA -> responseDOA(iLetianpaiService, command, data)
            SpeechConst.COMMAND_ENTER_CHAT_GPT -> enterChatGpt(iLetianpaiService, command, data)
            SpeechConst.COMMAND_ADD_CLOCK, SpeechConst.COMMAND_REMOVE_CLOCK, SpeechConst.COMMAND_ADD_REMINDER, SpeechConst.COMMAND_ADD_NOTICE -> {
                GeeUILogUtils.logi(
                    "letianpai_timer",
                    "----SpeechConst.COMMAND_ADD_CLOCK_command: $command"
                )
                GeeUILogUtils.logi(
                    "letianpai_timer",
                    "----SpeechConst.COMMAND_ADD_CLOCK_data: $data"
                )
                if (!LetianpaiFunctionUtil.isAlarmServiceRunning(mContext)) {
                    GeeUILogUtils.logi(
                        "letianpai_alarm_taskservice",
                        "============= create clock2 ============"
                    )
                    LetianpaiFunctionUtil.startAlarmService(mContext, command, data)
                }
            }

            SpeechConst.COMMAND_TURN -> responseTurn(data, iLetianpaiService)
            SpeechConst.ShutDown -> shutDown()
            SpeechConst.Reboot -> rebootRobot()
            SpeechConst.COMMAND_HAND_ENTER -> {}
            SpeechConst.COMMAND_HAND_EXIT -> {}
            SpeechConst.COMMAND_FINGER_GUEESS_ENTER -> enterFingerGuess()
            SpeechConst.COMMAND_FINGER_GUEESS_EXIT -> {}
            SpeechConst.COMMAND_TAKE_PHOTO -> LetianpaiFunctionUtil.takePhoto(mContext)
            SpeechConst.COMMAND_OPEN_FOLLOW_ME -> {}
            SpeechConst.COMMAND_OPEN_APP -> {
                GeeUILogUtils.logi(
                    "letianpai_sleep_test",
                    "---- =================================== openRobotMode ====  000 --- SpeechConst.COMMAND_OPEN_APP: =================================== "
                )
                openApp(iLetianpaiService, data)
            }

            SpeechConst.COMMAND_CLOSE_APP -> closeApp(iLetianpaiService, data)
            SpeechConst.COMMAND_BODY_ENTER -> RobotFuncResponseManager.getInstance(
                mContext
            ).openPeopleSearch(RobotFuncResponseManager.OPEN_TYPE_SPEECH)

            SpeechConst.COMMAND_BODY_EXIT -> exitPeopleSearch()
            SpeechConst.COMMAND_SEARCH_PEOPLE ->                 // 人脸识别
                if (data == "1") {
                    openSearchFace()
                } else if (data == "0") {
                    exitSearchFace()
                }

            SpeechConst.SetVolume -> if (data == SpeechConst.VOLUME_UP) {
                volumeUp(iLetianpaiService)
            } else if (data == SpeechConst.VOLUME_DOWN) {
                volumeDown(iLetianpaiService)
            } else if (data == SpeechConst.VOLUME_MAX) {
                volumeMax(iLetianpaiService)
            } else if (data == SpeechConst.VOLUME_MIN) {
                volumeMin(iLetianpaiService)
            } else if (data.contains(SpeechConst.VOLUME_PERCENTAGE) || data.matches("-?\\d+(\\.\\d+)?".toRegex())) {
                volumeChange(iLetianpaiService, data)
            }

            else -> {}
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
    private fun playTTS(iLetianpaiService: ILetianpaiService, ttsString: String) {
        val word = Word()
        word.word = ttsString
        try {
            iLetianpaiService.setLongConnectCommand(
                RobotRemoteConsts.COMMAND_TYPE_CONTROL_SEND_WORD,
                word.toString()
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun playTTSVolume(iLetianpaiService: ILetianpaiService) {
        //未安装
        if (SystemFunctionUtil.isChinese) {
            playTTS(iLetianpaiService, mContext.resources.getString(R.string.volume_changed))
        } else {
            playTTS(iLetianpaiService, mContext.resources.getString(R.string.volume_changed))
        }
    }

    private fun volumeChange(iLetianpaiService: ILetianpaiService, data: String) {
        val newData = data.replace(SpeechConst.VOLUME_PERCENTAGE, "")
        var intData = 0
        try {
            intData = newData.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //        if (intData != 0) {
        SleepModeManager.getInstance(mContext).setRobotVolume((15 * intData) / 100)
        if (intData >= 100) {
//                playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_max));
            playTTSVolume(iLetianpaiService)
            // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
        } else if (intData in 0..99) {
//                playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_to) + "70%");
//                playTTS(iLetianpaiService,String.format(mContext.getResources().getString(R.string.volume_to), data));
            playTTSVolume(iLetianpaiService)
            // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
        }

        //        }
    }

    private fun volumeDown(iLetianpaiService: ILetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeDown()
        //        playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_down));
        playTTSVolume(iLetianpaiService)
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    private fun volumeUp(iLetianpaiService: ILetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeUp()
        //        playTTS(iLetianpaiService,mContext.getResources().getString(R.string.volume_up));
        playTTSVolume(iLetianpaiService)
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    private fun volumeMax(iLetianpaiService: ILetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeMax()
        playTTSVolume(iLetianpaiService)
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    private fun volumeMin(iLetianpaiService: ILetianpaiService) {
        SleepModeManager.getInstance(mContext).volumeMin()
        playTTSVolume(iLetianpaiService)
        // playTTS(iLetianpaiService, mContext.getResources().getString(R.string.volume_changed));
    }

    fun openSearchFace() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_FACE_REG_MODE, 1)
        val intent = Intent()
        val cn = ComponentName("com.ltp.ident", "com.ltp.ident.services.IdentFaceService")
        intent.setComponent(cn)
        mContext.startService(intent)
    }

    fun openRemindSearchFace() {
        val intent = Intent()
        val cn = ComponentName("com.ltp.ident", "com.ltp.ident.services.IdentFaceService")
        intent.setComponent(cn)
        mContext.startService(intent)
    }

    fun exitSearchFace() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_FACE_REG_MODE, 0)
        val intent = Intent()
        val cn = ComponentName("com.ltp.ident", "com.ltp.ident.services.IdentFaceService")
        intent.setComponent(cn)
        mContext.stopService(intent)
    }

    private fun exitPeopleSearch() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_BODY_REG_MODE, 0)
        val intent = Intent()
        val cn = ComponentName("com.ltp.ident", "com.ltp.ident.services.BodyService")
        intent.setComponent(cn)
        mContext.stopService(intent)
    }

    private fun openApp(iLetianpaiService: ILetianpaiService, data: String) {
        if (data == mContext.getString(R.string.cmd_commemoration) || data == mContext.getString(R.string.cmd_commemoration_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openCommemoration(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_people_reg)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openPeopleSearch(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_robot) || data == mContext.getString(R.string.cmd_robot_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openRobotMode(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_weather) || data == mContext.getString(R.string.cmd_weather_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openWeather(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_sleep) || data == mContext.getString(R.string.cmd_sleep_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openSleepMode(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_countdown) || data == mContext.getString(
                R.string.cmd_countdown_en
            )
        ) {
            RobotFuncResponseManager.getInstance(mContext)
                .openEventCountdown(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_news) || data == mContext.getString(R.string.cmd_news_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openNews(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_message) || data == mContext.getString(R.string.cmd_message_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openMessage(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_stock) || data == mContext.getString(R.string.cmd_stock_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openStock(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_custom) || data == mContext.getString(R.string.cmd_custom_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openCustom(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_lamp) || data == mContext.getString(R.string.cmd_lamp_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openLamp(RobotFuncResponseManager.OPEN_TYPE_SPEECH)

            //        } else if (data.equals(mContext.getString(R.string.cmd_tech))) {
//            LetianpaiFunctionUtil.openStock(mContext);
        } else if (data == mContext.getString(R.string.cmd_switch_app)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openSwitchApp(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_words)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openWord(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_time) || data == mContext.getString(R.string.cmd_time_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openTime(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_fans) || data == mContext.getString(R.string.cmd_fans_en)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openFans(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_pets)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openPetsMode(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_upgrade)) {
            RobotFuncResponseManager.getInstance(mContext)
                .openUpgrade(RobotFuncResponseManager.OPEN_TYPE_SPEECH)
        } else if (data == mContext.getString(R.string.cmd_screen)) {
            if (RobotModeManager.getInstance(mContext).robotMode == ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE || RobotModeManager.getInstance(
                    mContext
                ).robotMode == ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE
            ) {
                RobotModeManager.getInstance(mContext)
                    .switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 0)
            }
        } else if (data == mContext.getString(R.string.auto_charging)) {
            RobotModeManager.getInstance(mContext)
                .switchRobotMode(ViewModeConsts.VM_AUTO_CHARGING, 1)
        } else if (data == mContext.getString(R.string.bluetooth_box)) {
            startBluetoothDiscover()
        } else {
            GeeUILogUtils.logi("speech open others app.appName::data::$data")
            LetianpaiFunctionUtil.openUniversalApp(mContext, data, iLetianpaiService)
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


    private fun closeApp(iLetianpaiService: ILetianpaiService, data: String) {
        if (data == mContext.getString(R.string.cmd_screen)) {
            RobotModeManager.getInstance(mContext)
                .switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 1)
        }
        // RemoteCmdResponser.getInstance(mContext).closeUniversalApp(data, iLetianpaiService);
    }


    private fun closeWeather(iLetianpaiService: ILetianpaiService) {
//        COMMAND_TYPE_CLOSE_TARGET_APP
        closeTargetApp(iLetianpaiService, PackageConsts.WEATHER_PACKAGE_NAME)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeStock(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.STOCK_PACKAGE_NAME)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeMessage(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_MESSAGE)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeNews(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_NEWS)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeFans(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_FANS)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeCountdown(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_COUNT_DOWN)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeWords(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_WORDS)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun closeCommemoration(iLetianpaiService: ILetianpaiService) {
        closeTargetApp(iLetianpaiService, PackageConsts.PACKAGE_NAME_COMMEMORATION)
        RobotModeManager.getInstance(mContext).switchToPreviousPlayMode()
    }

    private fun rebootRobot() {
        SystemFunctionUtil.reboot(mContext)
    }

    // private void exitFingerGuess() {
    //     enterHandReg();
    // }
    private fun enterFingerGuess() {
        GeeUILogUtils.logi("letianpai_hand", "enterFingerGuess")
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_HAND_REG_MODE, 1)
        val intent = Intent()
        intent.putExtra("type", "finger")
        val cn = ComponentName("com.ltp.ident", "com.ltp.ident.services.HandService")
        intent.setComponent(cn)
        mContext.startService(intent)
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
     * Access to AI-related programmes
     */
    private fun enterAIReg(enterAISpeechEntity: EnterAISpeechEntity?) {
        if (enterAISpeechEntity != null) {
            GeeUILogUtils.logi(TAG, "enterAISpeechEntity--" + enterAISpeechEntity.packageName)

            //Determine whether to install
            if (SystemFunctionUtil.isAppInstalled(mContext, enterAISpeechEntity.packageName!!)) {
                GeeUILogUtils.logi(TAG, "enterAIProgram")
                if (enterAISpeechEntity.intentType != null && enterAISpeechEntity.intentType == "activity") {
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(ViewModeConsts.VM_HAND_REG_MODE, 2)
                    val intent = Intent()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent.putExtra("type", enterAISpeechEntity.type)
                    val cn = ComponentName(
                        enterAISpeechEntity.packageName!!,
                        enterAISpeechEntity.clazz!!
                    )
                    intent.setComponent(cn)
                    mContext.startActivity(intent)
                } else {
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(ViewModeConsts.VM_HAND_REG_MODE, 1)
                    val intent = Intent()
                    intent.putExtra("type", enterAISpeechEntity.type)
                    val cn = ComponentName(
                        enterAISpeechEntity.packageName!!,
                        enterAISpeechEntity.clazz!!
                    )
                    intent.setComponent(cn)
                    mContext.startService(intent)
                }
            } else {
                RobotModeManager.getInstance(mContext).ttsUninstallAppText()
            }
        }
    }

    private fun makeCurrentApp(packageName: String?, data: String?) {
        currentBgPackageName = packageName
    }

    private fun shutDown() {
        ExpressionChangeCallback.instance.showShutDown()
        SystemFunctionUtil.shutdownRobot(mContext)
    }

    //     private void happyBirthday() {
    //         //TODO Add happyBirthday
    //         Log.e("SpeechCmdResponser_1", "happyBirthday ======= 1 ： ");
    // //        GestureCallback.instance.setGesture(GestureConsts.GESTURE_BIRTHDAY,RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY);
    //         Log.e("letianpai_birthday", " ============= 1 ==============");
    //         GestureCallback.instance.setGestures(GestureCenter.birthdayGestureData(), RGestureConsts.GESTURE_COMMAND_SPEECH_BIRTHDAY);
    // //        CommandResponseManager.getInstance(instance.mContext).responseGestures(GestureConsts.GESTURE_BIRTHDAY, iLetianpaiService);
    //     }
    //    private void chatGptSpeaking(ILetianpaiService iLetianpaiService, String command, String data) {
    //        GestureCallback.instance.setGestures(GestureCenter.getSpeakingAiGesture(), RGestureConsts.GESTURE_GPT_LISTENING);
    //    }
    //
    //    private void chatGptListening(ILetianpaiService iLetianpaiService, String command, String data) {
    //        GestureCallback.instance.setGestures(GestureCenter.getWakeupAiGesture(), RGestureConsts.GESTURE_GPT_LISTENING);
    //    }
    private fun motionHappy(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_EMOTION, 1)
        //        GestureCallback.instance.setGesture(GestureConsts.GESTURE_MOTION_HAPPY, RGestureConsts.GESTURE_ID_HAPPY);
        GestureCallback.instance
            .setGestures(GestureCenter.happyGesture, RGestureConsts.GESTURE_ID_HAPPY)
    }

    private fun motionSad(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_EMOTION, 1)
        //        GestureCallback.instance.setGesture(GestureConsts.GESTURE_MOTION_SAD, RGestureConsts.GESTURE_ID_SAD);
        GestureCallback.instance
            .setGestures(GestureCenter.sadGesture, RGestureConsts.GESTURE_ID_SAD)
    }

    private fun responseTurn(data: String, iLetianpaiService: ILetianpaiService) {
//        AudioCommand audioCommand = mGson.fromJson(data, AudioCommand.class);
//        responseMove(audioCommand,iLetianpaiService);
    }

    private fun responseMove(data: String, iLetianpaiService: ILetianpaiService) {
        Log.e("SpeechCmdResponser_1", "responseMove： $data")
        val audioCommand = mGson!!.fromJson(data, AudioCommand::class.java)
        responseMove(audioCommand, iLetianpaiService)
    }

    private fun closeTargetApp(iLetianpaiService: ILetianpaiService, packageName: String) {
        try {
            iLetianpaiService.setAppCmd(
                RobotRemoteConsts.COMMAND_TYPE_CLOSE_TARGET_APP,
                packageName
            )
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    /**
     * 响应唤醒
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun responseWakeup(
        iLetianpaiService: ILetianpaiService,
        command: String,
        data: String
    ) {
        updateWakeupState(iLetianpaiService, data)
    }

    /**
     * 响应角度
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun responseDOA(iLetianpaiService: ILetianpaiService, command: String, data: String) {
    }

    /**
     * 进入chatgpt
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun enterChatGpt(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        GestureCallback.instance
            .setGestures(GestureCenter.wakeupGesture, RGestureConsts.GESTURE_WAKE_UP)
    }

    /**
     * 退出chatgpt
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun quiteChatGpt(iLetianpaiService: ILetianpaiService, command: String, data: String) {
    }

    /**
     * 添加闹钟
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun addClock(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        Log.e("letianpai_speech", "command: $command")
        Log.e("letianpai_speech", "data: $data")
        RobotFuncResponseManager.stopRobot(iLetianpaiService)
        val time = data.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (time == null && time.size < 2) {
            return
        }
        val date = time[1]
        val year = date.substring(0, 4).toInt()
        val month = date.substring(4, 6).toInt()
        val day = date.substring(6, 8).toInt()

        Log.e("letianpai_speech", "year: $year")
        Log.e("letianpai_speech", "month: $month")
        Log.e("letianpai_speech", "day: $day")

        val times = data.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (times == null && times.size < 2) {
            return
        }

        //TODO Get the current time for comparison
        Log.e("letianpai_1234", "updateClockData ==== 3 ")
        Log.e(
            "letianpai_1234",
            "updateClockData ==== 4 Integer.valueOf(times[0]): " + times[0].toInt()
        )
        Log.e(
            "letianpai_1234",
            "updateClockData ==== 5 Integer.valueOf(times[1]): " + times[1].toInt()
        )

        //        GeeUIAlarmManager.getInstance(mContext).createAlarm(Integer.valueOf(times[0]), Integer.valueOf(times[1]));
        val hour = times[0].toInt()
        val minute = times[1].toInt()

        val isNeedToNextDay = isNeedToChangeToNextDay(year, month, day, hour, minute)
        if (isNeedToNextDay) {
            GeeUIAlarmManager.getInstance(mContext)
                .createAlarmNew(year, month, day + 1, hour, minute)
        } else {
            GeeUIAlarmManager.getInstance(mContext).createAlarmNew(year, month, day, hour, minute)
        }
    }

    private fun isNeedToChangeToNextDay(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Boolean {
        val currentTime = System.currentTimeMillis()
        val currentHour = TimeUtils.get24HourTime(currentTime)
        val currentMinute = TimeUtils.getMinTime(currentTime)
        val currentDay = TimeUtils.getDayTime(currentTime)
        val currentMon = TimeUtils.getMonTime(currentTime)
        val currentYear = TimeUtils.getYearTime(currentTime)

        Log.e("letianpai_1234", "currentYear: $currentYear")
        Log.e("letianpai_1234", "year: $year")
        Log.e("letianpai_1234", "currentMon: $currentMon")
        Log.e("letianpai_1234", "month: $month")
        Log.e("letianpai_1234", "currentDay: $currentDay")
        Log.e("letianpai_1234", "day: $day")
        Log.e("letianpai_1234", "currentHour: $currentHour")
        Log.e("letianpai_1234", "hour: $hour")
        Log.e("letianpai_1234", "currentMinute: $currentMinute")
        Log.e("letianpai_1234", "minute: $minute")

        if (day > currentDay) {
            return false
        } else if (day == currentDay && currentHour > hour) {
            return true
        } else if (day == currentDay && currentHour == hour && currentMinute > minute) {
            return true
        }

        return false
    }

    /**
     * Cancel Alarm
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun cancelClock(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        RobotFuncResponseManager.stopRobot(iLetianpaiService)
        val times = data.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (times == null && times.size < 2) {
            return
        }
        GeeUIAlarmManager.getInstance(mContext).cancelAlarm(times[0].toInt(), times[1].toInt())
    }

    /**
     * 添加提醒
     *
     * @param iLetianpaiService
     * @param command
     * @param data
     */
    private fun addReminder(iLetianpaiService: ILetianpaiService, command: String, data: String) {
        Log.e("letianpai_speech", "command1: $command")
        Log.e("letianpai_speech", "data1: $data")
        makeCurrentApp("com.letianpai.robot.alarm", data)
        LetianpaiFunctionUtil.openCountdown(mContext, data)
        RobotFuncResponseManager.stopRobot(iLetianpaiService)
    }


    /**
     * Updated wake word and tone settings
     *
     * @param data {"xiaole_switch":0,"xiaopai_switch":1,"selected_voice_id":"yyqiaf","selected_voice_name":"臻品女声悦悦",
     * "boy_voice_switch":0,"girl_voice_switch":1,"boy_child_voice_switch":0,"girl_child_voice_switch":0,"robot_voice_switch":0}
     */
    private fun updateWakeup(data: String) {
        val wakeUp = mGson!!.fromJson(data, WakeUp::class.java)
        GeeUILogUtils.logd("RemoteCmdResponser", "updateWakeup: $wakeUp")
            //TODO This part of the logic needs to be responded to within Spectrum, so comment out this part of the logic for now.
//        RhjAudioManager.instance.setSpeaker(mContext, wakeUp.getSelectedVoiceId());
//        RhjAudioManager.instance.setWakeupWord(mContext, wakeUp.getXiaoleSwitch() == 1, wakeUp.getXiaopaiSwitch() == 1);
    }

    private fun updateWakeupState(iLetianpaiService: ILetianpaiService, data: String) {
        speechCurrentStatus = data
        GeeUILogUtils.logd(
            "LTPAudioService",
            "updateWakeupState: $data---previousCmd::$previousCmd---currentBgPackageName::$currentBgPackageName"
        )
        when (data) {
            AudioServiceConst.ROBOT_STATUS_SILENCE -> {
                //At the end of the voice, determine if it is an automatic app switching
                val isAutoShow = SPUtils.getInstance(mContext)!!.getInt("isAutoShow")
                AppCmdResponser.getInstance(mContext)
                    .isAutoSwitchApp = (isAutoShow == 1f) //1, automatic switching, 0, manual switching

                if (previousCmd != null && (previousCmd == SpeechConst.COMMAND_OPEN_APP
                            || previousCmd == SpeechConst.COMMAND_CLOSE_APP
                            || previousCmd == SpeechConst.COMMAND_ADD_REMINDER)
                ) {
                    previousCmd = null
                    // return;
                }
                if (TextUtils.isEmpty(currentBgPackageName)) {
                    Log.e(
                        "letianpai_timer",
                        "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 5  LetianpaiFunctionUtil.isVideoCallRunning(mContext): " + LetianpaiFunctionUtil.isVideoCallRunning(
                            mContext
                        )
                    )
                    Log.e(
                        "letianpai_timer",
                        "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 5  LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext): " + LetianpaiFunctionUtil.isVideoCallServiceRunning(
                            mContext
                        )
                    )
                    Log.e(
                        "letianpai_timer",
                        "RobotMode:-------- " + RobotModeManager.getInstance(mContext).robotMode
                    )
                    val robotMode = RobotModeManager.getInstance(mContext).robotMode
                    if (robotMode == ViewModeConsts.VM_EMOTION || robotMode == ViewModeConsts.VM_TAKE_PHOTO || robotMode == ViewModeConsts.VM_DEMOSTRATE_MODE || robotMode == ViewModeConsts.VM_AUTO_CHARGING ||
                        LetianpaiFunctionUtil.isAlarmOnTheTop(mContext) ||
                        LetianpaiFunctionUtil.isNewAlarmOnTheTop(mContext) ||
                        LetianpaiFunctionUtil.isVideoCallRunning(mContext) ||
                        LetianpaiFunctionUtil.isVideoCallServiceRunning(mContext) || RobotModeManager.getInstance(
                            mContext
                        ).robotTrtcStatus != -1
                    ) {
                        //关闭思必驰
                        RobotFuncResponseManager.closeSpeechAudio(iLetianpaiService)
                        return
                    }
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 0)
                }
            }

            AudioServiceConst.ROBOT_STATUS_LISTENING -> {
                AppCmdResponser.getInstance(mContext)
                    .isAutoSwitchApp = false //1, automatic switching, 0, manual switching
                //When the robot is up, the currentPackageName value is null, otherwise there will be problems switching yujianbin
                LetianpaiFunctionUtil.currentPackageName = ""
                Log.e("---$TAG", " ===== 001  ====== AudioServiceConst.ROBOT_STATUS_LISTENING")
                try {
                    iLetianpaiService.setAppCmd(
                        RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                        PackageConsts.ROBOT_PACKAGE_NAME
                    )
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
                Log.e(
                    "letianpai_timer",
                    "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 1 currentBgPackageName: $currentBgPackageName"
                )
                makeCurrentApp(null, null)
                Log.e(
                    "letianpai_timer",
                    "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 1.1 currentBgPackageName: $currentBgPackageName"
                )
                if (LetianpaiFunctionUtil.isOtherRobotAppOnTheTop(mContext)) {
                    Log.e(
                        "letianpai_timer",
                        "AudioServiceConst.ROBOT_STATUS_LISTENING ===== 1.2 LetianpaiFunctionUtil.isOtherRobotAppOnTheTop(mContext) " + LetianpaiFunctionUtil.isOtherRobotAppOnTheTop(
                            mContext
                        )
                    )
                    shutdownAudioService(iLetianpaiService)

                    //                    try {
//                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP, RobotRemoteConsts.COMMAND_TYPE_CLOSE_APP);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
                    RobotFuncResponseManager.closeApp(iLetianpaiService)
                    Log.e("letianpai_screen_test", "black_screen_========== 1 ==========: ")
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT, 1)
                } else {
                    Log.e("letianpai_screen_test", "black_screen_========== 2 ==========: ")
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 1)
                }
            }

            AudioServiceConst.ROBOT_STATUS_MUSIC -> AppCmdResponser.getInstance(mContext)
                 .isAutoSwitchApp = false //1, automatic switching, 0, manual switching
            AudioServiceConst.ROBOT_STATUS_SPEAKING -> {
                Log.e("letianpai_timer", "AudioServiceConst.ROBOT_STATUS_SPEAKING ===== 2")

                if (TextUtils.isEmpty(currentBgPackageName)) {
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(ViewModeConsts.VM_AUDIO_WAKEUP_MODE, 2)
                }
            }

            else -> {}
        }
    }

    private fun shutdownAudioService(iLetianpaiService: ILetianpaiService) {
        if (!LetianpaiFunctionUtil.isAlarmRunning(mContext)) {
            return
        }
        try {
            iLetianpaiService.setRobotStatusCmd(
                AppCmdConsts.COMMAND_TYPE_SHUT_DOWN_AUDIO_SERVICE,
                AppCmdConsts.COMMAND_TYPE_SHUT_DOWN_AUDIO_SERVICE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun startBluetoothDiscover() {
        //Launching an Intent that modifies Bluetooth visibility
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        //Sets the duration of Bluetooth visibility, the method itself specifies a maximum of 300 seconds of visibility
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        if (mContext != null) {
            if (ActivityCompat.checkSelfPermission(
                    mContext,
                    "android.permission.BLUETOOTH_ADVERTISE"
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mContext.startActivity(intent)
        }
    }

    private fun startCountDownTimer(data: String) {
//        Message message = new Message();
//        message.what = UPDATE_COUNT_DOWN_TIMER;
//        message.obj = data;
//        handler.sendMessage(message);
    }

    private fun moveAppToTop(packageName: String) {
        // Get ActivityManager

        val activityManager = mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Get a list of recent tasks
        val recentTasks = activityManager.getRecentTasks(20, ActivityManager.RECENT_WITH_EXCLUDED)

        // Finding tasks for applications that need to be switched to the foreground
        for (recentTask in recentTasks) {
            if (recentTask.baseIntent.component!!.packageName == packageName) {
                // Found the task for the application that needs to be switched to the foreground
                val taskId = recentTask.persistentId
                activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
                break
            }
        }
    }

    companion object {
        private const val TAG = "SpeechCmdResponser"
        private var instance: SpeechCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): SpeechCmdResponser {
            synchronized(SpeechCmdResponser::class.java) {
                if (instance == null) {
                    instance = SpeechCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }

        /**
         * @param commandData
         * @param iLetianpaiService
         */
        fun responseMove(commandData: Any?, iLetianpaiService: ILetianpaiService?) {
            CommandResponseCallback.instance.setLTPCommand(
                MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,
                PowerMotion(3, 1).toString()
            )
            //TODO Send a message to the MCU to open the cliff and burst on the overhang
            CommandResponseCallback.instance.setLTPCommand(
                MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL,
                PowerMotion(5, 1).toString()
            )
            if (commandData is AudioCommand) {
                var direction = commandData.direction
                var directions = 0
                val number = commandData.number
                var numberInt = 0
                var internal = 0
                if (number != null) {
                    numberInt = number.toInt()
                    Log.e("SpeechCmdResponser_1", "numberInt： $numberInt")
                }
                if (number != null && numberInt > 0 && direction != null) {
                    if (direction == "前") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_FORWARD
                        internal = (numberInt - 1) * 300 * 6 + 8 * 300
                        directions = 63
                    } else if (direction == "后") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND
                        directions = 64
                        internal = (numberInt - 1) * 300 * 6 + 8 * 300
                    } else if (direction == "左") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND
                        directions = 5
                        internal = numberInt * 300 * 4
                    } else if (direction == "右") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND
                        directions = 6
                        internal = numberInt * 300 * 4
                    }


                    //                try {
                    Log.e(
                        "SpeechCmdResponser_1",
                        "responseMove： ===== iLetianpaiService.setMcuCommand"
                    )

                    //                    iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, new Motion(direction, numberInt).toString());
                    val list = ArrayList<GestureData>()
                    val data = GestureData()
                    data.footAction = Motion(null, directions, numberInt)
                    data.interval = internal.toLong()
                    list.add(data)
                    GestureCallback.instance
                        .setGestures(list, RGestureConsts.GESTURE_COMMAND_SPEECH_MOVE)

                    //                    iLetianpaiService.setMcuCommand(RobotRemoteConsts.COMMAND_TYPE_MOTION, new Motion(null, directions,numberInt).toString());

//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
                }
            }
        }
    }
}
