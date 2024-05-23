package com.letianpai.robot.control.system;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_AUDIO_WAKEUP_MODE;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_AUDIO_WAKEUP_MODE_DEFAULT;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_DEMOSTRATE_MODE;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_STATIC_MODE;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_TRTC_MONITOR;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_TRTC_TRANSFORM;
import static com.letianpai.robot.control.mode.ViewModeConsts.VM_UNBIND_DEVICE;
import static com.renhejia.robot.commandlib.consts.RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TAKE_PHOTO;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RecoverySystem;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.letianpai.robot.components.network.nets.GeeUiNetManager;
import com.letianpai.robot.components.utils.GeeUILogUtils;
import com.letianpai.robot.control.callback.ControlSteeringEngineCallback;
import com.letianpai.robot.control.callback.GestureCallback;
import com.letianpai.robot.control.callback.RobotCommandWordsCallback;
import com.letianpai.robot.control.callback.TemperatureUpdateCallback;
import com.letianpai.robot.control.floating.FloatingViewService;
import com.letianpai.robot.control.manager.L81AppListManager;
import com.letianpai.robot.control.manager.RobotModeManager;
import com.letianpai.robot.control.mode.ViewModeConsts;
import com.letianpai.robot.control.mode.callback.ModeChangeCallback;
import com.letianpai.robot.control.storage.RobotConfigManager;
import com.letianpai.robot.response.RobotFuncResponseManager;
import com.letianpai.robot.response.app.AppCmdResponser;
import com.letianpai.robot.response.robotStatus.RobotStatusResponser;
import com.letianpai.robot.taskservice.utils.LTPConfigConsts;
import com.letianpai.robot.taskservice.utils.RGestureConsts;
import com.renhejia.robot.commandlib.consts.PackageConsts;
import com.renhejia.robot.commandlib.consts.RobotRemoteConsts;
import com.renhejia.robot.commandlib.consts.SpeechConst;
import com.renhejia.robot.commandlib.parser.config.UserAppsConfig;
import com.renhejia.robot.commandlib.parser.config.UserAppsConfigModel;
import com.renhejia.robot.gesturefactory.manager.GestureCenter;
import com.renhejia.robot.letianpaiservice.ILetianpaiService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * @author liujunbin
 */
public class LetianpaiFunctionUtil {

    private static String TAG = "LetianpaiFunctionUtil";
    private static final String SPLIT = "____";

    //记录当前module，如果相同就不用切换
    public static String currentPackageName = "";

    public static List<String> packageNames = new ArrayList<>(4);

    public static void showFloatingView(Context context) {
        Intent intent = new Intent(context, FloatingViewService.class);
        context.startService(intent);
    }


    /**
     * 打开天气
     *
     * @param context
     */
    public static boolean openWeather(Context context) {
        return openApp(context, PackageConsts.WEATHER_PACKAGE_NAME, PackageConsts.ACTIVITY_PACKAGE_NAME, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WEATHER);
    }

    /**
     * 打开自动充电
     *
     * @param context
     */
    public static boolean openAutoCharging(Context context) {
        if (SystemFunctionUtil.isAppInstalled(context, PackageConsts.PACKAGE_NAME_AUTO_CHARGING)) {
            LetianpaiFunctionUtil.controlSteeringEngine(context, true, true);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(PackageConsts.PACKAGE_NAME_AUTO_CHARGING, PackageConsts.CLASS_NAME_AUTO_CHARGING));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
            RobotCommandWordsCallback.getInstance().showBattery(false);
            ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
            return true;
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText();
            return false;
        }
    }

    public static void startAlarmService(Context context, String data) {
        Intent intent = new Intent();
//        ComponentName cn = new ComponentName("com.letianpai.robot.alarm", "com.letianpai.robot.alarm.service.AlarmService");
        ComponentName cn = new ComponentName("com.letianpai.robot.alarmnotice", "com.letianpai.robot.alarmnotice.service.AlarmService");
        intent.setComponent(cn);
        intent.putExtra(SpeechConst.EXTRA_ALARM, data);
        context.startService(intent);
    }

    public static void startAlarmService(Context context, String command, String data) {
        Intent intent = new Intent();
//        ComponentName cn = new ComponentName("com.letianpai.robot.alarm", "com.letianpai.robot.alarm.service.AlarmService");
        ComponentName cn = new ComponentName("com.letianpai.robot.alarmnotice", "com.letianpai.robot.alarmnotice.service.AlarmService");
        intent.setComponent(cn);
        if ((!TextUtils.isEmpty(command)) && (!TextUtils.isEmpty(data))) {
            intent.putExtra(SpeechConst.EXTRA_SPEECH_ALARM, command + SpeechConst.COMMAND_SPEECH_SPLIT + data);
        }
        context.startService(intent);
    }

    public static void startAppStoreService(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent();
                    ComponentName cn = new ComponentName("com.letianpai.robot.appstore", "com.letianpai.robot.appstore.service.AppStoreService");
                    intent.setComponent(cn);
                    context.startService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    /**
     * 打开新闻
     *
     * @param context
     */
    public static boolean openNews(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_NEWS, PackageConsts.ACTIVITY_NAME_NEWS, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_NEWS);
    }

    /**
     * 打开跑马灯
     *
     * @param context
     */
    public static boolean openLamp(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_LAMP, PackageConsts.PACKAGE_NAME_LAMP_ACTIVITY, RobotRemoteConsts.COMMAND_VALUE_CHANGE_LAMP);
    }


    /**
     * 打开自定义桌面
     *
     * @param context
     */
    public static boolean openCustom(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_CUSTOM, PackageConsts.CLASS_NAME_CUSTOM, RobotRemoteConsts.COMMAND_VALUE_CHANGE_CUSTOM, true);
    }


    /**
     * 打开米家绑定app
     *
     * @param context
     */
    public static boolean openBindMijia(Context context) {
        return openApp(context, "com.geeui.miiot", "com.geeui.miiot.MiIoTActivity", "");
    }

    /**
     * 打开纪念日
     *
     * @param context
     */
    public static boolean openCommemoration(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_COMMEMORATION, PackageConsts.ACTIVITY_NAME_COMMEMORATION, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_COMMEMORATION);
    }

    /**
     * 打开股票
     *
     * @param context
     */
    public static boolean openStock(Context context) {
        return openApp(context, PackageConsts.STOCK_PACKAGE_NAME, PackageConsts.PACKAGE_NAME_STOCK_ACTIVITY, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_STOCK);
    }

    /**
     * 打开单词
     *
     * @param context
     */
    public static boolean openWord(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_WORDS, PackageConsts.PACKAGE_NAME_WORDS_ACTIVITY, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_WORD);
    }

    /**
     * 打开粉丝
     *
     * @param context
     */
    public static boolean openFans(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_FANS, PackageConsts.ACTIVITY_NAME_FANS, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_FANS);
    }

    /**
     * 打开表情
     *
     * @param context
     */
    public static boolean openExpressionApp(Context context) {
        return openApp(context, "com.letianpai.robot.expression", "com.letianpai.robot.expression.ui.activity.MainActivity", "com.letianpai.robot.expression");
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    public static boolean openEventCountdown(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_COUNT_DOWN, PackageConsts.ACTIVITY_NAME_COUNT_DOWN, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_EVENT);
    }

    public static void openUniversalApp(Context context, String name, ILetianpaiService iLetianpaiService) {
        GeeUILogUtils.logi("----" + TAG, "openUniversalApp::name:::" + name);

        if (!TextUtils.isEmpty(name) && name.contains(SPLIT)) {
            String[] appInfo = name.split(SPLIT);
            if (appInfo != null && appInfo.length >= 2) {
                LetianpaiFunctionUtil.openApp(context, appInfo[0], appInfo[1], appInfo[0]);
            }
        } else {
            //判断如果这个包名不在我们的APP中，第三方安装应用就本地打开
            if (name.contains(".") && !L81AppListManager.getInstance(context).isInThePackageList(name)) {
                try {
                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(name);
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        killProcessOfPackageName(context, name);
                    } else {
                        GeeUILogUtils.logi("----" + TAG, "openUniversalApp::无法打开该应用");
                    }
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    GeeUILogUtils.logi("----", Log.getStackTraceString(e));
                }
                return;
            }

            UserAppsConfigModel userAppsConfigModel = AppCmdResponser.getInstance(context).getUserAppsConfigModel();
            GeeUILogUtils.logi("----" + TAG, "openUniversalApp:userAppsConfigModel.getData().isEmpty():" + userAppsConfigModel.getData().isEmpty());

            if (userAppsConfigModel != null && !userAppsConfigModel.getData().isEmpty()) {
                List<UserAppsConfig> list;
                if (name.contains(".")) {//包名过滤
                    list = userAppsConfigModel.getData().stream().filter(item -> item.appPackageName.toLowerCase().equals(name.toLowerCase())).collect(Collectors.toList());
                } else {
                    list = userAppsConfigModel.getData().stream().filter(item -> item.appName.toLowerCase().equals(name.toLowerCase())).collect(Collectors.toList());
                }
                if (!list.isEmpty()) {
                    UserAppsConfig userAppsConfig = list.stream().findFirst().get();
                    GeeUILogUtils.logi("----" + TAG, "open others app.appName::" + userAppsConfig.appName + "--appPackageName::" + userAppsConfig.appPackageName + "--openContent::" + userAppsConfig.openContent);

                    if (SystemFunctionUtil.isAppInstalled(context, userAppsConfig.appPackageName)) {
                        if (userAppsConfig.appPackageName.equals("com.letianpai.robot.expression")) {
                            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_EXPRESSION);
                        } else {
                            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_STATIC_MODE, ViewModeConsts.APP_MODE_OTHER);
                        }
                        RobotFuncResponseManager.stopRobot(iLetianpaiService);
                        String moduleName = "";
                        if (userAppsConfig.isShowReport == 1) {
                            moduleName = userAppsConfig.appPackageName;
                        }
                        LetianpaiFunctionUtil.openApp(context, userAppsConfig.appPackageName, userAppsConfig.openContent, moduleName);
                        return;
                    }
                } else {
                    AppCmdResponser.getInstance(context).getUserAppsConfig();
                }
                RobotModeManager.getInstance(context).ttsUninstallAppText();
            } else {
                AppCmdResponser.getInstance(context).getUserAppsConfig();
            }
        }


    }


    public static boolean openTakePhoto(Context context) {
        if (SystemFunctionUtil.isAppInstalled(context, PackageConsts.TAKEPHOTO_PACKAGE_NAME)) {
            openRobotMode(context, COMMAND_VALUE_CHANGE_MODE_TAKE_PHOTO, "h0182");
            controlSteeringEngine(context, true, true);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(PackageConsts.TAKEPHOTO_PACKAGE_NAME, PackageConsts.TAKEPHOTO_PACKAGE_CLASS_NAME));
            context.startService(intent);
            return true;
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText();
            return false;
        }
    }

    public static void resetOtherAppStatus(Context context) {
        changeToStand(context);
    }

    public static boolean openApp(Context context, String packageName, String activityName, String moduleName) {
        return openApp(context, packageName, activityName, moduleName, true);
    }

    public static boolean openApp(Context context, String packageName, String activityName, String moduleName, boolean isShowBattery) {
        if (SystemFunctionUtil.isAppInstalled(context, packageName)) {
            changeToStand(context);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, activityName));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);

            //如果当前APP和上一个不相等的时候，或者是米家的时候，就打开。
            if (!currentPackageName.equals(packageName) || activityName.contains("MiIoTActivity")) {
                try {
                    context.startActivity(intent);
                    //判断是否有需要杀掉的APP
                    killProcessOfPackageName(context, packageName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                //获取前台应用程序包名是否是packageName，如果不是，则打开
                GeeUILogUtils.logi("letianpai_test", "getTopPackageName: " + getTopPackageName(context) + "---packageName::" + packageName);
                if (!getTopPackageName(context).equals(packageName)) {
                    try {
                        context.startActivity(intent);
                        //判断是否有需要杀掉的APP
                        killProcessOfPackageName(context, packageName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            // 记录当前的模块
            currentPackageName = packageName;

            RobotCommandWordsCallback.getInstance().showBattery(isShowBattery);
            if (!TextUtils.isEmpty(moduleName)) {
                //上报
                updateModeStatusOnServer(context, moduleName);
                //记录当前打开的APP,目的是为了上报顺序保持
                List<String> modeList = RobotModeManager.getInstance(context).modeList;
                if (modeList.contains(moduleName)) {
                    modeList.remove(moduleName);
                }
                modeList.add(moduleName);
            }
            ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
            // ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_IDENT_PROGRESS, 1);
            return true;
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText();
            return false;
        }
    }

    //杀掉APP
    public static void killProcessOfPackageName(Context context, String packageName) {
        if (packageNames.contains(packageName)) {
            packageNames.remove(packageName);
        }
        //应用中心不添加
        if (!packageName.equals("com.letianpai.robot.desktop")
                && !packageName.equals("com.letianpai.robot.downloader")
                && !packageName.equals("com.letianpai.robot.appstore")) {
            packageNames.add(packageName);
        }

        //如果第二个应用不在我们自己的应用列表，就杀掉
        for (int i = 0; i < packageNames.size() - 1; i++) {
            String packageN = packageNames.get(i);
            GeeUILogUtils.logi("-----", "--for-- packageName::" + packageN);
            if (!L81AppListManager.getInstance(context).isInThePackageList(packageN)) {
                killProcessOfPackageName2(context, packageN);
            }
        }

        GeeUILogUtils.logi("-----", "packageName::" + packageName);
        GeeUILogUtils.logi("-----", "packageNames.size()::" + packageNames.size());
        if (packageNames.size() >= 3) {
            String needKillPackageName = packageNames.get(0);
            killProcessOfPackageName2(context, needKillPackageName);
        }
        //test 遍历数组看看有几个
        // Log.d("---++++++++---", packageNames.size()+"");
        //
        // for (int i = 0; i < packageNames.size();i++){
        //     Log.d("---++++++++---", packageNames.get(i));
        // }
    }

    private static void killProcessOfPackageName2(Context context, String currentPackageName) {
        ActivityManager mActivityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        Method method = null;
        try {
            method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(mActivityManager, currentPackageName);
            //移除掉第0个
            packageNames.remove(currentPackageName);
            GeeUILogUtils.logi("---++++++++---", "kill process：：" + currentPackageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateModeStatusOnServer(Context context, String modeName) {
        if (modeName.isEmpty()) return;
        GeeUiNetManager.moduleChange(context, modeName, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response == null || response.body() == null) {
                    return;
                }
                String resData = response.body().string();
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(resData);
                    int code = jsonObject.getInt("code");
                    if (code == 0) {
                        long time = System.currentTimeMillis();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static void resetRobot(Context context) {
        GeeUILogUtils.logi("--" + TAG, "---resetRobot---- ");
        GeeUiNetManager.robotReset(context, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                GeeUILogUtils.logi("--" + TAG, "----resetRobot0000_onFailure----");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response == null || response.body() == null) {
                    return;
                }
                String resData = response.body().string();
                GeeUILogUtils.logi("--" + TAG, "resetRobot0000_resData : " + resData);
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(resData);
                    int code = jsonObject.getInt("code");
                    if (code == 0) {
                        //舵机卸力
                        changeToStand(context);
                        restoreRobot(context);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * "
     * 启动机器人模式
     *
     * @param context
     * @param mode    启动模式
     * @param face    表情标签
     */
    public static void openRobotFace(Context context, String mode, String face) {
        GeeUILogUtils.logi("---" + TAG, "----openRobotFace----mode::" + mode + "----face::" + mode);
        controlSteeringEngine(context, true, true);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.geeui.face", "com.geeui.face.MainActivity"));
        intent.putExtra("face", face);
        intent.putExtra("mode", mode);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//        intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP );
        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
        RobotCommandWordsCallback.getInstance().showBattery(false);
        ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_SPEECH_SERVICE, 1);
    }

    public static void startGeeUIOtaService(Context context) {
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.letianpai.otaservice", "com.letianpai.otaservice.ota.GeeUpdateService");
        intent.setComponent(cn);
        context.startService(intent);
    }

    /**
     * 启动机器人模式
     *
     * @param context
     * @param mode    启动模式
     * @param face    表情标签
     */
    public static void openRobotMode(Context context, String mode, String face) {
        if (!LetianpaiFunctionUtil.isRobotOnTheTop(context)) {
            LetianpaiFunctionUtil.openRobotFace(context, mode, face);
        } else {
            if (mode.equals(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_ROBOT)) {
                ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_AUTO_NEW_PLAY_MODE, 1);

            } else if (mode.equals(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_MONITOR)) {
                ModeChangeCallback.getInstance().setModeChange(VM_TRTC_MONITOR, 1);

            } else if (mode.equals(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_TRTC_TRANSFORM)) {
                ModeChangeCallback.getInstance().setModeChange(VM_TRTC_TRANSFORM, 1);
            } else if (mode.equals(RobotRemoteConsts.COMMAND_VALUE_CHANGE_MODE_DEMO)) {
                ModeChangeCallback.getInstance().setModeChange(VM_DEMOSTRATE_MODE, 1);
            }
        }
    }

    public static final String START_FROM_START_APP = "startApp";
    public static final String START_FROM_SLEEP = "sleep";
    public static final String START_FROM = "from";
    public static final String START_FROM_DEEP_SLEEP = "deep_sleep";
    public static final String START_FROM_NO_NETWORK = "no_network";


    /**
     * 开启倒计时
     *
     * @param context
     */
    public static void openTime(Context context) {
        openTimeView(context, START_FROM_START_APP);
    }


    /**
     * @param context
     */
    public static boolean openMessage(Context context) {
        return openApp(context, PackageConsts.PACKAGE_NAME_MESSAGE, PackageConsts.ACTIVITY_NAME_MESSAGE, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_MESSAGE);
    }


    /**
     * 开启倒计时
     *
     * @param context
     */
    public static void openTimeViewForSleep(Context context) {
        openTimeView(context, START_FROM_SLEEP);
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    public static void openTimeViewForDeepSleep(Context context) {
        openTimeView(context, START_FROM_DEEP_SLEEP);
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    public static void openTimeView(Context context, String from) {

        GeeUILogUtils.logi(TAG, "openTimeView---from::" + from);
        //将这个值重置，不然切换其他APP会有问题
        currentPackageName = "";
        changeToStand(context);
        String packageN = "";
        Intent intent = new Intent();
        if (from.equals(START_FROM_START_APP)) {
            packageN = "com.letianpai.robot.time";
            RobotCommandWordsCallback.getInstance().showBattery(true);
            intent.setComponent(new ComponentName(packageN, "com.letianpai.robot.time.ui.activity.MainActivity"));
        } else {
            packageN = "com.renhejia.robot.launcher";
            RobotCommandWordsCallback.getInstance().showBattery(false);
            intent.setComponent(new ComponentName(packageN, "com.renhejia.robot.launcher.main.activity.LeTianPaiMainActivity"));
        }
        intent.putExtra(START_FROM, from);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
        killProcessOfPackageName(context, packageN);

        updateModeStatusOnServer(context, RobotRemoteConsts.COMMAND_VALUE_CHANGE_SHOW_MODULE_TIME);
        ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ALL_INVALID_SERVICE, 1);
    }

    public static void getTopActivity(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        if (runningTasks != null && runningTasks.size() > 0) {
            ActivityManager.RunningTaskInfo taskInfo = runningTasks.get(0);
            ComponentName componentName = taskInfo.topActivity;
            GeeUILogUtils.logi("letianpai_test", "componentName.getPackageName(): " + componentName.getPackageName());
            GeeUILogUtils.logi("letianpai_test", "componecomponentName.getClassName(): " + componentName.getClassName());

//            if (componentName.getPackageName().equals("com.example.myapp") && componentName.getClassName().equals("com.example.myapp.MyActivity")) {
//                // MyActivity is the top activity in the task
//            } else {
//                // MyActivity is not the top activity in the task
//            }

        }
    }

    public static String getTopPackageName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        if (runningTasks != null && runningTasks.size() > 0) {
            ActivityManager.RunningTaskInfo taskInfo = runningTasks.get(0);
            ComponentName componentName = taskInfo.topActivity;
            GeeUILogUtils.logi("letianpai_test", "getTopPackageName: " + componentName.getPackageName());
            return componentName.getPackageName();
        }
        return "";
    }

    /**
     * 获取顶部 Activity
     *
     * @param context
     * @return
     */
    public static String getTopActivityName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        if (runningTasks != null && runningTasks.size() > 0) {
            ActivityManager.RunningTaskInfo taskInfo = runningTasks.get(0);
            ComponentName componentName = taskInfo.topActivity;
            if (componentName != null && componentName.getClassName() != null) {
                return componentName.getClassName();
            }
        }
        return null;
    }

    /**
     * 获取顶部 Activity
     *
     * @param context
     * @return
     */
    public static String getTopAppPackageName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
        if (runningTasks != null && runningTasks.size() > 0) {
            ActivityManager.RunningTaskInfo taskInfo = runningTasks.get(0);
            ComponentName componentName = taskInfo.topActivity;
            if (componentName != null && componentName.getClassName() != null) {
                return componentName.getPackageName();
            }
        }
        return null;
    }

    public static String getForegroundPackageName(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        if (runningAppProcesses != null && !runningAppProcesses.isEmpty()) {
            ActivityManager.RunningAppProcessInfo currentAppProcess = runningAppProcesses.get(1);
            return currentAppProcess.processName;
        }
        return null;
    }

    /**
     * @param context
     * @return
     */
    public static boolean isRobotOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.ROBOT_CLASS_NAME)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param context
     * @return
     */
    public static boolean isWifiConnectorOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_WIFI_CONNECTOR)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param context
     * @return
     */
    public static boolean isAlarmOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.ALARM_CLASS_NAME)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param context
     * @return
     */
    public static boolean isNewAlarmOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.ALARM_CLASS_NAME_NEW)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param context
     * @return
     */
    public static boolean isFactoryOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.FACTORY_MODE_CLASS_NAME)) {
            return true;
        } else if (activityName != null && activityName.equals(PackageConsts.FACTORY_CALI_MODE_CLASS_NAME)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param context
     * @return
     */
    public static boolean isOtaOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_OTA)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSpeechOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.SPEECH_CLASS_NAME)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isLexOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.LEX_CLASS_NAME)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param context
     * @return
     */
    public static boolean isOtherRobotAppOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && (!activityName.equals(PackageConsts.ROBOT_CLASS_NAME)) && (!activityName.equals(PackageConsts.LAUNCHER_CLASS_NAME))) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param context
     * @return
     */
    public static boolean isTargetAppRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(packageName) || info.baseActivity.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param context
     * @return
     */
    public static boolean isTargetAppTopRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param context
     * @return
     */
    public static boolean isAlarmRunning(Context context) {
//        return isTargetAppRunning(context, PackageConsts.ALARM_PACKAGE_NAME);
        return isTargetAppTopRunning(context, PackageConsts.ALARM_PACKAGE_NAME);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isVideoCallOnTheTop(Context context) {
        return isTargetAppTopRunning(context, PackageConsts.PACKAGE_NAME_VIDEO_CALL);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isVideoCallRunning(Context context) {
        return isTargetAppRunning(context, PackageConsts.PACKAGE_NAME_VIDEO_CALL);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isVideoCallServiceRunning(Context context) {
        return ServiceUtils.isServiceRunning(context, PackageConsts.PACKAGE_NAME_VIDEO_CALL_SERVICE);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isAlarmServiceRunning(Context context) {
        return ServiceUtils.isServiceRunning(context, PackageConsts.SERVICE_NAME_ALARM);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isRemindServiceRunning(Context context) {
        return ServiceUtils.isServiceRunning(context, PackageConsts.SERVICE_NAME_REMINDER);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isAppStoreServiceRunning(Context context) {
        return ServiceUtils.isServiceRunning(context, PackageConsts.SERVICE_NAME_APP_STORE);
    }


    /**
     * @param context
     * @return
     */
    public static boolean isAutoAppOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && (activityName.equals(PackageConsts.CLASS_NAME_AUTO_CHARGING))) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @param context
     * @return
     */
    // public static boolean isDesktopAppOnTheTop(Context context) {
    //     String activityName = getTopActivityName(context);
    //     if (activityName != null && (activityName.equals(PackageConsts.ACTIVITY_NAME_DESKTOP))) {
    //         return true;
    //     } else {
    //         return false;
    //     }
    //
    // }

    /**
     * @param context
     * @return
     */
    public static boolean isLauncherOrRobotOnTheTop(Context context) {
        String activityName = getTopActivityName(context);
        if (activityName != null && activityName.equals(PackageConsts.ROBOT_CLASS_NAME)) {
            return true;
        } else if (activityName != null && activityName.equals(PackageConsts.LAUNCHER_CLASS_NAME)) {
            return true;
        } else {
            return false;
        }

    }


    /**
     * @param context
     * @return
     */
    public static boolean isRobotRunning(Context context) {
        return isAppRunning(context, PackageConsts.ROBOT_CLASS_NAME);
    }

    public static boolean isAppRunning(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
        if (runningTasks != null) {
            for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                ComponentName componentName = taskInfo.topActivity;

                if (componentName.getPackageName().equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isActivityRunning(Context context, String activityName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        if (runningTasks != null) {
            for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                ComponentName componentName = taskInfo.topActivity;
                ComponentName componentName1 = taskInfo.baseActivity;
                if (componentName != null && !TextUtils.isEmpty(componentName.getClassName()) && (componentName.getClassName().equals(activityName))) {
                    return true;
                }
                if (componentName1 != null && !TextUtils.isEmpty(componentName1.getClassName()) && (componentName1.getClassName().equals(activityName))) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isSpeechRunning(Context context) {
        return isActivityRunning(context, PackageConsts.SPEECH_CLASS_NAME);
    }

    public static boolean isLexRunning(Context context) {
        return isActivityRunning(context, PackageConsts.LEX_CLASS_NAME);
    }

    /**
     * @param context
     * @return
     */
    public static boolean isRobotAppRunning(Context context) {
        return isActivityRunning(context, PackageConsts.ROBOT_CLASS_NAME);
    }

    /**
     * 开启ChatGPT
     *
     * @param context
     */
    public static void openChatGpt(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.rhj.chatgpt", "com.rhj.chatgpt.MainActivity"));
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    public static void openCountdown(Context context, String time) {
        GeeUILogUtils.logi("letianpai_control", "controlSteeringEngine_========================== 12 ==========================");
        controlSteeringEngine(context, false, false);
        setTimer(context, LTPConfigConsts.VALUE_ALARM, time);
        RobotCommandWordsCallback.getInstance().showBattery(true);

    }

    public static void startMIotService(ILetianpaiService iLetianpaiService, Context context) {
        RobotModeManager.getInstance(context).switchRobotMode(com.letianpai.robot.control.mode.ViewModeConsts.VM_STATIC_MODE, com.letianpai.robot.control.mode.ViewModeConsts.APP_MODE_MIJIA);
        RobotFuncResponseManager.stopRobot(iLetianpaiService);
    }

    /**
     * 切换舵机开关
     *
     * @param context
     */
    public static void controlSteeringEngine(Context context, boolean footSwitch, boolean sensorSwitch) {
        GeeUILogUtils.logi(TAG, "舵机卸力---" + footSwitch + " ====" + sensorSwitch);
        if (LetianpaiFunctionUtil.isAutoAppOnTheTop(context)) {
            return;
        }
        ControlSteeringEngineCallback.getInstance().setControlSteeringEngine(footSwitch, sensorSwitch);
    }

    /**
     * 回正并关闭舵机
     *
     * @param context
     */
    public static void changeToStand(Context context) {
        GestureCallback.getInstance().setGestures(GestureCenter.resetStandGesture(), RGestureConsts.GESTURE_COMMAND_DELAY);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                controlSteeringEngine(context, false, false);
            }
        }, 400);

    }

    /**
     * 回正并关闭舵机
     *
     * @param context
     */
    public static void changeToStand(Context context, boolean hasPower) {
        GestureCallback.getInstance().setGestures(GestureCenter.resetStandGesture(), RGestureConsts.GESTURE_COMMAND_DELAY);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (hasPower) {
                    controlSteeringEngine(context, true, true);
                } else {
                    controlSteeringEngine(context, false, false);
                }

            }
        }, 400);

    }

    /**
     * @param context
     */
    public static void openDevelopSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }

    /**
     * 开发者选项是否开启
     *
     * @return true 开启
     */
    public static boolean isOpenDevelopmentSetting(Context activity) {
        boolean enableAdb = Settings.Secure.getInt(activity.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        return enableAdb;
    }

    /**
     * usb调试是否开启
     *
     * @return true 开启
     */
    public static boolean isUSBDebugSetting(Context activity) {
        boolean enableAdb = Settings.Secure.getInt(activity.getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0;
        return enableAdb;
    }


    /**
     * 开启闹钟
     *
     * @param context
     */
    public static void openClock(Context context, String time) {
//        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(3, 0).toString());
//        //TODO 给MCU发消息，打开悬崖，悬空上爆棚
//        CommandResponseCallback.getInstance().setLTPCommand(MCUCommandConsts.COMMAND_TYPE_POWER_CONTROL, new PowerMotion(5, 0).toString());
        controlSteeringEngine(context, false, false);
        setTimer(context, LTPConfigConsts.VALUE_CLOCK, time);
        RobotCommandWordsCallback.getInstance().showBattery(true);
    }

    /**
     * 开启闹钟
     *
     * @param context
     */
    public static void openNotices(Context context, String time, String title) {
        controlSteeringEngine(context, false, false);
        setTimer(context, LTPConfigConsts.VALUE_NOTICE, time);
        RobotCommandWordsCallback.getInstance().showBattery(true);
    }

    /**
     * 开启倒计时
     *
     * @param context
     */
    public static boolean setTimer(Context context, String type, String time) {
        if (SystemFunctionUtil.isAppInstalled(context, "com.letianpai.robot.alarm")) {
            SystemFunctionUtil.wakeUp(context);
            RobotCommandWordsCallback.getInstance().showBattery(true);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.letianpai.robot.alarm", "com.letianpai.robot.alarm.MainActivity"));
            intent.putExtra(LTPConfigConsts.COUNT_DOWN_KEY, time);
            intent.putExtra(LTPConfigConsts.CLOCK_ALARM_TYPE, type);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
            return true;
        } else {
            RobotModeManager.getInstance(context).ttsUninstallAppText();
            return false;
        }
    }


//
//    public static void restoreRobot(Context context) {
//        Intent intent = new Intent("android.intent.action.FACTORY_RESET");
//        intent.putExtra("android.intent.extra.REASON", "Factory reset from app");
//        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", true);
//        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//        intent.setPackage("android");
//        context.startActivity(intent);
//    }

    public static void restoreRobot(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                RecoverySystem.rebootWipeUserData(context.getApplicationContext());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String packageName = context.getApplicationContext().getPackageName();
                Runtime.getRuntime().exec(new String[]{"su", "-c", "pm clear " + packageName}).waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static float getCpuThermal() {

        List<String> result = new ArrayList<>();
        BufferedReader br = null;
        float temp = 0;

        try {
            File dir = new File("/sys/class/thermal/");

            File[] files = dir.listFiles(file -> {
                if (Pattern.matches("thermal_zone[0-9]+", file.getName())) {
                    return true;
                }
                return false;
            });
            final int SIZE = files.length;
            String line;
            for (int i = 0; i < SIZE; i++) {
                br = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone" + i + "/temp"));
                line = br.readLine();
                if (line != null) {
                    long temperature = Long.parseLong(line);
                    if (temperature < 0) {
                        temp = -1f;
                        return temp;
                    } else {
                        temp = (float) (temperature / 1000.0);
                        return temp;
                    }

                }
                break;
            }
            return temp;
        } catch (FileNotFoundException e) {
            result.add(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return temp;
    }

    public static void takePhoto(Context context) {
        RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_TAKE_PHOTO, 1);
    }

    public static int getCompareTime(String time) {
//        LogUtils.logd("LetianpaiFunctionUtil", "takePhoto: ");

        String[] times = time.split(":");
        if (times == null || times.length < 2) {
            return 0;
        } else {
            return (Integer.valueOf(times[0])) * 100 + Integer.valueOf(times[1]);
        }
    }

    public static int getCompareTime(int hour, int minute) {
        return hour * 100 + minute;
    }

    public static boolean isTimeInSleepRange(Context context, int hour, int minute) {
        int startTime = RobotConfigManager.getInstance(context).getSleepStartTime();
        int endTime = RobotConfigManager.getInstance(context).getSleepEndTime();
        int currentTime = getCompareTime(hour, minute);
        if (startTime == 0 && endTime == 0) {
            return false;
        }

        if (startTime > endTime) {
            if (((currentTime >= startTime) && (currentTime <= 2400)) || ((currentTime > 0) && (currentTime <= endTime))) {
                return true;
            }
        } else if (startTime < endTime) {
            if ((currentTime >= startTime) && (currentTime <= endTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param context
     * @param hour
     * @param minute
     */
    public static void updateRobotSleepStatus(Context context, int hour, int minute) {
//        if (ChargingUpdateCallback.getInstance().isCharging()){
//            return;
//        }

        boolean isInRange = LetianpaiFunctionUtil.isTimeInSleepRange(context, hour, minute);
        boolean isSleepModeOn = RobotConfigManager.getInstance(context).getCloseScreenModeSwitch();
        int currentMode = RobotModeManager.getInstance(context).getRobotMode();
        GeeUILogUtils.logi("letianpai_sleep_test", "isInRange: " + isInRange);
        GeeUILogUtils.logi("letianpai_sleep_test", "isSleepModeOn: " + isSleepModeOn);
        GeeUILogUtils.logi("letianpai_sleep_test", "currentMode: " + currentMode);
//        if (isInRange && isSleepModeOn ){
//            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE,1);
//        }else if ((!isInRange || !isSleepModeOn) && (currentMode == ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE) ){
//            SystemFunctionUtil.wakeUp(context);
//            RobotModeManager.getInstance(context).switchToPreviousPlayMode();
//        }
        if (isInRange && isSleepModeOn) {
            GeeUILogUtils.logi("letianpai_sleep", "currentMode ===== updateRobotSleepStatus ========1 : " + currentMode);
            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE, 1);
        } else if ((!isInRange || !isSleepModeOn) && (currentMode == ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE)) {
            GeeUILogUtils.logi("letianpai_sleep", "currentMode ===== updateRobotSleepStatus ========2 : " + currentMode);
            GeeUILogUtils.logi("letianpai_sleep_test", "open view ========================== 3 ==========================");

            SystemFunctionUtil.wakeUp(context);
            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_NIGHT_SLEEP_MODE, 0);
        }

    }

    public static void startBleService(Context context) {
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.rhj.speech", "com.rhj.audio.service.BleService");
        intent.setComponent(cn);
        context.startService(intent);
    }

    public static void stopBleService(Context context) {
        Intent intent = new Intent();
        ComponentName cn = new ComponentName("com.rhj.speech", "com.rhj.audio.service.BleService");
        intent.setComponent(cn);
        context.stopService(intent);
    }

    public static void responseCharging(Context context) {
        //如果设备已经解绑
        if (RobotModeManager.getInstance(context).getRobotMode() == VM_UNBIND_DEVICE) {
            RobotCommandWordsCallback.getInstance().showBattery(true);
            return;
        }

        // long currentTime  = System.currentTimeMillis();
        // long inter = currentTime - LetianpaiFunctionUtil.getExitLongTrtcTime();
        // Log.i("LetianpaiFunctionUtil::responseCharging", " --- inter: "+ inter +
        //         "---LetianpaiFunctionUtil.getExitLongTrtcTime():"+ LetianpaiFunctionUtil.getExitLongTrtcTime() +
        //         "---LetianpaiFunctionUtil.isVideoCallServiceRunning(context):"+ LetianpaiFunctionUtil.isVideoCallServiceRunning(context)
        // );
        //远程实时监控在这过滤了。
        if ((RobotModeManager.getInstance(context).getRobotTrtcStatus() != -1)) {
            return;
        }
        // else if (LetianpaiFunctionUtil.isVideoCallServiceRunning(context) && (inter>= 0  && inter<= 50 )){
        //     Log.i("LetianpaiFunctionUtil::responseCharging", " =================================== letianpai_sleep_test ==== 3.3 ===================================  ");
        //     LetianpaiFunctionUtil.setExitLongTrtcTime(-1);
        // }

        ModeChangeCallback.getInstance().setModeChange(ViewModeConsts.VM_KILL_ROBOT_PROGRESS, 1);
        GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0001.1 ========== ");
        if (RobotModeManager.getInstance(context).isRobotDeepSleepMode() && TemperatureUpdateCallback.getInstance().isInHighTemperature()) {
            GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0002 ========== ");
            return;
        }

        GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0002.1 ========== ");
        if (RobotStatusResponser.getInstance(context).isNoNeedResponseMode()) {
            GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0003 ========== ");
            return;
        }
        GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0003.1 ========== ");
        if (RobotModeManager.getInstance(context).getInstance(context).isSleepMode()
                && RobotModeManager.getInstance(context).isPreviousModeIsRobotMode()
                && RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_SLEEP_ZZZ) {
            GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0005.1 ========== ");
            LetianpaiFunctionUtil.openTimeViewForSleep(context);
            return;
        }
        if (RobotModeManager.getInstance(context).getInstance(context).isSleepMode()) {
            GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0005 ========== ");
            return;
        }

        GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0006.2 ==========RobotModeManager.getInstance(context).getRobotMode():  " + RobotModeManager.getInstance(context).getRobotMode());
        GeeUILogUtils.logi("LetianpaiFunctionUtil::responseCharging", "=========== 0006.3 ==========RobotModeManager.getInstance(context).getRobotModeStatus():  " + RobotModeManager.getInstance(context).getRobotModeStatus());
        // if ((!RobotModeManager.getInstance(context).isRobotMode())
        //         && (!RobotModeManager.getInstance(context).isRobotWakeupMode())) {
        //     Log.e("letianpai_sleep_test", "=========== 0007 ========== ");
        //     return;
        // }
        int robotMode = RobotModeManager.getInstance(context).getRobotMode();
        int robotModeStatus = RobotModeManager.getInstance(context).getRobotModeStatus();
        if (robotMode == VM_AUDIO_WAKEUP_MODE_DEFAULT && robotModeStatus == 1) {
            return;
        } else if (robotMode == VM_AUDIO_WAKEUP_MODE && robotModeStatus == 0) {
            //退出了唤醒
            if (RobotModeManager.getInstance(context).isPreviousModeIsAppMode()) {
                RobotCommandWordsCallback.getInstance().showBattery(true);
                RobotModeManager.getInstance(context).switchToPreviousAppMode();
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0007.11 ========== ");
                return;
            }
        } else if (robotMode == VM_STATIC_MODE) {
            RobotCommandWordsCallback.getInstance().showBattery(true);
            RobotModeManager.getInstance(context).switchToPreviousAppMode();
            return;
        } else {
            if (RobotModeManager.getInstance(context).isPreviousModeIsAppMode()) {
                RobotCommandWordsCallback.getInstance().showBattery(true);
                RobotModeManager.getInstance(context).switchToPreviousAppMode();
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0007.12========== ");
                return;
            }
        }

        //睡眠模式下关闭屏幕
        if (RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_CLOSE_SCREEN) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0010 ========== ");
            RobotModeManager.getInstance(context).setRobotModeBeforeChargingOn(true);
            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 1);
            //睡眠模式下打呼噜
        } else if (RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_SLEEP_ZZZ) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0011 ========== ");
            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1);
        } else {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0012 ========== ");
            if (RobotModeManager.getInstance(context).getRobotMode() != ViewModeConsts.VM_STATIC_MODE) {
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== 0013 ========== ");
                RobotModeManager.getInstance(context).switchToPreviousAppMode();
            }
        }
    }

    public static void responseDisCharging(Context context) {
        GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0001 ========== ");
        if (RobotModeManager.getInstance(context).isRobotDeepSleepMode() && TemperatureUpdateCallback.getInstance().isInHighTemperature()) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0002 ========== ");
            return;
        }
        GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0002.1 ========== ");
        if (RobotStatusResponser.getInstance(context).isNoNeedResponseMode()) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0003 ========== ");
            return;
        }
        GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0003.1 ========== ");
        if (RobotModeManager.getInstance(context).isAppMode()) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0004 ========== ");
            return;
        }
        GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0004.1 ========== ");
//        if ((!RobotModeManager.getInstance(context).isRobotMode()) && (!RobotModeManager.getInstance(context).isRobotWakeupMode())){
//            Log.e("letianpai_sleep_test", "=========== responseDisCharging 0004.1 ========== ");
//            return;
//        }

        GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0008 ========== ");

//        if (RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_FUNCTION_MODE) {
//            Log.e("letianpai_test000", "=========== 0009 ========== ");
//            RobotModeManager.getInstance(context).setRobotModeBeforeChargingOn(true);
//            RobotModeManager.getInstance(context).switchToPreviousAppMode();
//        } else
        if (RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_CLOSE_SCREEN) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0010 ========== ");
            RobotModeManager.getInstance(context).setRobotModeBeforeChargingOn(true);
            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_BLACK_SCREEN_SLEEP_MODE, 1);

        } else if (RobotConfigManager.getInstance(context).getSleepTimeStatusModeSwitch() == ViewModeConsts.ROBOT_STATUS_SLEEP_ZZZ) {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0011 ========== ");
            RobotModeManager.getInstance(context).switchRobotMode(ViewModeConsts.VM_SLEEP_MODE, 1);

        } else {
            GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0012 ========== ");
            if (RobotModeManager.getInstance(context).getRobotMode() != ViewModeConsts.VM_STATIC_MODE) {
                GeeUILogUtils.logi("letianpai_sleep_test", "=========== responseDisCharging 0013 ========== ");
                RobotModeManager.getInstance(context).switchToPreviousAppMode();

            }
        }
    }

    public static void hideStatusBar() {
        RobotCommandWordsCallback.getInstance().showBattery(false);
    }

    public static void closeAppByPackageName(String packageName, Context context) {
        // ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        // activityManager.killBackgroundProcesses(packageName);

        ActivityManager mActivityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            method.invoke(mActivityManager, packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isAutoAppCanBeLaunched(Context context) {
        String activityName = getTopActivityName(context);
        if ((activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_NEWS))
                || (activityName != null && activityName.equals(PackageConsts.LAUNCHER_CLASS_NAME))
                || (activityName != null && activityName.equals(PackageConsts.PACKAGE_NAME_WORDS_ACTIVITY))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_COMMEMORATION))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_PACKAGE_NAME))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_COUNT_DOWN))
                || (activityName != null && activityName.equals(PackageConsts.PACKAGE_NAME_STOCK_ACTIVITY))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_FANS))
                || (activityName != null && activityName.equals(PackageConsts.CLASS_NAME_CUSTOM))
                || (activityName != null && activityName.equals(PackageConsts.PACKAGE_NAME_LAMP_ACTIVITY))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_REMINDER))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_SPECTRUM))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_POMO))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_MEDITATION))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_MESSAGE))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_TIME))
                || (activityName != null && activityName.equals(PackageConsts.ROBOT_CLASS_NAME))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_EXPRESSION))
                || (activityName != null && activityName.equals(PackageConsts.ACTIVITY_NAME_APHORISMS))
        ) {
            return true;
        } else {
            return false;
        }

    }
}
