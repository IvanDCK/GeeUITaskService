package com.letianpai.robot.response.identify

import android.content.Context
import android.os.RemoteException
import com.google.gson.Gson
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts.Companion.VM_HAND_REG_MODE
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.AppCmdConsts
import com.renhejia.robot.commandlib.consts.PackageConsts
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class IdentifyCmdResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService, command: String?, data: String) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return
        }
        if (command == null) {
            return
        }

        when (command) {
            AppCmdConsts.COMMAND_TYPE_HAND_REG -> when (data) {
                AppCmdConsts.COMMAND_TYPE_HAND_REG_IN -> {
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(VM_HAND_REG_MODE, 1)
                }
                AppCmdConsts.COMMAND_TYPE_HAND_REG_OUT -> {
                    try {
                        iLetianpaiService.setRobotStatusCmd(
                            PackageConsts.PACKAGE_NAME_IDENT,
                            AppCmdConsts.COMMAND_VALUE_EXIT
                        )
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                    try {
                        iLetianpaiService.setAppCmd(
                            RobotRemoteConsts.COMMAND_VALUE_KILL_PROCESS,
                            PackageConsts.ROBOT_PACKAGE_NAME
                        )
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                    RobotModeManager.getInstance(mContext)
                        .switchRobotMode(VM_HAND_REG_MODE, 0)
                }
                else -> {
                    GestureCallback.instance.setGestures(
                        GestureCenter.getHandGestureWithID(data),
                        RGestureConsts.GESTURE_COMMAND_HAND_ID
                    )
                }
            }

            else -> {}
        }
    }


    companion object {
        private var instance: IdentifyCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): IdentifyCmdResponser {
            synchronized(IdentifyCmdResponser::class.java) {
                if (instance == null) {
                    instance = IdentifyCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
