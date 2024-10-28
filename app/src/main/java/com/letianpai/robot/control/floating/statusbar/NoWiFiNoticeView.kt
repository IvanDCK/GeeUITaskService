package com.letianpai.robot.control.floating.statusbar

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.letianpai.robot.taskservice.R

/**
 * @author liujunbin
 */
class NoWiFiNoticeView : LinearLayout {
    private var tvReconnectWifi: TextView? = null
    private var mContext: Context? = null

    constructor(context: Context) : super(context) {
        init(context)
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

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    private val ivCharging: ImageView? = null

    private fun init(context: Context) {
        this.mContext = context
        inflate(context, R.layout.robot_wifi_notice, this)
        tvReconnectWifi = findViewById(R.id.tvReconnectWifi)
        tvReconnectWifi!!.text = mContext!!.resources.getString(R.string.cmd_reconnect_wifi)
    }
}
