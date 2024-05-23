package com.letianpai.robot.taskservice.dispatch.command;

import android.util.Log;

/**
 * 模式切换状态回调
 *
 * @author liujunbin
 */
public class CommandResponseCallback {

    private CommandResponseListener mModeChangeListener;
    private LTPCommandResponseListener mLTPCommandResponseListener;
    private LTPIdentifyCommandResponseListener mLTPIdentifyCommandResponseListener;
    private LTPRobotStatusCmdResponseListener mLTPRobotStatusCmdResponseListener;

    private static class CommandResponseCallbackHolder {
        private static CommandResponseCallback instance = new CommandResponseCallback();
    }

    public static CommandResponseCallback getInstance() {
        return CommandResponseCallbackHolder.instance;
    }

    private CommandResponseCallback() {

    }

    public interface CommandResponseListener {
        void onCommandReceived(String commandFrom, String commandType, Object commandData);
    }

    public interface LTPCommandResponseListener {
        void onLTPCommandReceived(String command, String data);
    }

    public interface LTPRobotStatusCmdResponseListener {
        void onRobotStatusCmdResponse(String command, String data);
    }
    public interface LTPIdentifyCommandResponseListener {
        void onIdentifyCmdResponse(String command, String data);
    }

    public void setCommandReceivedListener(CommandResponseListener listener) {
        this.mModeChangeListener = listener;
    }

    public void setLTPCommandResponseListener(LTPCommandResponseListener listener) {
        this.mLTPCommandResponseListener = listener;
    }
    public void setLTPIdentifyCommandResponseListener(LTPIdentifyCommandResponseListener listener) {
        this.mLTPIdentifyCommandResponseListener = listener;
    }

    public void setLTPRobotStatusCmdResponseListener(LTPRobotStatusCmdResponseListener robotStatusCmdResponseListener) {
        this.mLTPRobotStatusCmdResponseListener = robotStatusCmdResponseListener;
    }

    public void setCommand(String commandFrom, String commandType, Object commandData) {
        if (mModeChangeListener != null) {
            mModeChangeListener.onCommandReceived(commandFrom, commandType, commandData);
        }
    }

//    public void setLTPCommand(LtpCommand ltpCommand) {
//        Log.e("letianpai_test_control","switchToNewAutoPlayMode === COMMAND_VALUE_CHANGE_MODE_ROBOT ======= 9.1 ============mode: ");
//        if (mLTPCommandResponseListener != null) {
//            Log.e("letianpai_test_control","switchToNewAutoPlayMode === COMMAND_VALUE_CHANGE_MODE_ROBOT ======= 9.2 ============mode: ");
//            mLTPCommandResponseListener.onLTPCommandReceived(ltpCommand);
//        }else{
//            Log.e("letianpai_test_control","switchToNewAutoPlayMode === COMMAND_VALUE_CHANGE_MODE_ROBOT ======= 9.3 ============mode: mLTPCommandResponseListener is null");
//        }
//    }

    public void setLTPCommand(String command,String data) {
        if (mLTPCommandResponseListener != null) {
            mLTPCommandResponseListener.onLTPCommandReceived(command,data);
        }
    }

    public void setIdentifyCmd(String command,String data) {
        if (mLTPIdentifyCommandResponseListener != null) {
            mLTPIdentifyCommandResponseListener.onIdentifyCmdResponse(command,data);
        }
    }

    public void setRobotStatusCmdResponse(String command,String data) {
        if (mLTPRobotStatusCmdResponseListener != null) {
            mLTPRobotStatusCmdResponseListener.onRobotStatusCmdResponse(command,data);
        }
    }


}
