package com.letianpai.robot.control.callback


/**
 * Created by liujunbin
 */
class RobotCommandWordsCallback private constructor() {
    private var mRobotCommandWordsUpdateListener: RobotCommandWordsUpdateListener? = null

    private object RobotCommandWordsCallbackHolder {
        val instance: RobotCommandWordsCallback = RobotCommandWordsCallback()
    }

    interface RobotCommandWordsUpdateListener {
        fun showBattery(showBattery: Boolean)
    }

    fun setRobotCommandWordsUpdateListener(listener: RobotCommandWordsUpdateListener?) {
        this.mRobotCommandWordsUpdateListener = listener
    }

    fun showBattery(showBattery: Boolean) {
        if (mRobotCommandWordsUpdateListener != null) {
            mRobotCommandWordsUpdateListener!!.showBattery(showBattery)
        }
    }

    companion object {
        val instance: RobotCommandWordsCallback
            get() = RobotCommandWordsCallbackHolder.instance
    }
}












