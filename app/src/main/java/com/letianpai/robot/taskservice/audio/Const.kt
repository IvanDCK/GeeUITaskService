package com.letianpai.robot.taskservice.audio

class Const {
    /**
     * 提醒命令
     * https://www.duiopen.com/docs/tixing_tyty
     */
    object Remind {
        const val Insert: String = "ai.dui.dskdm.reminder.insert"
        const val Remove: String = "ai.dui.dskdm.reminder.remove"
    }

    /**
     * 思必驰中控技能
     */
    object DUIController {
        const val ShutDown: String = "DUI.System.Shutdown"

        /**
         * command" : {
         * "param" : {
         *
         *
         * "volume" : "+"
         * },
         * "api" : "DUI.MediaController.SetVolume"
         * },
         *
         *
         * 声音跳到百分之二十的时候 volume："20"
         * 声音大一点 volume："+"
         * 声音小一点 volume："-"
         */
        const val SetVolumeWithNumber: String = "DUI.MediaController.SetVolume"
        const val OpenBluetooth: String = "DUI.System.Connectivity.OpenBluetooth"
        const val CloseBluetooth: String = "DUI.System.Connectivity.CloseBluetooth"
        const val SystemUpgrade: String = "DUI.System.Upgrade"
        const val OpenSettings: String = "DUI.System.OpenSettings"
        const val CloseSettings: String = "DUI.System.CloseSettings"

        /**
         * 静音模式
         */
        const val SoundsOpenMode: String = "DUI.System.Sounds.OpenMode"
        const val SoundsCloseMode: String = "DUI.System.Sounds.CloseMode"
    }

    /**
     * 播放控制
     * https://www.duiopen.com/docs/bofangkongzhi_tytycomm
     */
    object MediaController {
        const val Play: String = "DUI.MediaController.Play"
        const val Pause: String = "DUI.MediaController.Pause"
        const val Stop: String = "DUI.MediaController.Stop"
        const val Replay: String = "DUI.MediaController.Replay"
        const val Prev: String = "DUI.MediaController.Prev"
        const val Next: String = "DUI.MediaController.Next"
        const val Switch: String = "DUI.MediaController.Switch"
        const val SwitchPlayMode: String = "DUI.MediaController.SwitchPlayMode"
        const val AddCollectionList: String = "DUI.MediaController.AddCollectionList"
        const val RemoveCollectionList: String = "DUI.MediaController.RemoveCollectionList"
        const val PlayCollectionList: String = "DUI.MediaController.PlayCollectionList"
        const val OpenCollectionList: String = "DUI.MediaController.OpenCollectionList"
        const val CloseCollectionList: String = "DUI.MediaController.CloseCollectionList"
        const val SetPlayMode: String = "DUI.MediaController.SetPlayMode"
        const val Progress: String = "DUI.MediaController.Progress"
    }

    object RhjController {
        const val GoBack: String = "DUI.System.GoBack"
        const val GoHome: String = "DUI.System.GoHome"
        const val OpenMode: String = "DUI.System.UserMode.OpenMode"

        //生日快乐祝福
        const val congraturationBirthday: String = "rhj.controller.congraturation"

        //        public static final String congraturationBirthday = "rhj.controller.congraturation";
        //转向
        const val move: String = "rhj.controller.navigation" //?direction=#方向#&number=#数值#
        const val turn: String = "rhj.controller.turn" //?direction=#方向#&number=#数值#
        const val show: String = "rhj.controller.show" //表演金鸡独立
        const val flighty: String = "rhj.controller.flighty" //撒娇
        const val angry: String = "rhj.controller.angry"

        //跳舞
        const val dance: String = "rhj.controller.dance"


        //打开关闭
        const val takePhoto: String = "rhj.controller.takephoto"
        const val openSitePose: String = "rhj.controller.opensitepose"
        const val closeSitePose: String = "rhj.controller.closesitepose"
        const val openDrink: String = "rhj.controller.opendrink"
        const val closeDrink: String = "rhj.controller.closedrink"
        const val openFitness: String = "rhj.controller.openfitness"
        const val closeFitness: String = "rhj.controller.closefitness"
        const val openSleep: String = "rhj.controller.opensleep"
        const val closeSleep: String = "rhj.controller.closesleep"
        const val openSedentary: String = "rhj.controller.opensedentary" //久坐提醒
        const val closeSedentary: String = "rhj.controller.closesedentary"
        const val openBirthday: String = "rhj.controller.openbirthday"
        const val closeBirthday: String = "rhj.controller.closebirthday"
        const val openChildren: String = "rhj.controller.openchildren"
        const val closeChildren: String = "rhj.controller.closechildren"

        const val enterChatGpt: String = "rhj.controller.chatgpt.enter"
        const val quitChatGpt: String = "rhj.controller.chatgpt.quit"
    }
}
