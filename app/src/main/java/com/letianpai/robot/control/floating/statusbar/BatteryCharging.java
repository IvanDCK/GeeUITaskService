package com.letianpai.robot.control.floating.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.letianpai.robot.control.broadcast.battery.ChargingUpdateCallback;
import com.letianpai.robot.taskservice.R;

/**
 * 充电状态
 * @author liujunbin
 */
public class BatteryCharging extends RelativeLayout {

    private View progress;
    private View empty;
    private View redProgress;
    private ImageView ivCharging;
    private static final int SHOW_TIME = 110;
    private static final int SHOW_HI_XIAOLE = 111;
    private static final int UPDATE_ICON = 110;
    private static final int HIDE_TEXT = 112;

    public BatteryCharging(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.robot_charge,this);
        progress = findViewById(R.id.battery_progress);
        empty = findViewById(R.id.battery_empty);
        redProgress = findViewById(R.id.battery_low);
        ivCharging = findViewById(R.id.ivcharging);
        addChargingCallback();
    }


    private void addChargingCallback() {
        ChargingUpdateCallback.getInstance().registerChargingStatusUpdateListener(new ChargingUpdateCallback.ChargingUpdateListener() {
            @Override
            public void onChargingUpdateReceived(boolean changingStatus, int percent) {
                setBatteryLevel(percent);
                if (changingStatus){
                    ivCharging.setVisibility(View.VISIBLE);
                }else{
                    ivCharging.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onChargingUpdateReceived(boolean changingStatus, int percent, int chargePlug) {}
        });
    }

    public void setBatteryLevel(float batteryLevel) {
        ivCharging.setVisibility(View.VISIBLE);
        redProgress.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT);
//        params.weight = batteryLevel;//在此处设置weight
        if (batteryLevel <20){
            batteryLevel = 20;
        }
//        params.weight = batteryLevel -20;//在此处设置weight
        params.weight = (batteryLevel -20) * 1.25F;//在此处设置weight

//        float weight1 = params.weight;
//        float weight2 = (batteryLevel -20) * 1.25F;
//        Log.e("letianpai_weight","weight1: "+ weight1);
//        Log.e("letianpai_weight","weight2: "+ weight2);

        progress.setLayoutParams(params);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT);
//        params1.weight = 100 - batteryLevel;//在此处设置weight
        params1.weight = 100 - params.weight;//在此处设置weight
        empty.setLayoutParams(params1);

    }

    public void setBatteryLow(float batteryLevel) {
        ivCharging.setVisibility(View.GONE);
        redProgress.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT);
//        params.weight = batteryLevel;//在此处设置weight
        params.weight = (int)((batteryLevel-20) *1.25);//在此处设置weight
        redProgress.setLayoutParams(params);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT);
//        params1.weight = 100 - batteryLevel;//在此处设置weight
        params1.weight = 100 - params.weight;//在此处设置weight
        empty.setLayoutParams(params1);

    }

    public BatteryCharging(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BatteryCharging(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

}
