package com.letianpai.robot.control.callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liujunbin
 */

public class TemperatureUpdateCallback {
    public static final int HIGH_TEMP = 90;
    public static final int TARGET_TEMP = 75;
    private boolean isHighTemp = false;

    private float temp;
    private List<TemperatureUpdateListener> mTemperatureUpdateListener = new ArrayList();

    private static class TemperatureUpdateCallBackHolder {
        private static TemperatureUpdateCallback instance = new TemperatureUpdateCallback();
    }

    private TemperatureUpdateCallback() {

    }

    public static TemperatureUpdateCallback getInstance() {
        return TemperatureUpdateCallBackHolder.instance;
    }

    public interface TemperatureUpdateListener {
        void onTemperatureUpdate(float temp);
    }

    public void registerTemperatureUpdateListener(TemperatureUpdateListener listener) {
        if (mTemperatureUpdateListener != null) {
            mTemperatureUpdateListener.add(listener);
        }
    }

    public void unregisterTemperatureUpdateListener(TemperatureUpdateListener listener) {
        if (mTemperatureUpdateListener != null) {
            mTemperatureUpdateListener.remove(listener);
        }
    }

    public void setTemperature(float temp) {
        this.temp = temp;
        if (temp >= HIGH_TEMP){
            isHighTemp = true;

        }else if (temp <= TARGET_TEMP){
            isHighTemp = false;

        }else if (temp > TARGET_TEMP && temp < HIGH_TEMP){

        }else{
            isHighTemp = false;
        }

        for (int i = 0; i < mTemperatureUpdateListener.size(); i++) {
            if (mTemperatureUpdateListener.get(i) != null) {
                mTemperatureUpdateListener.get(i).onTemperatureUpdate(temp);
            }
        }
    }

    public boolean isInHighTemperature(){
        return isHighTemp;
    }

    public float getTemp() {
        return temp;
    }
}
