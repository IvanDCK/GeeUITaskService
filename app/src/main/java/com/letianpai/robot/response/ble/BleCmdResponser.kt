package com.letianpai.robot.response.ble

import android.content.Context
import com.google.gson.Gson
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.renhejia.robot.commandlib.consts.BleCmdConsts
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class BleCmdResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
    }

    fun commandDistribute(
        iLetianpaiService: ILetianpaiService?,
        command: String?,
        data: String?,
        isNeedResponse: Boolean
    ) {
        if (!SystemUtil.robotActivateStatus) {
            return
        }
        if (command == null) {
            return
        }

        when (command) {
            BleCmdConsts.BLE_COMMAND_TYPE_REBOOT -> SystemFunctionUtil.reboot(mContext)
            BleCmdConsts.BLE_COMMAND_TYPE_SHUTDOWN -> SystemFunctionUtil.shutdownRobot(mContext)
            else -> {}
        }
    }


    companion object {
        private var instance: BleCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): BleCmdResponser {
            synchronized(BleCmdResponser::class.java) {
                if (instance == null) {
                    instance = BleCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
