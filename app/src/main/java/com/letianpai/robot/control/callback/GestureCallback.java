package com.letianpai.robot.control.callback;


import com.renhejia.robot.gesturefactory.parser.GestureData;

import java.util.ArrayList;

/**
 * @author liujunbin
 */
public class GestureCallback {


    private ArrayList<GestureResponseListener> mGestureResponseListener = new ArrayList<>();
    private ArrayList<GestureCompleteListener> mGestureCompleteListener = new ArrayList<>();
    private ArrayList<GestureCompleteListener> mOneShotGestureCompleteListener = new ArrayList<>();

    private static class GestureCallbackHolder {
        private static final GestureCallback instance = new GestureCallback();
    }

    private GestureCallback() {

    }

    public static GestureCallback getInstance() {
        return GestureCallbackHolder.instance;
    }

    public interface GestureResponseListener {
        void onGestureReceived(String gesture);

        void onGestureReceived(String gesture, int gestureId);

        void onGesturesReceived(ArrayList<GestureData> list, int taskId);

        void onGesturesReceived(GestureData gestureData);
    }

    public void setGestureListener(GestureResponseListener listener) {
        this.mGestureResponseListener.add(listener);
    }

    public void setGestureCompleteListener(GestureCompleteListener listener) {
        this.mGestureCompleteListener.add(listener);
    }

    public void setOneShotGestureCompleteListener(GestureCompleteListener mOneShotGestureCompleteListener) {
        this.mOneShotGestureCompleteListener.add(mOneShotGestureCompleteListener);
    }

    public void removeOneShotGestureCompleteListener(GestureCompleteListener mGestureCompleteListener) {
        if (this.mOneShotGestureCompleteListener.size() > 0) {
            mOneShotGestureCompleteListener.remove(mGestureCompleteListener);
        }
    }


    public void setGesture(String gesture) {
        for (int i = 0; i < mGestureCompleteListener.size(); i++) {
            if (mGestureResponseListener.get(i) != null) {
                mGestureResponseListener.get(i).onGestureReceived(gesture);
            }
        }
    }

    public void setGesture(String gesture, int geTaskId) {
        for (int i = 0; i < mGestureResponseListener.size(); i++) {
            if (mGestureResponseListener.get(i) != null) {
                mGestureResponseListener.get(i).onGestureReceived(gesture);
            }
        }
    }

    public void setGestures(ArrayList<GestureData> list, int taskId) {
        for (int i = 0; i < mGestureResponseListener.size(); i++) {
            if (mGestureResponseListener.get(i) != null) {
                mGestureResponseListener.get(i).onGesturesReceived(list, taskId);
            }
        }
    }

    public void setGesture(GestureData gestureData) {
        for (int i = 0; i < mGestureResponseListener.size(); i++) {
            if (mGestureResponseListener.get(i) != null) {
                mGestureResponseListener.get(i).onGesturesReceived(gestureData);
            }
        }
    }

    public interface GestureCompleteListener {
        void onGestureCompleted(String gesture, int geTaskId);
    }

    public void setGesturesComplete(String gesture, int geTaskId) {
        for (int i = 0; i < mGestureResponseListener.size(); i++) {
            if (mGestureCompleteListener.get(i) != null) {
                mGestureCompleteListener.get(i).onGestureCompleted(gesture, geTaskId);
            }
        }
        for (int i = 0; i < mOneShotGestureCompleteListener.size(); i++) {
            if (mOneShotGestureCompleteListener.get(i) != null) {
                mOneShotGestureCompleteListener.get(i).onGestureCompleted(gesture, geTaskId);
            }
        }
    }
}
