/**
 *
 */
package com.letianpai.robot.control.storage

/**
 *
 * @author liujunbin
 */
interface RobotConfigConst {
    companion object {
        const val KEY_SSID: String = "robot_ssid"
        const val KEY_PASSWORD: String = "robot_password"
        const val KEY_ACTIVATED: String = "robot_activated"
        const val KEY_RANDOM: String = "random"

        const val KEY_GENERAL_BATTERY_SWITCH: String = "general_battery_switch"
        const val VALUE_GENERAL_BATTERY_SWITCH_OPEN: String = "1"
        const val VALUE_GENERAL_BATTERY_SWITCH_CLOSED: String = "0"

        const val KEY_VOLUME: String = "volume"

        const val KEY_START_SLEEP_TIME: String = "start_sleep_time"
        const val KEY_END_SLEEP_TIME: String = "end_sleep_time"
        const val KEY_SLEEP_MODE_STATUS: String = "sleep_mode_status"
        const val KEY_SOUND_MODE_SWITCH: String = "sound_mode_switch"

        const val KEY_CLOSE_SCREEN_MODE_SWITCH: String = "close_screen_mode_switch"
        const val KEY_SLEEP_SOUND_MODE_SWITCH: String = "sleep_sound_mode_switch"
        const val KEY_SLEEP_TIME_STATUS_MODE_SWITCH: String = "sleep_time_status_mode_switch"

        const val KEY_REMIND_WATER_START_TIME: String = "remind_water_start_time"
        const val KEY_REMIND_WATER_END_TIME: String = "remind_water_end_time"
        const val KEY_REMIND_WATER_SWITCH: String = "remind_water_switch"
        const val KEY_REMIND_: String = "remind_"

        const val KEY_REMIND_KEEP_NOTICE_TIME: String = "remind_keep_notice_time"
        const val KEY_REMIND_KEEP_NOTICE_WEEK: String = "remind_keep_notice_week"
        const val KEY_REMIND_KEEP_NOTICE_SWITCH: String = "remind_keep_notice_switch"

        const val KEY_REMIND_SIT_START_TIME: String = "remind_sit_start_time"
        const val KEY_REMIND_SIT_END_TIME: String = "remind_sit_end_time"
        const val KEY_REMIND_SIT_SWITCH: String = "remind_sit_switch"

        const val KEY_REMIND_SIT_POSTURE_START_TIME: String = "remind_sit_posture_start_time"
        const val KEY_REMIND_SIT_POSTURE_END_TIME: String = "remind_sit_posture_end_time"
        const val KEY_REMIND_SIT_POSTURE_SWITCH: String = "remind_sit_posture_switch"

        const val KEY_UPDATE_ROBOT_TIME: String = "update_robot_time"

        const val KEY_AUTOMATIC_RECHARGE_VAL: String = "automatic_recharge_val"
        const val KEY_AUTOMATIC_RECHARGE_SWITCH: String = "automatic_recharge_switch"
    }
}


