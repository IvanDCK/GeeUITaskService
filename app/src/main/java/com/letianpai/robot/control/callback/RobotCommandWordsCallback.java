package com.letianpai.robot.control.callback;


import android.util.Log;

import com.renhejia.robot.commandlib.parser.tips.Tips;

/**
 * Created by liujunbin
 */

public class RobotCommandWordsCallback {

    private RobotCommandWordsUpdateListener mRobotCommandWordsUpdateListener;

    private static class RobotCommandWordsCallbackHolder {
        private static RobotCommandWordsCallback instance = new RobotCommandWordsCallback();
    }

    private RobotCommandWordsCallback() {}

    public static RobotCommandWordsCallback getInstance() {
        return RobotCommandWordsCallbackHolder.instance;
    }

    public interface RobotCommandWordsUpdateListener {
        void showBattery(boolean showBattery);
    }

    public void setRobotCommandWordsUpdateListener(RobotCommandWordsUpdateListener listener) {
        this.mRobotCommandWordsUpdateListener = listener;
    }

    public void showBattery(boolean showBattery) {
        if (mRobotCommandWordsUpdateListener != null) {
            mRobotCommandWordsUpdateListener.showBattery(showBattery);
        }
    }

}












