package com.letianpai.robot.response.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.letianpai.robot.components.consts.AppStoreUpdateConsts;
import com.letianpai.robot.components.network.nets.AppStoreCmdConsts;
import com.letianpai.robot.components.network.nets.GeeUiNetManager;
import com.letianpai.robot.components.network.system.SystemUtil;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.callback.RobotCommandWordsCallback;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.storage.SPUtils;
import com.letianpai.robot.control.system.LetianpaiFunctionUtil;
import com.letianpai.robot.control.system.SystemFunctionUtil;
import com.letianpai.robot.response.RobotFuncResponseManager;
import com.renhejia.robot.commandlib.consts.AppCmdConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.parser.config.AppShow;
import com.renhejia.robot.commandlib.parser.config.AppsShowList;
import com.renhejia.robot.commandlib.parser.config.AppsShowListModel;
import com.renhejia.robot.commandlib.parser.config.UserAppsConfig;
import com.renhejia.robot.commandlib.parser.config.UserAppsConfigModel;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @author liujunbin
 */
public class AppCmdResponser {
    private static String TAG = "AppCmdResponser";
    private static AppCmdResponser instance;
    private Context mContext;

    private Timer mTimer = null;
    private boolean isTimerCancel = false;

    private boolean isAutoSwitchApp = true;

    private List<Map<String, String>> installedApps;

    private UserAppsConfigModel userAppsConfigModel;

    //自动切换的APP
    private AppsShowListModel appsShowListModel;

    public AppsShowListModel getAppsShowListModel() {
        return appsShowListModel;
    }

    public void setAppsShowListModel(AppsShowListModel appsShowListModel) {
        this.appsShowListModel = appsShowListModel;
    }

    public UserAppsConfigModel getUserAppsConfigModel() {
        return userAppsConfigModel;
    }

    public void setUserAppsConfigModel(UserAppsConfigModel userAppsConfigModel) {
        this.userAppsConfigModel = userAppsConfigModel;
    }

    public boolean isAutoSwitchApp() {
        return isAutoSwitchApp;
    }

    //切换机器人APP
    public void setAutoSwitchApp(boolean autoSwitchApp) {
        isAutoSwitchApp = autoSwitchApp;
        if (autoSwitchApp) {
            //请求网络
            requestAppsShowConfig();
            if (mTimer != null) {
                mTimer.cancel();
            }
            mTimer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    GeeUILogUtils.logi(TAG, "run------appsShowListModel::" + appsShowListModel + "--isTimerCancel::"+isTimerCancel);
                    if (isTimerCancel) return;
                    if (appsShowListModel != null) {
                        AppsShowList appsShowList = appsShowListModel.getData();
                        if (appsShowList != null) {
                            List<AppShow> appShowList = appsShowList.getAppShowList();
                            if (appShowList != null && appShowList.size() > 0) {
                                AppShow appShow = appShowList.get(0);
                                GeeUILogUtils.logi(TAG, "run------appShow::" + appShow);

                                if (appShow != null) {
                                    String openPath = appShow.getOpenPath();
                                    String packageName = appShow.getPackageName();
                                    String appTag = appShow.getAppTag();
                                    GeeUILogUtils.logi(TAG, "openPath:" + openPath + "----packageName:" + packageName + "---appTag:" + appTag);
                                    if (SystemFunctionUtil.isAppInstalled(mContext, packageName)) {
                                        if (packageName.equals("com.geeui.face")){
                                            RobotFuncResponseManager.getInstance(mContext).openRobotMode("");
                                        }else{
                                            //打开应用
                                            RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_OTHER);
                                            LetianpaiFunctionUtil.openApp(mContext, packageName, openPath, appTag);
                                        }
                                    }
                                    appShowList.remove(appShow);
                                }
                            }else {
                                requestAppsShowConfig();
                            }
                        }
                    }
                }
            };
            isTimerCancel = false;
            mTimer.schedule(task, 0, 3 * 60 * 1000);  // 3分钟 = 3 * 60 * 1000 毫秒
        }else{
            if (mTimer != null) {
                isTimerCancel = true;
                mTimer.cancel();
                mTimer = null;
            }
        }
    }

    private final Callback appsConfigCallback = new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            e.printStackTrace();
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws
                IOException {
            if (response.body() != null) {
                String info = response.body().string();
                GeeUILogUtils.logi("------", "info--" + info);
                try {
                    userAppsConfigModel = new Gson().fromJson(info, UserAppsConfigModel.class);
                    //遍历安装的APP，然后判断是否需要拉起
                    Iterator<Map<String, String>> iterator = installedApps.iterator();
                    while (iterator.hasNext()) {
                        Map<String, String> map = iterator.next();
                        String packageName = map.get("packageName");
                        List<UserAppsConfig> list = userAppsConfigModel.getData().stream().filter(item -> item.appPackageName.equals(packageName)).collect(Collectors.toList());
                        UserAppsConfig userAppsConfig = list.stream().findFirst().get();
                        if (userAppsConfig.isRestart == 1 && userAppsConfig.openType == 2) {
                            //需要判断海外还是国内
                            String pro = SystemUtil.get(SystemUtil.REGION_LANGUAGE, "zh");
                            if (userAppsConfig.appPackageName.equals("com.geeui.lex") && !"en".equals(pro)) {
                                break;
                            }
                            if (userAppsConfig.appPackageName.equals("com.rhj.speech") && !"zh".equals(pro)) {
                                break;
                            }

                            //需要重启service
                            Intent intent = new Intent();
                            ComponentName cn = new ComponentName(userAppsConfig.appPackageName, userAppsConfig.openContent);
                            intent.setComponent(cn);
                            mContext.startService(intent);
                            //启动一个就删除一个
                            iterator.remove();
                        }
                        GeeUILogUtils.logi("------", "userAppsConfig:appPackageName:--" + userAppsConfig.appPackageName + "--userAppsConfig:openContent:" + userAppsConfig.openContent);
                    }
                    //重启完成之后，清空
                    installedApps.clear();

                    //----
                    // for ( UserAppsConfig config:userAppsConfigModel.getData()) {
                    //     Log.i("------","userAppsConfig:appPackageName:--"+config.appPackageName +"--userAppsConfig:openContent:"+config.openContent);
                    //     Log.i("------","userAppsConfig:appName:--"+config.appName );
                    // }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final Callback appsShowCallback = new Callback() {

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            e.printStackTrace();
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.body() != null) {
                String info = response.body().string();
                GeeUILogUtils.logi("------", "appsShowCallback info--" + info);
                try {
                    appsShowListModel = new Gson().fromJson(info, AppsShowListModel.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private AppCmdResponser(Context context) {
        this.mContext = context;
        init();
    }

    public static AppCmdResponser getInstance(Context context) {
        synchronized (AppCmdResponser.class) {
            if (instance == null) {
                instance = new AppCmdResponser(context.getApplicationContext());
            }
            return instance;
        }
    }

    private void init() {
        installedApps = new ArrayList<>();
        double isAutoShow = SPUtils.getInstance(mContext).getDouble("isAutoShow");
        //等等于1，自动切换
        setAutoSwitchApp(isAutoShow == 1);
    }

    public void commandDistribute(ILetianpaiService iLetianpaiService, String command, String data) {
        GeeUILogUtils.logi(TAG, "commandDistribute: ============ D ============ command: " + command + "---data:" + data);
        if (command == null) {
            return;
        }

        switch (command) {
            case AppStoreCmdConsts.COMMAND_INSTALL_APP_STORE_SUCCESS:
                // 请求已安装APP的数据,安装成功之后，当前安装的包名会发送过来
                //{packageName:'com.letianpai.robot.reminder', displayName:'GeeUIReminder'}
                JsonObject jsonObject = new Gson().fromJson(data, JsonObject.class);
                String packageName = jsonObject.get("packageName").getAsString();
                String displayName = jsonObject.get("displayName").getAsString();
                List<Map<String, String>> tempList = installedApps.stream().filter(item -> item.get("packageName").equals(packageName)).collect(Collectors.toList());
                if (tempList.isEmpty()) {
                    Map<String, String> installedApp = new HashMap<>();
                    installedApp.put("packageName", packageName);
                    installedApp.put("displayName", displayName);
                    installedApps.add(installedApp);
                }

                getUserAppsConfig();
                break;

            case RobotRemoteConsts.COMMAND_TYPE_MOTION:
                break;

            case RobotRemoteConsts.COMMAND_SET_APP_MODE:
                if (data == null) {
                    return;
                }
                if (data.equals(RobotRemoteConsts.COMMAND_SHOW_CHARGING)) {
                    RobotCommandWordsCallback.getInstance().showBattery(true);
                } else if (data.equals(RobotRemoteConsts.COMMAND_HIDE_CHARGING)) {
                    RobotCommandWordsCallback.getInstance().showBattery(false);
                }
                break;

            case AppCmdConsts.COMMAND_TYPE_TAKE_PHOTO:
                RobotModeManager.getInstance(mContext).switchRobotMode(RobotModeManager.VM_TAKE_PHOTO, Integer.valueOf(data));
                break;

//            case AppCmdConsts.COMMAND_TYPE_HAND_REG:
//                if (data.equals(AppCmdConsts.COMMAND_TYPE_HAND_REG_IN)) {
//                    RobotModeManager.getInstance(mContext).switchRobotMode(RobotModeManager.VM_HAND_REG_MODE, 1);
//                } else if (data.equals(AppCmdConsts.COMMAND_TYPE_HAND_REG_OUT)) {
//                    try {
//                        iLetianpaiService.setRobotStatusCmd("com.ltp.ident",AppCmdConsts.COMMAND_VALUE_EXIT);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                    RobotModeManager.getInstance(mContext).switchRobotMode(RobotModeManager.VM_HAND_REG_MODE, 0);
//                }else{
////                    GestureCallback.getInstance().setGestures(GestureCenter.getHandGestureWithID(data), AppCmdConsts.COMMAND_TYPE_HAND_ID);
//                    GestureCallback.getInstance().setGestures(GestureCenter.getHandGestureWithID(data), RGestureConsts.GESTURE_COMMAND_HAND_ID);
//                }
//
//                break;

            case RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN:
                SystemFunctionUtil.shutdownRobot(mContext);
                break;

            case RobotRemoteConsts.COMMAND_TYPE_SHUTDOWN_STEERING_ENGINE:
                if (!RobotModeManager.getInstance(mContext).isPowerOnChargingMode()) {
                    LetianpaiFunctionUtil.controlSteeringEngine(mContext, false, false);
                }
                break;

            case RobotRemoteConsts.COMMAND_TYPE_POWER_ON_CHARGING:
                responsePowerOnCharging();
                break;

            case AppStoreUpdateConsts.COMMAND_INSTALL_APP_STORE:
//                updateAppStore(data);
                GeeUILogUtils.logi("letianpai_install_app_store","letianpai_install_app_store: "+ AppStoreUpdateConsts.COMMAND_INSTALL_APP_STORE + " =============== 1 =============");
                startAppStoreService();
                break;
            default:
        }

    }


    private void startAppStoreService() {
        GeeUILogUtils.logi("letianpai_install_app_store","letianpai_install_app_store: "+ AppStoreUpdateConsts.COMMAND_INSTALL_APP_STORE + " =============== 2 =============");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10 * 1000);
                    Intent intent = new Intent();
                    ComponentName cn = new ComponentName("com.letianpai.robot.appstore", "com.letianpai.robot.appstore.service.AppStoreService");
                    intent.setComponent(cn);
                    mContext.startService(intent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }



    //获取用户安装的APP

    public void getUserAppsConfig() {
        GeeUiNetManager.getUserAppsConfig(mContext, appsConfigCallback);
    }


    //获取需要自动切换的APP
    public void requestAppsShowConfig() {
        GeeUiNetManager.getAppsShowConfig(mContext, appsShowCallback);
    }


    /**
     * 响应开机充电的动作
     */
    private void responsePowerOnCharging() {
        RobotModeManager.getInstance(mContext).switchRobotMode(ViewModeConsts.VM_POWER_ON_CHARGING, 1);
    }
}
