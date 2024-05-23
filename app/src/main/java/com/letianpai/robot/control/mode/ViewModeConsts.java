package com.letianpai.robot.control.mode;

/**
 * 模式常量
 * @author liujunbin
 */
public interface ViewModeConsts {
    //
    public static final int VM_AUTO_MODE = 1;
    // 待机模式
    public static final int VM_STANDBY_MODE = 2;

    public static final int VM_STATIC_MODE = 3;


    public static final int VM_CHARGING_MODE = 4;
    public static final int VM_SLEEP_MODE = 5;
    public static final int VM_FUNCTION_MODE = 6;
    public static final int VM_CHAT_GPT_MODE = 7;
    public static final int VM_REMOTE_CONTROL_MODE = 8;
    public static final int VM_DEMOSTRATE_MODE = 9;
    public static final int VM_AUTO_PLAY_MODE = 10;
    public static final int VM_AUTO_NEW_PLAY_MODE = 11;
    public static final int VM_DISPLAY_MODE = 999;
    public static final int VM_APP_MODE = 20;
    public static final int VM_DEEP_SLEEP_MODE = 21;
    public static final int VM_CHARGING = 22;
    public static final int VM_GESTURE = 23;
    public static final int VM_24_HOUR_PLAY = 25;
    public static final int VM_POWER_ON_CHARGING = 26;
    public static final int VM_BLACK_SCREEN_SLEEP_MODE = 27;
    public static final int VM_BLACK_SCREEN_NIGHT_SLEEP_MODE = 28;
    public static final int VM_AUTO_CHARGING = 29;

    public static final int VM_TRTC_MONITOR = 1001;
    public static final int VM_TRTC_TRANSFORM = 1002;

    public static final int VM_UNBIND_DEVICE = 1003;


    /**
     * 情绪识别类型
     */
    public static final int VM_EMOTION = 30;

    /**
     * 情绪识别类型
     */
    public static final int VM_TAKE_PHOTO = 31;

    /**
     * 语音唤醒
     */
    public static final int VM_AUDIO_WAKEUP_MODE = 12;

    /**
     * kill机器人进程
     */
    public static final int VM_KILL_ROBOT_PROGRESS = 99;

    /**
     * kill机器人检测进程
     */
    public static final int VM_KILL_IDENT_PROGRESS = 100;

    /**
     * kill机器人进程
     */
    public static final int VM_KILL_ROBOT_IDENT_PROGRESS = 101;

    /**
     * OPEN App Mode（杀掉语音进程）和 identity进程
     */
    public static final int VM_KILL_ALL_INVALID_SERVICE = 102;

    /**
     * OPEN App Mode（杀掉语音进程）和 identity进程
     */
    public static final int VM_KILL_SPEECH_SERVICE = 103;

    /**
     * 语音唤醒 换形态占位
     */
    public static final int VM_AUDIO_WAKEUP_MODE_DEFAULT = 19;

    /**
     * 一次性执行
     */
    public static final int VM_ONESHOT_MODE = 13;

    public static final int VM_UPDATE_MODE   = 16;
    public static final int VM_FACTORY_MODE  = 17;
    public static final int VM_HAND_REG_MODE  = 18;

    public static final int VM_BODY_REG_MODE  = 33;
    public static final int VM_FACE_REG_MODE  = 34;
    public static final int VM_MI_IOT_MODE  = 35;


    public static final int VIEW_MODE_IN = 1;
    public static final int VIEW_MODE_OUT = 0;


    /**
     * app 模式静态图
     */
    public static final int APP_MODE_TIME = 1;
    public static final int APP_MODE_EVENT_COUNTDOWN = 2;
    public static final int APP_MODE_WEATHER = 3;
    public static final int APP_MODE_FANS = 4;
    public static final int APP_MODE_MESSAGE = 5;
    public static final int APP_MODE_NEWS = 6;
    public static final int APP_MODE_COMMEMORATION = 7;
    public static final int APP_MODE_STOCK = 8;
    public static final int APP_MODE_WORD = 9;
    public static final int APP_MODE_CUSTOM = 10;
    public static final int APP_MODE_LAMP = 11;
    public static final int APP_MODE_OTHER = 111;
    public static final int APP_MODE_EXPRESSION = 112;




    public static final int APP_MODE_MIJIA = 99;

    public static final int ROBOT_STATUS_FUNCTION_MODE = 0;
    public static final int ROBOT_STATUS_CLOSE_SCREEN = 1;
    public static final int ROBOT_STATUS_SLEEP_ZZZ = 2;


}
