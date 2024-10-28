package com.letianpai.robot.response.mi

import android.content.Context
import com.google.gson.Gson
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.control.callback.GestureCallback
import com.letianpai.robot.control.manager.RobotModeManager
import com.letianpai.robot.control.mode.ViewModeConsts
import com.letianpai.robot.taskservice.utils.RGestureConsts
import com.renhejia.robot.commandlib.consts.MIIotConsts
import com.renhejia.robot.gesturefactory.manager.GestureCenter
import com.renhejia.robot.gesturefactory.manager.GestureResPool
import com.renhejia.robot.letianpaiservice.ILetianpaiService

/**
 * @author liujunbin
 */
class MiIotCmdResponser private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    private val gestureResPool: GestureResPool? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
        //        gestureResPool = new GestureResPool(mContext);
    }

    fun commandDistribute(iLetianpaiService: ILetianpaiService?, command: String?, data: String?) {
        if (!SystemUtil.getRobotActivateStatus()) {
            return
        }
        if (command == null) {
            return
        }

        when (command) {
            MIIotConsts.MI_SAY_HELLO -> sayHello()
            MIIotConsts.MI_FELL_COLD -> fellCold()
            MIIotConsts.MI_FELL_HOT -> fellHot()
            MIIotConsts.MI_COOKING_FINISH -> cookingFinish()
            MIIotConsts.MI_SLEEP_MODE -> miSleepMode()
            MIIotConsts.MI_SMOKE_ALARM -> smokeAlarm()
            MIIotConsts.MI_GAS_ALARM -> gasAlarm()
            MIIotConsts.MI_WATER_ALARM -> waterAlarm()
            else -> {}
        }
    }

    private fun waterAlarm() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miWaterAlarm(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun gasAlarm() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miGasAlarm(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun smokeAlarm() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miSmokeAlarm(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun miSleepMode() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miSleepMode(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun cookingFinish() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miCookingFinish(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun fellHot() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miFeelHot(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun fellCold() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miFeelCold(), RGestureConsts.GESTURE_MI_IOT)
    }

    private fun sayHello() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_MI_IOT_MODE, 1)
        GestureCallback.instance
            .setGestures(GestureCenter.miSayHello(), RGestureConsts.GESTURE_MI_IOT)
    }


    companion object {
        private var instance: MiIotCmdResponser? = null
        @JvmStatic
        fun getInstance(context: Context): MiIotCmdResponser {
            synchronized(MiIotCmdResponser::class.java) {
                if (instance == null) {
                    instance = MiIotCmdResponser(context.applicationContext)
                }
                return instance!!
            }
        }
    }
}
