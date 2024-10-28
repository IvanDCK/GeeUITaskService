package com.letianpai.robot.response.appstatus

import android.content.Context
import com.google.gson.Gson
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts
import com.renhejia.robot.gesturefactory.manager.GestureResPool
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class AppStatusResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    private val gestureResPool: GestureResPool? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService?, command: String?, data: String?) {
        if (command == null) {
            return
        }

        when (command) {
            RobotRemoteConsts.COMMAND_TYPE_MOTION -> {}
            RobotRemoteConsts.COMMAND_SET_APP_MODE -> if (data == null) {
                return
            }

            else -> {}
        }
    }

    companion object {
        private var instance: AppStatusResponser? = null
        fun getInstance(context: Context): AppStatusResponser {
            synchronized(AppStatusResponser::class.java) {
                if (instance == null) {
                    instance = AppStatusResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
