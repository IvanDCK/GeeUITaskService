package com.letianpai.robot.control.floating.statusbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback
import com.letianpai.robot.taskservice.R

/**
 * 充电状态
 * @author liujunbin
 */
class BatteryCharging : RelativeLayout {
    private var progress: View? = null
    private var empty: View? = null
    private var redProgress: View? = null
    private var ivCharging: ImageView? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        inflate(context, R.layout.robot_charge, this)
        progress = findViewById(R.id.battery_progress)
        empty = findViewById(R.id.battery_empty)
        redProgress = findViewById(R.id.battery_low)
        ivCharging = findViewById(R.id.ivcharging)
        addChargingCallback()
    }


    private fun addChargingCallback() {
        ChargingUpdateCallback.instance.registerChargingStatusUpdateListener(object :
            ChargingUpdateCallback.ChargingUpdateListener {
            override fun onChargingUpdateReceived(changingStatus: Boolean, percent: Int) {
                setBatteryLevel(percent.toFloat())
                if (changingStatus) {
                    ivCharging!!.setVisibility(VISIBLE)
                } else {
                    ivCharging!!.setVisibility(INVISIBLE)
                }
            }

            override fun onChargingUpdateReceived(
                changingStatus: Boolean,
                percent: Int,
                chargePlug: Int
            ) {
            }
        })
    }

    fun setBatteryLevel(batteryLevel: Float) {
        var newBatteryLevel: Float = batteryLevel
        ivCharging!!.visibility = VISIBLE
        redProgress!!.visibility = GONE
        progress!!.visibility = VISIBLE
        val params: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT)
        //        params.weight = batteryLevel;//Set the weight here
        if (newBatteryLevel < 20) {
            newBatteryLevel = 20f
        }
        //        params.weight = batteryLevel -20;//Set the weight here
        params.weight = (newBatteryLevel - 20) * 1.25f //Set the weight here

        //        float weight1 = params.weight;
//        float weight2 = (batteryLevel -20) * 1.25F;
//        Log.e("letianpai_weight","weight1: "+ weight1);
//        Log.e("letianpai_weight","weight2: "+ weight2);
        progress!!.setLayoutParams(params)

        val params1: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT)
        //        params1.weight = 100 - batteryLevel;//Set the weight here
        params1.weight = 100 - params.weight //Set the weight here
        empty!!.setLayoutParams(params1)
    }

    fun setBatteryLow(batteryLevel: Float) {
        ivCharging!!.setVisibility(GONE)
        redProgress!!.setVisibility(VISIBLE)
        progress!!.setVisibility(GONE)

        val params: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT)
        //        params.weight = batteryLevel;//Set the weight here
        params.weight = ((batteryLevel - 20) * 1.25).toInt().toFloat() //Set the weight here
        redProgress!!.setLayoutParams(params)

        val params1: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT)
        //        params1.weight = 100 - batteryLevel;//Set the weight here
        params1.weight = 100 - params.weight //Set the weight here
        empty!!.setLayoutParams(params1)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    companion object {
        private const val SHOW_TIME: Int = 110
        private const val SHOW_HI_XIAOLE: Int = 111
        private const val UPDATE_ICON: Int = 110
        private const val HIDE_TEXT: Int = 112
    }
}
