package com.letianpai.robot.notice.receiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liujunbin
 */

public class NoticeCallback {

    private final List<NoticeTimeListener> mNoticeTimeListener = new ArrayList();

    private static class AlarmCallbackHolder {
        private static final NoticeCallback instance = new NoticeCallback();
    }

    private NoticeCallback() {

    }

    public static NoticeCallback getInstance() {
        return AlarmCallbackHolder.instance;
    }

    public interface NoticeTimeListener {
        void onNoticeTimeOut(int hour, int minute, String title);
    }

    public void registerNoticeTimeListener(NoticeTimeListener listener) {
        if (mNoticeTimeListener != null) {
            mNoticeTimeListener.add(listener);
        }
    }

    public void unregisterNoticeTimeListener(NoticeTimeListener listener) {
        if (mNoticeTimeListener != null) {
            mNoticeTimeListener.remove(listener);
        }
    }


    public void setNoticeTimeOut(int hour, int minute, String title) {
        for (int i = 0; i < mNoticeTimeListener.size(); i++) {
            if (mNoticeTimeListener.get(i) != null) {
                mNoticeTimeListener.get(i).onNoticeTimeOut(hour, minute,title);
            }
        }
    }

}
