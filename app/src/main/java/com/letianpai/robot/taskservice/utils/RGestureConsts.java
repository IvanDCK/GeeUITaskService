package com.letianpai.robot.taskservice.utils;

import java.util.ArrayList;

/**
 * 姿态标签常量
 *
 * @author liujunbin
 */
public class RGestureConsts {

    public static final int GESTURE_ID_REFORM_DEFAULT = 1000;
    public static final int GESTURE_ID_DEFAULT = 1001;         //默认姿态
    public static final int GESTURE_ID_STANDBY_DEFAULT = 1002; //默认待机
    public static final int GESTURE_ID_FOUND_PEOPLE = 1003;
    public static final int GESTURE_ID_STAND_GESTURE = 1004;
    public static final int GESTURE_ID_RANDOM_GESTURE = 1005;
    public static final int GESTURE_ID_SEARCH_PEOPLE2 = 1006;
    public static final int GESTURE_ID_STAND_RESET = 1111;
    public static final int GESTURE_ID_SEARCH_TIME_8 = 1008;

    /**
     * 默认姿态
     */
    public static final int GESTURE_CHANGE_STANDBY = 1011;
    /**
     * 找人动作
     */
    public static final int GESTURE_CHANGE_PEOPLE = 1012;
    /**
     * 待机默认姿态
     */
    public static final int GESTURE_CHANGE_STANDBY_SECOND = 1013;
    /**
     * 常态姿态
     */
    public static final int GESTURE_CHANGE_COMMON = 1014;
    /**
     * 第二次找人
     */
    public static final int GESTURE_CHANGE_PEOPLE_SECOND = 1015;
    /**
     * 定义的姿态库中的姿态随机
     */
    public static final int GESTURE_CHANGE_ALL = 1016;

    /**
     * 常态姿态
     */
    public static final int GESTURE_CHANGE_COMMON_DISPLAY = 1018;

    /**
     * 整点报时
     */
    public static final int GESTURE_ID_TIME_KEEPER = 1099;

    /**
     * 悬空开始
     */
    public static final int GESTURE_ID_DANGLING_START = 2100;
    /**
     * 悬空结束
     */
    public static final int GESTURE_ID_DANGLING_END = 2101;

    /**
     * 倒下开始
     */
    public static final int GESTURE_ID_FALLDOWN_START = 2102;
    /**
     * 倒下结束
     */
    public static final int GESTURE_ID_FALLDOWN_END = 2103;
    /**
     * 单击
     */
    public static final int GESTURE_ID_TAP = 2104;
    /**
     * 双击
     */
    public static final int GESTURE_ID_DOUBLE_TAP = 2105;
    /**
     * 长按
     */
    public static final int GESTURE_ID_LONG_PRESS = 2106;

    /**
     * 防跌落往前
     */
    public static final int GESTURE_ID_CLIFF_FORWARD = 2107;

    /**
     * 防跌落往后
     */
    public static final int GESTURE_ID_CLIFF_BACKEND = 2108;

    /**
     * 防跌落往左
     */
    public static final int GESTURE_ID_CLIFF_LEFT = 2109;

    /**
     * 防跌落往右
     */
    public static final int GESTURE_ID_CLIFF_RIGHT = 2110;

    /**
     * 摇晃
     */
    public static final int GESTURE_ID_WAGGLE = 2111;

    /**
     * 避障
     */
    public static final int GESTURE_ID_Tof = 2112;


    /**
     * 24小时常态姿态
     */
    public static final int GESTURE_ID_24_HOUR = 3107;

    /**
     * 开心表情ID
     */
    public static final int GESTURE_ID_HAPPY = 4001;

    /**
     * 开心表情ID
     */
    public static final int GESTURE_ID_SAD = 4002;

    /**
     * 唤醒
     */
    public static final int GESTURE_WAKE_UP = 4000;

    /**
     *
     */
    public static final int GESTURE_SPEAKING = 4004;

    /**
     * 唤醒
     */
    public static final int GESTURE_COMMAND_HAND_ID = 4003;

    /**
     * 唤醒
     */
    public static final int GESTURE_GPT_LISTENING = 4006;

    /**
     *
     */
    public static final int GESTURE_GPT_SPEAKING = 4007;

    /**
     * 情绪识别类型
     */
    public static final int GESTURE_TYPE_EMOTION = 4099;

    /**
     * 思必驰语音控制
     */
    public static final int GESTURE_COMMAND_SPEECH_MOVE = 5001;
    public static final int GESTURE_COMMAND_SPEECH_BIRTHDAY = 5002;

    /**
     * 闹钟启动
     */
    public static final int GESTURE_COMMAND_CLOCK_START = 6001;

    /**
     * 闹钟关闭
     */
    public static final int GESTURE_COMMAND_CLOCK_STOP = 6002;

    public static final int GESTURE_COMMAND_DELAY = 6003;

    /**
     * 进入睡眠模式
     */
    public static final int GESTURE_COMMAND_GO_TO_SLEEP = 6007;

    /**
     * 高温模式TTS
     */
    public static final int GESTURE_COMMAND_HIGH_TEMP_TTS = 6009;

    /**
     * 高温模式TTS Hibernation
     */
    public static final int GESTURE_COMMAND_HIBERNATION = 6010;

    /**
     * 高温模式TTS Hibernation
     */
    public static final int GESTURE_COMMAND_EXIT_HIBERNATION = 6011;

    /**
     * 米IOT
     */
    public static final int GESTURE_MI_IOT = 8000;

    /**
     * 开机充电
     */
    public static final int GESTURE_POWER_ON_CHARGING = 9000;

    /**
     * 机器人模式下的低电提醒
     */
    public static final int GESTURE_ROBOT_LOW_BATTERY_NOTICE = 9001;

    /**
     * 关闭音效
     */
    public static final int GESTURE_ID_CLOSE_SOUND_EFFECT = 9002;


    public static final int GESTURE_ID_REMIND_KEEP = 10001;
    public static final int GESTURE_ID_REMIND_WATER = 10002;
    public static final int GESTURE_ID_REMIND_SITE = 10003;
    public static final int GESTURE_ID_REMIND_SED = 10004;



}
