package com.letianpai.robot.taskservice.dispatch.expression

/**
 * 表情切换
 * @author liujunbin
 */
class ExpressionChangeCallback private constructor() {
    private var mExpressionChangeListener: ExpressionChangeListener? = null

    private object ExpressionChangeCallbackHolder {
        val instance: ExpressionChangeCallback = ExpressionChangeCallback()
    }

    interface ExpressionChangeListener {
        fun onExpressionChanged(expression: String?)
        fun onMainImageShow()
        fun onDisplayViewShow(viewName: String?)
        fun onChatGptView()
        fun onRemoteControlViewShow()
        fun onShowImage()
        fun onShowText(text: String?)
        fun onShowBlack(isShow: Boolean)
        fun onShutdown()
        fun onCountDownShow(time: String?)
    }

    fun setExpressionChangeListener(listener: ExpressionChangeListener?) {
        this.mExpressionChangeListener = listener
    }

    fun setExpression(expression: String?) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onExpressionChanged(expression)
        }
    }

    fun showMainImage() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onMainImageShow()
        }
    }

    fun showDisplayView(viewName: String?) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onDisplayViewShow(viewName)
        }
    }

    fun showRemoteControlView() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onRemoteControlViewShow()
        }
    }

    fun showRemoteImage() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onShowImage()
        }
    }

    fun showRemoteText(text: String?) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onShowText(text)
        }
    }

    fun showBlackView(isShow: Boolean) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onShowBlack(isShow)
        }
    }

    fun showShutDown() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onShutdown()
        }
    }

    fun showCountDown(time: String?) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener!!.onCountDownShow(time)
        }
    }


    companion object {
        @JvmStatic
        val instance: ExpressionChangeCallback
            get() = ExpressionChangeCallbackHolder.instance
    }
}
