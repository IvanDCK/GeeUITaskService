package com.letianpai.robot.taskservice.remote

object RemoteCmdCallback {

    private var remoteCmdListener: RemoteCmdListener? = null

    fun interface RemoteCmdListener {
        fun onRemoteCmdReceived(commandType: String, commandData: Any)
    }

    fun setRemoteCmdReceivedListener(listener: RemoteCmdListener) {
        remoteCmdListener = listener
    }

    fun setRemoteCmd(commandType: String, commandData: Any) {
        remoteCmdListener?.onRemoteCmdReceived(commandType, commandData)
    }
}