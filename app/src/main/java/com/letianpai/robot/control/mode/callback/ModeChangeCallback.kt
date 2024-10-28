package com.letianpai.robot.control.mode.callback

/**
 * 模式切换状态回调
 * @author liujunbin
 */
class ModeChangeCallback private constructor() {
    private var mModeChangeListener: ModeChangeListener? = null

    private object MModeChangeCallbackHolder {
        val instance: ModeChangeCallback = ModeChangeCallback()
    }

    interface ModeChangeListener {
        fun onViewModeChanged(viewMode: Int, modeStatus: Int)
    }

    fun setViewModeChangeListener(listener: ModeChangeListener?) {
        this.mModeChangeListener = listener
    }

    fun setModeChange(viewMode: Int, modeStatus: Int) {
        if (mModeChangeListener != null) {
            mModeChangeListener!!.onViewModeChanged(viewMode, modeStatus)
        }
    }

    companion object {
        val instance: ModeChangeCallback
            get() {
                return MModeChangeCallbackHolder.instance
            }
    }
}
