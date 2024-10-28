package com.letianpai.robot.control.callback

/**
 * @author liujunbin
 */
class ControlSteeringEngineCallback private constructor() {
    private var mControlSteeringEngineListener: ControlSteeringEngineListener? = null


    private object ControlSteeringEngineCallbackHolder {
        val instance: ControlSteeringEngineCallback = ControlSteeringEngineCallback()
    }

    interface ControlSteeringEngineListener {
        fun onControlSteeringEngine(footSwitch: Boolean, sensorSwitch: Boolean)
    }


    fun setControlSteeringEngineListener(listener: ControlSteeringEngineListener?) {
        this.mControlSteeringEngineListener = listener
    }


    fun setControlSteeringEngine(footSwitch: Boolean, sensorSwitch: Boolean) {
        mControlSteeringEngineListener!!.onControlSteeringEngine(footSwitch, sensorSwitch)
    }


    companion object {
        val instance: ControlSteeringEngineCallback
            get() = ControlSteeringEngineCallbackHolder.instance
    }
}
