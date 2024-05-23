package com.letianpai.robot.response.appstatus;

import android.content.Context;

import com.google.gson.Gson;
import com.letianpai.robot.control.callback.RobotCommandWordsCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.gesturefactory.manager.GestureResPool;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;


/**
 * @author liujunbin
 */
public class AppStatusResponser {
    private Gson mGson;

    private static AppStatusResponser instance;
    private Context mContext;
    private GestureResPool gestureResPool;

    private AppStatusResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static AppStatusResponser getInstance(Context context) {
        synchronized (AppStatusResponser.class) {
            if (instance == null) {
                instance = new AppStatusResponser(context.getApplicationContext());
            }
            return instance;
        }

    }

    private void init() {
        mGson = new Gson();
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        if (command == null) {
            return;
        }

        switch (command) {
            case RobotRemoteConsts.COMMAND_TYPE_MOTION:
                break;

            case RobotRemoteConsts.COMMAND_SET_APP_MODE:
                if (data == null){
                    return;
                }
                break;

            default:
                break;
        }

    }

}
