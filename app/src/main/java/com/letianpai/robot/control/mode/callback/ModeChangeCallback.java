package com.letianpai.robot.control.mode.callback;

/**
 * 模式切换状态回调
 * @author liujunbin
 */
public class ModeChangeCallback {

    private ModeChangeListener mModeChangeListener;

    private static class MModeChangeCallbackHolder {
        private static ModeChangeCallback instance = new ModeChangeCallback();
    }

    private ModeChangeCallback() {

    }

    public static ModeChangeCallback getInstance() {
        return MModeChangeCallbackHolder.instance;
    }

    public interface ModeChangeListener {
        void onViewModeChanged(int viewMode,int modeStatus);
    }

    public void setViewModeChangeListener(ModeChangeListener listener) {
        this.mModeChangeListener = listener;
    }

    public void setModeChange(int viewMode,int modeStatus) {
        if (mModeChangeListener != null) {
            mModeChangeListener.onViewModeChanged(viewMode,modeStatus);
        }
    }
}
