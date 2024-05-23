package com.letianpai.robot.response.identify;

import android.content.Context;
import android.os.RemoteException;

import com.google.gson.Gson;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.commandlib.consts.PackageConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;


/**
 * @author liujunbin
 */
public class IdentifyCmdResponser {
    private Gson mGson;

    private static IdentifyCmdResponser instance;
    private Context mContext;

    private IdentifyCmdResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static IdentifyCmdResponser getInstance(Context context) {
        synchronized (IdentifyCmdResponser.class) {
            if (instance == null) {
                instance = new IdentifyCmdResponser(context.getApplicationContext());
            }
            return instance;
        }

    }

    private void init() {
        mGson = new Gson();
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        if (!SystemUtil.getRobotActivateStatus()){
            return;
        }
        if (command == null) {
            return;
        }

        switch (command) {

            case AppCmdConsts.COMMAND_TYPE_HAND_REG:
                if (data.equals(AppCmdConsts.COMMAND_TYPE_HAND_REG_IN)) {
                    RobotModeManager.getInstance(mContext).switchRobotMode(RobotModeManager.VM_HAND_REG_MODE, 1);
                } else if (data.equals(AppCmdConsts.COMMAND_TYPE_HAND_REG_OUT)) {
                    try {
                        iLetianpaiService.setRobotStatusCmd(PackageConsts.PACKAGE_NAME_IDENT,AppCmdConsts.COMMAND_VALUE_EXIT);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    try {
                        iLetianpaiService.setAppCmd(RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,PackageConsts.ROBOT_PACKAGE_NAME );
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    RobotModeManager.getInstance(mContext).switchRobotMode(RobotModeManager.VM_HAND_REG_MODE, 0);
                }else{
                    GestureCallback.getInstance().setGestures(GestureCenter.getHandGestureWithID(data), RGestureConsts.GESTURE_COMMAND_HAND_ID);
                }

                break;

            default:
                break;
        }

    }


}
