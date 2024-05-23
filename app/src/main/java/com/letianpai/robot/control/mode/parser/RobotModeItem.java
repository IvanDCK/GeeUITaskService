package com.letianpai.robot.control.mode.parser;

public class RobotModeItem {

    private int priority;

    private int modeId;

    private boolean isElectricityOn;

    public int getModeId() {
        return modeId;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isElectricityOn() {
        return isElectricityOn;
    }

    public void setElectricityOn(boolean electricityOn) {
        isElectricityOn = electricityOn;
    }

    public void setModeId(int modeId) {
        this.modeId = modeId;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "{" +
                "priority:" + priority +
                ", modeId:" + modeId +
                ", isElectricityOn:" + isElectricityOn +
                '}';
    }
}
