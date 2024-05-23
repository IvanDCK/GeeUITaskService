package com.letianpai.robot.control.floating.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.taskservice.R;

/**
 * @author liujunbin
 */
public class NoWiFiNoticeView extends LinearLayout {
    private TextView tvReconnectWifi;
    private Context mContext;

    public NoWiFiNoticeView(Context context) {
        super(context);
        init(context);
    }

    public NoWiFiNoticeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NoWiFiNoticeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public NoWiFiNoticeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private ImageView ivCharging;

    private void init(Context context) {
        this.mContext = context;
        inflate(context, R.layout.robot_wifi_notice,this);
        tvReconnectWifi = findViewById(R.id.tvReconnectWifi);
        tvReconnectWifi.setText(mContext.getResources().getString(R.string.cmd_reconnect_wifi));
    }
}
