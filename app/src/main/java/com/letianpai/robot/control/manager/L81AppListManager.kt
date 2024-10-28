package com.letianpai.robot.control.manager

import android.content.Context
import com.letianpai.robot.taskservice.entity.PackageConsts

/**
 * @author liujunbin
 */
class L81AppListManager(private val mContext: Context) {
    private val robotPackageList: ArrayList<String> = ArrayList()

    init {
        initPackageList()
    }


    private fun initPackageList() {
        robotPackageList.clear()
        robotPackageList.add(PackageConsts.TAKEPHOTO_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.ROBOT_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.LAUNCHER_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.AUTO_APP_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.LEX_CLASS_PACKAGE)
        robotPackageList.add(PackageConsts.SPEECH_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.ALARM_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.STOCK_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.WEATHER_PACKAGE_NAME)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_COUNT_DOWN)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_COMMEMORATION)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_WORDS)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_NEWS)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_MESSAGE)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_FANS)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_IDENT)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_CUSTOM)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_VIDEO_CALL)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_LAMP)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_REMINDER)
        robotPackageList.add(PackageConsts.PACKAGE_APP_NAME_REMINDER)
        robotPackageList.add(PackageConsts.PACKAGE_APP_NAME_SPECTRUM)
        robotPackageList.add(PackageConsts.PACKAGE_APP_NAME_POMO)
        robotPackageList.add(PackageConsts.PACKAGE_APP_NAME_OTA)
        robotPackageList.add(PackageConsts.PACKAGE_APP_NAME_MEDITATION)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_TIME)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_EXPRESSION)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_ALBUM)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_WIFI_CONNECTOR)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_GEEUI_SETTINGS)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_APP_STORE)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_FIST_PALM_GAME)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_VOICE_MEMO)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_MCU_SERVICE)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_APHORISMS)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_GEEUI_RESOURCE)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_GEEUI_VIDEO_PLAYER)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_GEEUI_DOWNLOADER)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_GEEUI_MY_MUSIC)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_TASK_SERVICE)
        robotPackageList.add(PackageConsts.PACKAGE_NAME_DESKTOP)
    }

    fun isInThePackageList(packageName: String): Boolean {
        if (robotPackageList == null || robotPackageList.size == 0) {
            return false
        }
        if (robotPackageList.contains(packageName)) {
            return true
        } else {
            return false
        }
    }


    companion object {
        private var sInstance: L81AppListManager? = null
        fun getInstance(context: Context): L81AppListManager {
            if (sInstance == null) {
                synchronized(L81AppListManager::class.java) {
                    if (sInstance == null) {
                        sInstance = L81AppListManager(context)
                    }
                }
            }
            return sInstance!!
        }
    }
}
