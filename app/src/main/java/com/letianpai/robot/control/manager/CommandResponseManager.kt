package com.letianpai.robot.control.manager

import android.content.Context
import android.os.Handler
import android.os.RemoteException
import com.letianpai.robot.components.utils.GeeUILogUtils
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.control.mode.callback.ModeChangeCallback
import com.letianpai.robot.control.storage.RobotConfigManager
import com.letianpai.robot.control.system.LetianpaiFunctionUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.response.app.AppCmdResponser.Companion.getInstance
import com.letianpai.robot.response.ble.BleCmdResponser.Companion.getInstance
import com.letianpai.robot.response.identify.IdentifyCmdResponser.Companion.getInstance
import com.letianpai.robot.response.mi.MiIotCmdResponser.Companion.getInstance
import com.letianpai.robot.response.remote.RemoteCmdResponser.Companion.getInstance
import com.letianpai.robot.response.robotStatus.RobotStatusResponser.Companion.getInstance
import com.letianpai.robot.response.sensor.SensorCmdResponser.Companion.getInstance
import com.letianpai.robot.response.speech.SpeechCmdResponser.Companion.getInstance
import com.letianpai.robot.taskservice.audio.Const
import com.letianpai.robot.taskservice.audio.parser.AudioCommand
import com.renhejia.robot.commandlib.consts.ATCmdConsts
import com.renhejia.robot.commandlib.consts.MCUCommandConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.commandlib.parser.motion.Motion
import com.renhejia.robot.gesturefactory.manager.GestureManager
import com.renhejia.robot.gesturefactory.parser.GestureData
import com.renhejia.robot.gesturefactory.util.GestureConsts
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * Voice Command Execution Unit
 *
 * @author liujunbin
 */
class CommandResponseManager private constructor(context: Context) {
    private var mContext: Context? = null

    private fun init(context: Context) {
        this.mContext = context
    }

    var i: Int = 0

    init {
        init(context)
    }

    fun responseGesture(gestureData: GestureData?, iLetianpaiService: ILetianpaiService) {
        responseGestureData(mContext, gestureData, iLetianpaiService)
    }

    fun responseGestures(gesture: String?, iLetianpaiService: ILetianpaiService) {
        val list: ArrayList<GestureData>? = GestureManager.getInstance(mContext!!).getRobotGesture(
            gesture!!
        )
        responseGestures(list!!, -1, iLetianpaiService)
    }

    fun responseGestures(gesture: String, gestureId: Int, iLetianpaiService: ILetianpaiService) {
        GeeUILogUtils.logi(
            "AudioCmdResponseManager",
            "responseGestures: gesture= $gesture | gestureId: $gestureId"
        )
        val list: ArrayList<GestureData>? =
            GestureManager.getInstance(mContext!!).getRobotGesture(gesture)
        responseGestures(list!!, gestureId, iLetianpaiService)
    }

    fun responseGestures(
        list: ArrayList<GestureData>,
        taskId: Int,
        iLetianpaiService: ILetianpaiService
    ) {
        GestureDataThreadExecutor.instance.execute {
            GeeUILogUtils.logi("AudioCmdResponseManager", "run start: taskId: $taskId")
            for (gestureData: GestureData in list) {
                responseGesture(gestureData, iLetianpaiService)
                try {
                    if (gestureData.interval == 0L) {
                        Thread.sleep(2000)
                    } else {
                        Thread.sleep(gestureData.interval)
                    }
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            GeeUILogUtils.logi("AudioCmdResponseManager", "run end: taskId: $taskId")
            GestureCallback.instance.setGesturesComplete("list", taskId)
        }
    }

    companion object {
        private var instance: CommandResponseManager? = null
        fun getInstance(context: Context): CommandResponseManager {
            synchronized(CommandResponseManager::class.java) {
                if (instance == null) {
                    instance = CommandResponseManager(context.applicationContext)
                }
                return instance!!
            }
        }

        fun commandResponse(
            context: Context,
            commandFrom: String?,
            commandType: String,
            commandData: Any,
            iLetianpaiService: ILetianpaiService
        ) {
            if (commandFrom == null) {
                return
            }

            if (commandType == Const.RhjController.move) {
                responseMove(commandData, iLetianpaiService)
            } else if (commandType == Const.RhjController.congraturationBirthday) {
                if (instance == null) {
                    instance = CommandResponseManager(context.applicationContext)
                }
                getInstance(instance!!.mContext!!).responseGestures(
                    GestureConsts.GESTURE_BIRTHDAY,
                    iLetianpaiService
                )
            } else if (commandType == Const.DUIController.ShutDown) {
                Handler().postDelayed({ SystemFunctionUtil.shutdownRobot(context) }, 2000)
            } else if (commandType == Const.RhjController.turn) {
            } else if (commandType == RobotRemoteConsts.COMMAND_TYPE_SOUND) {
            } else if (commandType == MCUCommandConsts.COMMAND_AUDIO_TURN_AROUND) {
                turnDirection(
                    iLetianpaiService,
                    ATCmdConsts.AT_STR_MOVEW_LOCAL_ROUND_LEFT,
                    commandData as Int
                )
            }
        }

        private fun turnDirection(
            iLetianpaiService: ILetianpaiService,
            direction: String,
            number: Int
        ) {
            try {
                iLetianpaiService.setMcuCommand(
                    RobotRemoteConsts.COMMAND_TYPE_MOTION,
                    Motion(direction, number).toString()
                )
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        /**
         * @param commandData
         * @param iLetianpaiService
         */
        fun responseMove(commandData: Any?, iLetianpaiService: ILetianpaiService) {
            if (commandData is AudioCommand) {
                var direction: String? = commandData.direction
                val number: String? = commandData.number
                if (number != null && direction != null) {
                    if (direction == "前") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_FORWARD
                    } else if (direction == "后") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND
                    } else if (direction == "左") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND
                    } else if (direction == "右") {
                        direction = MCUCommandConsts.COMMAND_VALUE_MOTION_BACKEND
                    }
                    val numberInt: Int = number.toInt()
                    try {
//                    iLetianpaiService.setCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_MOTION, (new Motion(direction, numberInt)).toString()));
                        iLetianpaiService.setMcuCommand(
                            RobotRemoteConsts.COMMAND_TYPE_MOTION,
                            Motion(direction, numberInt).toString()
                        )
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun responseGestureData(
            context: Context?,
            gestureData: GestureData?,
            iLetianpaiService: ILetianpaiService
        ) {
            logGestureData(gestureData)

            if (gestureData == null) {
                return
            }

            try {
                if (gestureData.ttsInfo != null) {
                    //The response unit is in the Launcher
                    iLetianpaiService.setTTS("speakText", gestureData.ttsInfo!!.tts)
                }


                if (!RobotModeManager.getInstance(context!!)
                        .isNoExpressionMode && gestureData.expression != null
                ) {
//            if (gestureData.getExpression() != null) {
                    //The response unit is in the Launcher
//                iLetianpaiService.setCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_FACE, ((gestureData.getExpression()).toString())));
                    // TODO Increase expression judgement
                    // TODO && LetianpaiFunctionUtil.isRobotRunning(context)
                    if (!LetianpaiFunctionUtil.isRobotOnTheTop(context) && LetianpaiFunctionUtil.isRobotRunning(
                            context
                        )
                    ) {
                        LetianpaiFunctionUtil.openRobotFace(
                            context,
                            GestureConsts.GESTURE_DEMO,
                            gestureData.expression!!.face
                        )
                    } else if (!LetianpaiFunctionUtil.isRobotOnTheTop(context) && !LetianpaiFunctionUtil.isRobotRunning(
                            context
                        )
                    ) {
                        LetianpaiFunctionUtil.openRobotFace(
                            context,
                            GestureConsts.GESTURE_DEMO,
                            gestureData.expression!!.face
                        )
                    } else {
                        if (gestureData.expression != null && gestureData.expression!!.face != null) {
                            if (gestureData.expression!!.isIs24HourGesture) {
                                ModeChangeCallback.instance.setModeChange(
                                    ViewModeConsts.VM_24_HOUR_PLAY, 1
                                )
                                LetianpaiFunctionUtil.controlSteeringEngine(context, true, true)
                            }
                            iLetianpaiService.setExpression(
                                RobotRemoteConsts.COMMAND_TYPE_FACE,
                                (gestureData.expression!!.face)
                            )
                        }
                    }
                }
                if (gestureData.antennalight != null) {
                    //Response unit in MCUservice
                    iLetianpaiService.setMcuCommand(
                        RobotRemoteConsts.COMMAND_TYPE_ANTENNA_LIGHT,
                        (gestureData.antennalight).toString()
                    )
                }
                if (gestureData.soundEffects != null) {
                    //If it's in sleep mode and there's an open snoring sound
                    if (RobotModeManager.getInstance(context).isSleepMode) {
                        if (RobotConfigManager.getInstance(context)
                                !!.sleepSoundModeSwitch
                        ) {
                            iLetianpaiService.setAudioEffect(
                                RobotRemoteConsts.COMMAND_TYPE_SOUND,
                                gestureData.soundEffects!!.sound
                            )
                        }
                    } else {
                        iLetianpaiService.setAudioEffect(
                            RobotRemoteConsts.COMMAND_TYPE_SOUND,
                            gestureData.soundEffects!!.sound
                        )
                    }
                }

                if (gestureData.footAction != null && ((!ChargingUpdateCallback.instance
                        .isCharging) || (RobotModeManager.getInstance(
                        instance!!.mContext!!
                    ).isPowerOnChargingMode))
                ) {
                    //Response unit in MCUservice
                    iLetianpaiService.setMcuCommand(
                        RobotRemoteConsts.COMMAND_TYPE_MOTION,
                        (gestureData.footAction).toString()
                    )
                }

                if (gestureData.earAction != null) {
                    //Response unit in MCUservice
                    iLetianpaiService.setMcuCommand(
                        RobotRemoteConsts.COMMAND_TYPE_ANTENNA_MOTION,
                        (gestureData.earAction).toString()
                    )
                } else {
                    //Antennas
//                AntennaMotion antennaMotion=new AntennaMotion("sturn");
//                iLetianpaiService.setCommand(new LtpCommand(MCUCommandConsts.COMMAND_TYPE_ANTENNA_MOTION, (gestureData.getEarAction()).toString()));
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }


        private fun logGestureData(gestureData: GestureData?) {
            GeeUILogUtils.logi("AudioCmdResponseManager",
                "Parsing to the actual implementation unit $gestureData"
            )
        }
    }
}
