package com.letianpai.robot.taskservice.dispatch.expression;

/**
 * 表情切换
 * @author liujunbin
 */
public class ExpressionChangeCallback {

    private ExpressionChangeListener mExpressionChangeListener;

    private static class ExpressionChangeCallbackHolder {
        private static ExpressionChangeCallback instance = new ExpressionChangeCallback();
    }

    private ExpressionChangeCallback() {

    }

    public static ExpressionChangeCallback getInstance() {
        return ExpressionChangeCallbackHolder.instance;
    }

    public interface ExpressionChangeListener {
        void onExpressionChanged(String expression);
        void onMainImageShow();
        void onDisplayViewShow(String viewName);
        void onChatGptView();
        void onRemoteControlViewShow();
        void onShowImage();
        void onShowText(String text);
        void onShowBlack(boolean isShow);
        void onShutdown();
        void onCountDownShow(String time);

    }

    public void setExpressionChangeListener(ExpressionChangeListener listener) {
        this.mExpressionChangeListener = listener;
    }

    public void setExpression(String expression) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onExpressionChanged(expression);
        }
    }

    public void showMainImage() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onMainImageShow();
        }
    }
    public void showDisplayView(String viewName) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onDisplayViewShow(viewName);
        }
    }

    public void showRemoteControlView() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onRemoteControlViewShow();
        }
    }

    public void showRemoteImage() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onShowImage();
        }
    }

    public void showRemoteText(String text) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onShowText(text);
        }
    }

    public void showBlackView(boolean isShow) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onShowBlack(isShow);
        }
    }

    public void showShutDown() {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onShutdown();
        }
    }
    public void showCountDown(String time) {
        if (mExpressionChangeListener != null) {
            mExpressionChangeListener.onCountDownShow(time);
        }
    }


}
