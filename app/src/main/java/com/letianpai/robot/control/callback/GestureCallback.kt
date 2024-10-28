package com.letianpai.robot.control.callback

import com.renhejia.robot.gesturefactory.parser.GestureData


/**
 * @author liujunbin
 */
class GestureCallback private constructor() {
    private val mGestureResponseListener = ArrayList<GestureResponseListener?>()
    private val mGestureCompleteListener = ArrayList<GestureCompleteListener?>()
    private val mOneShotGestureCompleteListener = ArrayList<GestureCompleteListener?>()

    private object GestureCallbackHolder {
        val instance: GestureCallback = GestureCallback()
    }

    interface GestureResponseListener {
        fun onGestureReceived(gesture: String?)

        fun onGestureReceived(gesture: String, gestureId: Int)

        fun onGesturesReceived(list: ArrayList<GestureData>, taskId: Int)

        fun onGesturesReceived(gestureData: GestureData?)
    }

    fun setGestureListener(listener: GestureResponseListener?) {
        mGestureResponseListener.add(listener)
    }

    fun setGestureCompleteListener(listener: GestureCompleteListener?) {
        mGestureCompleteListener.add(listener)
    }

    fun setOneShotGestureCompleteListener(mOneShotGestureCompleteListener: GestureCompleteListener?) {
        this.mOneShotGestureCompleteListener.add(mOneShotGestureCompleteListener)
    }

    fun removeOneShotGestureCompleteListener(mGestureCompleteListener: GestureCompleteListener?) {
        if (mOneShotGestureCompleteListener.size > 0) {
            mOneShotGestureCompleteListener.remove(mGestureCompleteListener)
        }
    }


    fun setGesture(gesture: String?) {
        for (i in mGestureCompleteListener.indices) {
            if (mGestureResponseListener[i] != null) {
                mGestureResponseListener[i]!!.onGestureReceived(gesture)
            }
        }
    }

    fun setGesture(gesture: String?, geTaskId: Int) {
        for (i in mGestureResponseListener.indices) {
            if (mGestureResponseListener[i] != null) {
                mGestureResponseListener[i]!!.onGestureReceived(gesture)
            }
        }
    }

    fun setGestures(list: ArrayList<GestureData>, taskId: Int) {
        for (i in mGestureResponseListener.indices) {
            if (mGestureResponseListener[i] != null) {
                mGestureResponseListener[i]!!.onGesturesReceived(list, taskId)
            }
        }
    }

    fun setGesture(gestureData: GestureData?) {
        for (i in mGestureResponseListener.indices) {
            if (mGestureResponseListener[i] != null) {
                mGestureResponseListener[i]!!.onGesturesReceived(gestureData)
            }
        }
    }

    fun interface GestureCompleteListener {
        fun onGestureCompleted(gesture: String, geTaskId: Int)
    }

    fun setGesturesComplete(gesture: String, geTaskId: Int) {
        for (i in mGestureResponseListener.indices) {
            if (mGestureCompleteListener[i] != null) {
                mGestureCompleteListener[i]!!.onGestureCompleted(gesture, geTaskId)
            }
        }
        for (i in mOneShotGestureCompleteListener.indices) {
            if (mOneShotGestureCompleteListener[i] != null) {
                mOneShotGestureCompleteListener[i]!!.onGestureCompleted(gesture, geTaskId)
            }
        }
    }

    companion object {
        val instance: GestureCallback
            get() = GestureCallbackHolder.instance
    }
}
