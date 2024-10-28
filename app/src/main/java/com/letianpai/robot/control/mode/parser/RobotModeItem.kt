package com.letianpai.robot.control.mode.parser

class RobotModeItem {
    var priority: Int = 0

    var modeId: Int = 0

    var isElectricityOn: Boolean = false

    override fun toString(): String {
        return "{" +
                "priority:" + priority +
                ", modeId:" + modeId +
                ", isElectricityOn:" + isElectricityOn +
                '}'
    }
}
