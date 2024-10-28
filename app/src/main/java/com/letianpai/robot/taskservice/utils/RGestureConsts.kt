package com.letianpai.robot.taskservice.utils

/**
 * 姿态标签常量
 *
 * @author liujunbin
 */
object RGestureConsts {
    const val GESTURE_ID_REFORM_DEFAULT: Int = 1000
    const val GESTURE_ID_DEFAULT: Int = 1001 //默认姿态
    const val GESTURE_ID_STANDBY_DEFAULT: Int = 1002 //默认待机
    const val GESTURE_ID_FOUND_PEOPLE: Int = 1003
    const val GESTURE_ID_STAND_GESTURE: Int = 1004
    const val GESTURE_ID_RANDOM_GESTURE: Int = 1005
    const val GESTURE_ID_SEARCH_PEOPLE2: Int = 1006
    const val GESTURE_ID_STAND_RESET: Int = 1111
    const val GESTURE_ID_SEARCH_TIME_8: Int = 1008

    /**
     * 默认姿态
     */
    const val GESTURE_CHANGE_STANDBY: Int = 1011

    /**
     * 找人动作
     */
    const val GESTURE_CHANGE_PEOPLE: Int = 1012

    /**
     * 待机默认姿态
     */
    const val GESTURE_CHANGE_STANDBY_SECOND: Int = 1013

    /**
     * 常态姿态
     */
    const val GESTURE_CHANGE_COMMON: Int = 1014

    /**
     * 第二次找人
     */
    const val GESTURE_CHANGE_PEOPLE_SECOND: Int = 1015

    /**
     * 定义的姿态库中的姿态随机
     */
    const val GESTURE_CHANGE_ALL: Int = 1016

    /**
     * 常态姿态
     */
    const val GESTURE_CHANGE_COMMON_DISPLAY: Int = 1018

    /**
     * 整点报时
     */
    const val GESTURE_ID_TIME_KEEPER: Int = 1099

    /**
     * 悬空开始
     */
    const val GESTURE_ID_DANGLING_START: Int = 2100

    /**
     * 悬空结束
     */
    const val GESTURE_ID_DANGLING_END: Int = 2101

    /**
     * 倒下开始
     */
    const val GESTURE_ID_FALLDOWN_START: Int = 2102

    /**
     * 倒下结束
     */
    const val GESTURE_ID_FALLDOWN_END: Int = 2103

    /**
     * 单击
     */
    const val GESTURE_ID_TAP: Int = 2104

    /**
     * 双击
     */
    const val GESTURE_ID_DOUBLE_TAP: Int = 2105

    /**
     * 长按
     */
    const val GESTURE_ID_LONG_PRESS: Int = 2106

    /**
     * 防跌落往前
     */
    const val GESTURE_ID_CLIFF_FORWARD: Int = 2107

    /**
     * 防跌落往后
     */
    const val GESTURE_ID_CLIFF_BACKEND: Int = 2108

    /**
     * 防跌落往左
     */
    const val GESTURE_ID_CLIFF_LEFT: Int = 2109

    /**
     * 防跌落往右
     */
    const val GESTURE_ID_CLIFF_RIGHT: Int = 2110

    /**
     * 摇晃
     */
    const val GESTURE_ID_WAGGLE: Int = 2111

    /**
     * 避障
     */
    const val GESTURE_ID_Tof: Int = 2112


    /**
     * 24小时常态姿态
     */
    const val GESTURE_ID_24_HOUR: Int = 3107

    /**
     * 开心表情ID
     */
    const val GESTURE_ID_HAPPY: Int = 4001

    /**
     * 开心表情ID
     */
    const val GESTURE_ID_SAD: Int = 4002

    /**
     * 唤醒
     */
    const val GESTURE_WAKE_UP: Int = 4000

    /**
     *
     */
    const val GESTURE_SPEAKING: Int = 4004

    /**
     * 唤醒
     */
    const val GESTURE_COMMAND_HAND_ID: Int = 4003

    /**
     * 唤醒
     */
    const val GESTURE_GPT_LISTENING: Int = 4006

    /**
     *
     */
    const val GESTURE_GPT_SPEAKING: Int = 4007

    /**
     * 情绪识别类型
     */
    const val GESTURE_TYPE_EMOTION: Int = 4099

    /**
     * 思必驰语音控制
     */
    const val GESTURE_COMMAND_SPEECH_MOVE: Int = 5001
    const val GESTURE_COMMAND_SPEECH_BIRTHDAY: Int = 5002

    /**
     * 闹钟启动
     */
    const val GESTURE_COMMAND_CLOCK_START: Int = 6001

    /**
     * 闹钟关闭
     */
    const val GESTURE_COMMAND_CLOCK_STOP: Int = 6002

    const val GESTURE_COMMAND_DELAY: Int = 6003

    /**
     * 进入睡眠模式
     */
    const val GESTURE_COMMAND_GO_TO_SLEEP: Int = 6007

    /**
     * 高温模式TTS
     */
    const val GESTURE_COMMAND_HIGH_TEMP_TTS: Int = 6009

    /**
     * 高温模式TTS Hibernation
     */
    const val GESTURE_COMMAND_HIBERNATION: Int = 6010

    /**
     * 高温模式TTS Hibernation
     */
    const val GESTURE_COMMAND_EXIT_HIBERNATION: Int = 6011

    /**
     * 米IOT
     */
    const val GESTURE_MI_IOT: Int = 8000

    /**
     * 开机充电
     */
    const val GESTURE_POWER_ON_CHARGING: Int = 9000

    /**
     * 机器人模式下的低电提醒
     */
    const val GESTURE_ROBOT_LOW_BATTERY_NOTICE: Int = 9001

    /**
     * 关闭音效
     */
    const val GESTURE_ID_CLOSE_SOUND_EFFECT: Int = 9002


    const val GESTURE_ID_REMIND_KEEP: Int = 10001
    const val GESTURE_ID_REMIND_WATER: Int = 10002
    const val GESTURE_ID_REMIND_SITE: Int = 10003
    const val GESTURE_ID_REMIND_SED: Int = 10004
}
