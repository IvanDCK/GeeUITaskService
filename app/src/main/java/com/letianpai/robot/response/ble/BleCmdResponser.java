package com.letianpai.robot.response.ble;

import android.content.Context;

import com.google.gson.Gson;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.renhejia.robot.commandlib.consts.BleCmdConsts;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;


/**
 * @author liujunbin
 */
public class BleCmdResponser {
    private Gson mGson;

    private static BleCmdResponser instance;
    private Context mContext;

    private BleCmdResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static BleCmdResponser getInstance(Context context) {
        synchronized (BleCmdResponser.class) {
            if (instance == null) {
                instance = new BleCmdResponser(context.getApplicationContext());
            }
            return instance;
        }

    }

    private void init() {
        mGson = new Gson();
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data, boolean isNeedResponse) {
        if (!SystemUtil.getRobotActivateStatus()){
            return;
        }
        if (command == null) {
            return;
        }

        switch (command) {

            case BleCmdConsts.BLE_COMMAND_TYPE_REBOOT:
                SystemFunctionUtil.reboot(mContext);
                break;

            case BleCmdConsts.BLE_COMMAND_TYPE_SHUTDOWN:
                SystemFunctionUtil.shutdownRobot(mContext);
                break;

            default:
                break;
        }

    }


}
