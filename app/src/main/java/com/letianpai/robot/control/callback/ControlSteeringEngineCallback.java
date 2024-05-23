package com.letianpai.robot.control.callback;

/**
 * @author liujunbin
 */
public class ControlSteeringEngineCallback {

    private ControlSteeringEngineListener mControlSteeringEngineListener;


    private static class ControlSteeringEngineCallbackHolder {
        private static final ControlSteeringEngineCallback instance = new ControlSteeringEngineCallback();
    }

    private ControlSteeringEngineCallback() {

    }

    public static ControlSteeringEngineCallback getInstance() {
        return ControlSteeringEngineCallbackHolder.instance;
    }


    public interface ControlSteeringEngineListener {
        void onControlSteeringEngine(boolean footSwitch, boolean sensorSwitch);
    }


    public void setControlSteeringEngineListener (ControlSteeringEngineListener listener) {
        this.mControlSteeringEngineListener = listener;
    }



    public void setControlSteeringEngine(boolean footSwitch, boolean sensorSwitch) {
        mControlSteeringEngineListener.onControlSteeringEngine(footSwitch,sensorSwitch);
    }



}
