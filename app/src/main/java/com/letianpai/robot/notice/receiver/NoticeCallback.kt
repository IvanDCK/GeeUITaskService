package com.letianpai.robot.notice.receiver

/**
 * Created by liujunbin
 */
class NoticeCallback private constructor() {
    private val mNoticeTimeListener: MutableList<NoticeTimeListener?> = ArrayList()

    private object AlarmCallbackHolder {
        val instance: NoticeCallback = NoticeCallback()
    }

    interface NoticeTimeListener {
        fun onNoticeTimeOut(hour: Int, minute: Int, title: String?)
    }

    fun registerNoticeTimeListener(listener: NoticeTimeListener?) {
        if (mNoticeTimeListener != null) {
            mNoticeTimeListener.add(listener)
        }
    }

    fun unregisterNoticeTimeListener(listener: NoticeTimeListener?) {
        if (mNoticeTimeListener != null) {
            mNoticeTimeListener.remove(listener)
        }
    }


    fun setNoticeTimeOut(hour: Int, minute: Int, title: String?) {
        for (i in mNoticeTimeListener.indices) {
            if (mNoticeTimeListener[i] != null) {
                mNoticeTimeListener[i]!!.onNoticeTimeOut(hour, minute, title)
            }
        }
    }

    companion object {
        val instance: NoticeCallback
            get() {
                return AlarmCallbackHolder.instance
            }
    }
}
