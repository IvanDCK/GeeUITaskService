package com.letianpai.robot.control.storage;

import android.content.Context;

import com.letianpai.robot.control.floating.statusbar.TimeUtil;


/**
 * 机器人 偏好设置管理器
 * @author liujunbin
 */
public class RobotConfigManager implements RobotConfigConst {

    private static RobotConfigManager mRobotConfigManager;
    private RobotSharedPreference mRobotSharedPreference;
    private Context mContext;


    private RobotConfigManager(Context context) {
        this.mContext = context;
        this.mRobotSharedPreference = new RobotSharedPreference(context,
                RobotSharedPreference.SHARE_PREFERENCE_NAME, RobotSharedPreference.ACTION_INTENT_CONFIG_CHANGE);
    }


    private void initKidSmartConfigState() {

    }

    public static RobotConfigManager getInstance(Context context) {
        if (mRobotConfigManager == null) {
            mRobotConfigManager = new RobotConfigManager(context);
            mRobotConfigManager.initKidSmartConfigState();
            mRobotConfigManager.commit();
        }
        return mRobotConfigManager;

    }

    public boolean commit() {
        return mRobotSharedPreference.commit();
    }

    public boolean isGeneralBatterySwitchOn() {
        boolean isActivated = mRobotSharedPreference.getBoolean(KEY_GENERAL_BATTERY_SWITCH, false);
        isActivated = false;
        return isActivated;
    }

    public void setGeneralBatterySwitchStatus(boolean generalBatterySwitchStatus) {
        mRobotSharedPreference.putBoolean(KEY_GENERAL_BATTERY_SWITCH, generalBatterySwitchStatus);
    }

    public int getRobotVolume(){
        return mRobotSharedPreference.getInt(KEY_VOLUME,6);
    }

    public void setRobotVolume(int volume){
        if (volume > 15){
            volume = 15;
        }else if (volume <0 ){
            volume = 0;
        }
        mRobotSharedPreference.putInt(KEY_VOLUME,volume);
    }

    public void setSleepStartTime(int time){
        mRobotSharedPreference.putInt(KEY_START_SLEEP_TIME,time);
    }

    public void setSleepEndTime(int time){
        mRobotSharedPreference.putInt(KEY_END_SLEEP_TIME,time);
    }

    public int getSleepStartTime(){
        return mRobotSharedPreference.getInt(KEY_START_SLEEP_TIME,0);
    }

    public int getSleepEndTime(){
        return mRobotSharedPreference.getInt(KEY_END_SLEEP_TIME,0);
    }

    public void setSleepModeStatus(boolean status){

        mRobotSharedPreference.putBoolean(KEY_SLEEP_MODE_STATUS,status);
    }

    public boolean getSleepModeStatus(){
        return mRobotSharedPreference.getBoolean(KEY_SLEEP_MODE_STATUS,false);

    }

    public void setCloseScreenModeSwitch(boolean status){

        mRobotSharedPreference.putBoolean(KEY_CLOSE_SCREEN_MODE_SWITCH,status);
    }

    public boolean getCloseScreenModeSwitch(){
        return mRobotSharedPreference.getBoolean(KEY_CLOSE_SCREEN_MODE_SWITCH,false);

    }

    public void setSleepSoundModeSwitch(boolean status){
        mRobotSharedPreference.putBoolean(KEY_SLEEP_SOUND_MODE_SWITCH,status);
    }

    public boolean getSleepSoundModeSwitch(){
        return mRobotSharedPreference.getBoolean(KEY_SLEEP_SOUND_MODE_SWITCH,true);
    }

    public void setSleepTimeStatusModeSwitch(int status){
        mRobotSharedPreference.putInt(KEY_SLEEP_TIME_STATUS_MODE_SWITCH,status);
    }

    public int getSleepTimeStatusModeSwitch(){
        return mRobotSharedPreference.getInt(KEY_SLEEP_TIME_STATUS_MODE_SWITCH,2);
    }

    public String getNoticeTitle(){
        String key = TimeUtil.getNoticeKeyTime();
        return mRobotSharedPreference.getString(key,null);
    }
    public String getNoticeTitle(String key){
        return mRobotSharedPreference.getString(key,null);
    }

    public void setNoticeTitle(String key,String title){
        mRobotSharedPreference.putString(key,title);
    }

    public String getRemindText(int key){
        return mRobotSharedPreference.getString(KEY_REMIND_+key,null);
    }

    public void setReminderText(int key,String title){
        mRobotSharedPreference.putString(KEY_REMIND_+ key,title);
    }

    public void setRemindWaterStartTime(int time){
        mRobotSharedPreference.putInt(KEY_REMIND_WATER_START_TIME,time);
    }

    public int getRemindWaterStartTime(){
        return mRobotSharedPreference.getInt(KEY_REMIND_WATER_START_TIME,0);
    }

    public void setRemindWaterEndTime(int time){
        mRobotSharedPreference.putInt(KEY_REMIND_WATER_END_TIME,time);
    }

    public int getRemindWaterEndTime(){
        return mRobotSharedPreference.getInt(KEY_REMIND_WATER_END_TIME,0);
    }

    public void settRemindWaterSwitch(boolean status){
        mRobotSharedPreference.putBoolean(KEY_REMIND_WATER_SWITCH,status);
    }

    public boolean getRemindWaterSwitch(){
        return mRobotSharedPreference.getBoolean(KEY_REMIND_WATER_SWITCH,false);
    }

    ////

    public void setRemindSedStartTime(int time){
        mRobotSharedPreference.putInt(KEY_REMIND_SIT_START_TIME,time);
    }

    public int getRemindSedStartTime(){
        return mRobotSharedPreference.getInt(KEY_REMIND_SIT_START_TIME,0);
    }

    public void setRemindSedEndTime(int time){
        mRobotSharedPreference.putInt(KEY_REMIND_SIT_END_TIME,time);
    }

    public int getRemindSedEndTime(){
        return mRobotSharedPreference.getInt(KEY_REMIND_SIT_END_TIME,0);
    }

    public void setRemindSedSwitch(boolean status){
        mRobotSharedPreference.putBoolean(KEY_REMIND_SIT_SWITCH,status);
    }

    public boolean getRemindSedSwitch(){
        return mRobotSharedPreference.getBoolean(KEY_REMIND_SIT_SWITCH,false);
    }
    /////

    public void setRemindSiteStartTime(int time){
        mRobotSharedPreference.putInt(KEY_REMIND_SIT_POSTURE_START_TIME,time);
    }

    public int getRemindSiteStartTime(){
        return mRobotSharedPreference.getInt(KEY_REMIND_SIT_POSTURE_START_TIME,0);
    }

    public void setRemindSiteEndTime(int time){
        mRobotSharedPreference.putInt(KEY_REMIND_SIT_POSTURE_END_TIME,time);
    }

    public int getRemindSiteEndTime(){
        return mRobotSharedPreference.getInt(KEY_REMIND_SIT_POSTURE_END_TIME,0);
    }

    public void setRemindSiteSwitch(boolean status){
        mRobotSharedPreference.putBoolean(KEY_REMIND_SIT_POSTURE_SWITCH,status);
    }

    public boolean getRemindSiteSwitch(){
        return mRobotSharedPreference.getBoolean(KEY_REMIND_SIT_POSTURE_SWITCH,false);
    }

    public void setRemindKeepNoticeSwitch(boolean status){
        mRobotSharedPreference.putBoolean(KEY_REMIND_KEEP_NOTICE_SWITCH,status);
    }

    public boolean getRemindKeepNoticeSwitch(){
        return mRobotSharedPreference.getBoolean(KEY_REMIND_KEEP_NOTICE_SWITCH,false);
    }

    public void setRemindKeepNoticeTime(String noticeTime){
        mRobotSharedPreference.putString(KEY_REMIND_KEEP_NOTICE_TIME,noticeTime);
    }

    public String getRemindKeepNoticeTime(){
        return mRobotSharedPreference.getString(KEY_REMIND_KEEP_NOTICE_TIME,null);
    }

    public void setRemindKeepNoticeWeek(String noticeTime){
        mRobotSharedPreference.putString(KEY_REMIND_KEEP_NOTICE_WEEK,noticeTime);
    }

    public String getRemindKeepNoticeWeek(){
        return mRobotSharedPreference.getString(KEY_REMIND_KEEP_NOTICE_WEEK,null);
    }

    public long getUpdateTime() {
        return mRobotSharedPreference.getLong(KEY_UPDATE_ROBOT_TIME,0l);
    }

    public void setUpdateTime(long time){
        mRobotSharedPreference.putLong(KEY_UPDATE_ROBOT_TIME,time);
    }

    public boolean getAutomaticRechargeSwitch() {
        return (mRobotSharedPreference.getInt(KEY_AUTOMATIC_RECHARGE_SWITCH,0) == 1);
    }

    public void setAutomaticRechargeSwitch(int switchStatus){
        mRobotSharedPreference.putInt(KEY_AUTOMATIC_RECHARGE_SWITCH,switchStatus);
    }

    public int getAutomaticRechargeVal() {
        return mRobotSharedPreference.getInt(KEY_AUTOMATIC_RECHARGE_VAL,50);
    }

    public void setAutomaticRechargeVal(int rechargeVal){
        mRobotSharedPreference.putInt(KEY_AUTOMATIC_RECHARGE_VAL,rechargeVal);
    }



}
