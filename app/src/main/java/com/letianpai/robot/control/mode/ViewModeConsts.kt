package com.letianpai.robot.control.mode

/**
 * ViewMode Consts
 * @author liujunbin
 */
interface ViewModeConsts {
    companion object {
        //
        const val VM_AUTO_MODE: Int = 1

        // standby mode
        const val VM_STANDBY_MODE: Int = 2

        const val VM_STATIC_MODE: Int = 3


        const val VM_CHARGING_MODE: Int = 4
        const val VM_SLEEP_MODE: Int = 5
        const val VM_FUNCTION_MODE: Int = 6
        const val VM_CHAT_GPT_MODE: Int = 7
        const val VM_REMOTE_CONTROL_MODE: Int = 8
        const val VM_DEMOSTRATE_MODE: Int = 9
        const val VM_AUTO_PLAY_MODE: Int = 10
        const val VM_AUTO_NEW_PLAY_MODE: Int = 11
        const val VM_DISPLAY_MODE: Int = 999
        const val VM_APP_MODE: Int = 20
        const val VM_DEEP_SLEEP_MODE: Int = 21
        const val VM_CHARGING: Int = 22
        const val VM_GESTURE: Int = 23
        const val VM_24_HOUR_PLAY: Int = 25
        const val VM_POWER_ON_CHARGING: Int = 26
        const val VM_BLACK_SCREEN_SLEEP_MODE: Int = 27
        const val VM_BLACK_SCREEN_NIGHT_SLEEP_MODE: Int = 28
        const val VM_AUTO_CHARGING: Int = 29

        const val VM_TRTC_MONITOR: Int = 1001
        const val VM_TRTC_TRANSFORM: Int = 1002

        const val VM_UNBIND_DEVICE: Int = 1003


        /**
         * Types of Emotion Recognition
         */
        const val VM_EMOTION: Int = 30

        /**
         * Types of Emotion Recognition
         */
        const val VM_TAKE_PHOTO: Int = 31

        /**
         * Wake up with a voice
         */
        const val VM_AUDIO_WAKEUP_MODE: Int = 12

        /**
         * Kill robot process
         */
        const val VM_KILL_ROBOT_PROGRESS: Int = 99

        /**
         * Kill robot detection process
         */
        const val VM_KILL_IDENT_PROGRESS: Int = 100

        /**
         * Kill robot process
         */
        const val VM_KILL_ROBOT_IDENT_PROGRESS: Int = 101

        /**
         * OPEN App Mode (kill the voice process) and identity process
         */
        const val VM_KILL_ALL_INVALID_SERVICE: Int = 102

        /**
         * OPEN App Mode (kill the voice process) and identity process
         */
        const val VM_KILL_SPEECH_SERVICE: Int = 103

        /**
         * Voice Wakeup Change Form Occupancy
         */
        const val VM_AUDIO_WAKEUP_MODE_DEFAULT: Int = 19

        /**
         * One-time implementation
         */
        const val VM_ONESHOT_MODE: Int = 13

        const val VM_UPDATE_MODE: Int = 16
        const val VM_FACTORY_MODE: Int = 17
        const val VM_HAND_REG_MODE: Int = 18

        const val VM_BODY_REG_MODE: Int = 33
        const val VM_FACE_REG_MODE: Int = 34
        const val VM_MI_IOT_MODE: Int = 35


        const val VIEW_MODE_IN: Int = 1
        const val VIEW_MODE_OUT: Int = 0


        /**
         * app Mode Static Chart
         */
        const val APP_MODE_TIME: Int = 1
        const val APP_MODE_EVENT_COUNTDOWN: Int = 2
        const val APP_MODE_WEATHER: Int = 3
        const val APP_MODE_FANS: Int = 4
        const val APP_MODE_MESSAGE: Int = 5
        const val APP_MODE_NEWS: Int = 6
        const val APP_MODE_COMMEMORATION: Int = 7
        const val APP_MODE_STOCK: Int = 8
        const val APP_MODE_WORD: Int = 9
        const val APP_MODE_CUSTOM: Int = 10
        const val APP_MODE_LAMP: Int = 11
        const val APP_MODE_OTHER: Int = 111
        const val APP_MODE_EXPRESSION: Int = 112


        const val APP_MODE_MIJIA: Int = 99

        const val ROBOT_STATUS_FUNCTION_MODE: Int = 0
        const val ROBOT_STATUS_CLOSE_SCREEN: Int = 1
        const val ROBOT_STATUS_SLEEP_ZZZ: Int = 2
    }
}
