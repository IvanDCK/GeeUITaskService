package com.letianpai.robot.taskservice.dispatch.statusbar

/**
 * 表情切换
 * @author liujunbin
 */
class StatusBarUpdateCallback private constructor() {
    private var mStatusBarChangeListener: StatusBarChangeListener? = null

    private object StatusBarUpdateCallbackHolder {
        val instance: StatusBarUpdateCallback = StatusBarUpdateCallback()
    }

    interface StatusBarChangeListener {
        fun onStatusBarTextChanged(viewName: String?)
    }

    fun setStatusBarTextChangeListener(listener: StatusBarChangeListener?) {
        this.mStatusBarChangeListener = listener
    }

    fun setStatusBarText(viewName: String?) {
        if (mStatusBarChangeListener != null) {
            mStatusBarChangeListener!!.onStatusBarTextChanged(viewName)
        }
    }


    companion object {
        @JvmStatic
        val instance: StatusBarUpdateCallback
            get() = StatusBarUpdateCallbackHolder.instance
    }
}
