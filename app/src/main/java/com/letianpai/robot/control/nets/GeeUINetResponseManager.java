package com.letianpai.robot.control.nets;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.letianpai.robot.components.network.nets.GeeUIStatusUploader;
import com.letianpai.robot.components.network.nets.GeeUiNetManager;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.renhejia.robot.commandlib.parser.deviceinfo.DeviceInfo;
import com.renhejia.robot.commandlib.parser.displaymodes.calendar.CalenderInfo;
import com.renhejia.robot.commandlib.parser.displaymodes.countdown.CountDownListInfo;
import com.renhejia.robot.commandlib.parser.displaymodes.fans.FansInfo;
import com.renhejia.robot.commandlib.parser.displaymodes.weather.WeatherInfo;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author liujunbin
 */
public class GeeUINetResponseManager {

    private static GeeUINetResponseManager instance;
    private Context mContext;
    private Gson gson;
    private FansInfo fansInfo;
    private WeatherInfo weatherInfo;
    private CountDownListInfo countDownListInfo;
    private CalenderInfo calenderInfo;
    private static final int GET_DISPLAY_INFO = 2;
    private static final int SHOW_GESTURES_STR = 3;
    private NetRequestHandler handler;

    private GeeUINetResponseManager(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        gson = new Gson();
        handler = new NetRequestHandler(context);
        addNetChangeListeners();
    }

    private void addNetChangeListeners() {
        NetworkChangingUpdateCallback.getInstance().registerChargingStatusUpdateListener(new NetworkChangingUpdateCallback.NetworkChangingUpdateListener() {
            @Override
            public void onNetworkChargingUpdateReceived(int networkType, int networkStatus) {
                Log.i("GeeUINetResponseManager", "onNetworkChargingUpdateReceived--networkType:"+networkType +"--networkStatus::"+networkStatus);
                if (networkType == NetworkChangingUpdateCallback.NETWORK_TYPE_WIFI) {
                    getDisplayInfo();
                    //网络变化，同步数据
                    GeeUIStatusUploader.getInstance(mContext).syncRobotStatus();
                }
            }
        });
    }

    public static GeeUINetResponseManager getInstance(Context context) {
        synchronized (GeeUINetResponseManager.class) {
            if (instance == null) {
                instance = new GeeUINetResponseManager(context.getApplicationContext());
            }
            return instance;
        }

    }

    public void getDisplayInfo() {

        if (WIFIConnectionManager.getInstance(mContext).isNetworkAvailable(mContext) && SystemUtil.hasHardCode()) {

        } else if (WIFIConnectionManager.getInstance(mContext).isNetworkAvailable(mContext) && !SystemUtil.hasHardCode()) {

            getEncodeInfo();
        }
    }

    public void getEncodeInfo() {
        if (SystemUtil.isInChinese()){
            getEncodeInfo(true);
        }else{
            getEncodeInfo(false);
        }
    }

    public void getEncodeInfo(boolean isChinese) {
        if (SystemUtil.hasHardCode()) {
            return;
        }
        GeeUiNetManager.getDeviceInfo(mContext, isChinese, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    DeviceInfo deviceInfo = null;
                    String info = "";
                    if (response != null && response.body() != null) {
                        info = response.body().string();
                    }
                    try{
                        if (info != null) {
                            deviceInfo = new Gson().fromJson(info, DeviceInfo.class);
                            if (deviceInfo != null && !TextUtils.isEmpty(deviceInfo.getData().getClient_id()) && !TextUtils.isEmpty(deviceInfo.getData().getHard_code()) && !TextUtils.isEmpty(deviceInfo.getData().getSn())) {
                                SystemUtil.setHardCode(deviceInfo.getData().getHard_code());
//                            getDisplayInfo();
                                getDisplayInformation(1000);
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     *
     */
    public void updateWeather() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getWeather();
            }
        }).start();
    }

    /**
     *
     */
    private void updateCountDownList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getCountDownList();
            }
        }).start();
    }

    private void getWeather() {
        GeeUiNetManager.getWeatherInfo(mContext, SystemFunctionUtil.isChinese(), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    WeatherInfo weatherInfo = null;
                    String info = "";
                    if (response != null && response.body() != null) {
                        info = response.body().string();

                    }
                    if (info != null) {
                        weatherInfo = gson.fromJson(info, WeatherInfo.class);
                        if (weatherInfo != null) {
                        }

                    }
                }

            }
        });
    }

    private void getCountDownList() {
        GeeUiNetManager.getCountDownList(mContext, SystemFunctionUtil.isChinese(), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {}

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response != null && response.body() != null) {

                    CountDownListInfo countDownListInfo = null;
                    String info = "";

                    if (response != null && response.body() != null) {
                        info = response.body().string();
                    }
                    if (info != null) {
                        countDownListInfo = gson.fromJson(info, CountDownListInfo.class);
                    }
                }
            }
        });
    }

    public void setFansInfo(FansInfo fansInfo) {
        this.fansInfo = fansInfo;
    }

    public void setCalenderInfo(CalenderInfo calenderInfo) {
        this.calenderInfo = calenderInfo;
    }

    public void setCountDownListInfo(CountDownListInfo countDownListInfo) {
        this.countDownListInfo = countDownListInfo;
    }


    public void setWeatherInfo(WeatherInfo weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public CountDownListInfo getCountDownListInfo() {
        if (countDownListInfo == null) {
            getCountDownList();
        }
        return countDownListInfo;
    }

    public WeatherInfo getWeatherInfo() {
        if (weatherInfo == null) {
            getWeather();
        }
        return weatherInfo;
    }

    private void getDisplayInformation (long delay){
        Message message = new Message();
        message.what = GET_DISPLAY_INFO;
        if (delay == 0){
            handler.sendMessage(message);
        }else{
            handler.sendMessageDelayed(message,delay);
        }
    }

    private class NetRequestHandler extends Handler {
        private final WeakReference<Context> context;
        public NetRequestHandler(Context context) {
            this.context = new WeakReference<>(context);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == GET_DISPLAY_INFO) {
                getDisplayInfo();
            } else if (msg.what == SHOW_GESTURES_STR) {
            }
        }
    }
}
