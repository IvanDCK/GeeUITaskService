package com.letianpai.robot.control.nets

import android.content.Context
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.letianpai.robot.components.network.nets.GeeUIStatusUploader
import com.letianpai.robot.components.network.nets.GeeUiNetManager
import com.letianpai.robot.components.network.system.SystemUtil
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback
import com.letianpai.robot.control.callback.NetworkChangingUpdateCallback.NetworkChangingUpdateListener
import com.letianpai.robot.control.system.SystemFunctionUtil
import com.letianpai.robot.response.app.AppCmdResponser.Companion.getInstance
import com.letianpai.robot.response.ble.BleCmdResponser.Companion.getInstance
import com.letianpai.robot.response.identify.IdentifyCmdResponser.Companion.getInstance
import com.letianpai.robot.response.mi.MiIotCmdResponser.Companion.getInstance
import com.letianpai.robot.response.remote.RemoteCmdResponser.Companion.getInstance
import com.letianpai.robot.response.robotStatus.RobotStatusResponser.Companion.getInstance
import com.letianpai.robot.response.sensor.SensorCmdResponser.Companion.getInstance
import com.letianpai.robot.response.speech.SpeechCmdResponser.Companion.getInstance
import com.renhejia.robot.commandlib.parser.deviceinfo.DeviceInfo
import com.renhejia.robot.commandlib.parser.displaymodes.calendar.CalenderInfo
import com.renhejia.robot.commandlib.parser.displaymodes.countdown.CountDownListInfo
import com.renhejia.robot.commandlib.parser.displaymodes.fans.FansInfo
import com.renhejia.robot.commandlib.parser.displaymodes.weather.WeatherInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * @author liujunbin
 */
class GeeUINetResponseManager private constructor(context: Context) {
    private var mContext: Context? = null
    private var gson: Gson? = null
    private var fansInfo: FansInfo? = null
    var weatherInfo: WeatherInfo? = null
        get() {
            if (field == null) {
                weather
            }
            return field
        }
    var countDownListInfo: CountDownListInfo? = null
        get() {
            if (field == null) {
                countDownList
            }
            return field
        }
    private var calenderInfo: CalenderInfo? = null
    private var handler: NetRequestHandler? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        this.mContext = context
        gson = Gson()
        handler = NetRequestHandler(context)
        addNetChangeListeners()
    }

    private fun addNetChangeListeners() {
        NetworkChangingUpdateCallback.instance
            .registerChargingStatusUpdateListener(object : NetworkChangingUpdateListener {
                override fun onNetworkChargingUpdateReceived(networkType: Int, networkStatus: Int) {
                    Log.i(
                        "GeeUINetResponseManager",
                        "onNetworkChargingUpdateReceived--networkType: $networkType--networkStatus::$networkStatus"
                    )
                    if (networkType == NetworkChangingUpdateCallback.NETWORK_TYPE_WIFI) {
                        displayInfo
                        //Network changes, synchronised data
                        GeeUIStatusUploader.getInstance(mContext!!)!!.syncRobotStatus()
                    }
                }
            })
    }

    val displayInfo: Unit
        get() {
            if (WIFIConnectionManager.isNetworkAvailable(
                    mContext!!
                ) && SystemUtil.hasHardCode()
            ) {
            } else if (WIFIConnectionManager.isNetworkAvailable(
                    mContext!!
                ) && !SystemUtil.hasHardCode()
            ) {
                encodeInfo
            }
        }

    val encodeInfo: Unit
        get() {
            if (SystemUtil.isInChinese) {
                getEncodeInfo(true)
            } else {
                getEncodeInfo(false)
            }
        }

    fun getEncodeInfo(isChinese: Boolean) {
        if (SystemUtil.hasHardCode()) {
            return
        }
        GeeUiNetManager.getDeviceInfo(mContext, isChinese, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.body != null) {
                    var deviceInfo: DeviceInfo? = null
                    var info: String? = ""
                    if (response.body != null) {
                        info = response.body!!.string()
                    }
                    try {
                        if (info != null) {
                            deviceInfo = Gson().fromJson(
                                info,
                                DeviceInfo::class.java
                            )
                            if (deviceInfo != null && !TextUtils.isEmpty(deviceInfo.data!!.client_id) && !TextUtils.isEmpty(
                                    deviceInfo.data!!.hard_code
                                ) && !TextUtils.isEmpty(deviceInfo.data!!.sn)
                            ) {
                                SystemUtil.hardCode = deviceInfo.data!!.hard_code
                                //                            getDisplayInfo();
                                getDisplayInformation(1000)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    /**
     *
     */
    fun updateWeather() {
        Thread { weather }.start()
    }

    /**
     *
     */
    private fun updateCountDownList() {
        Thread { countDownList }.start()
    }

    private val weather: Unit
        get() {
            GeeUiNetManager.getWeatherInfo(
                mContext,
                SystemFunctionUtil.isChinese,
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                    }

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (response.body != null) {
                            var weatherInfo: WeatherInfo? = null
                            var info: String? = ""
                            if (response.body != null) {
                                info = response.body!!.string()
                            }
                            if (info != null) {
                                weatherInfo =
                                    gson!!.fromJson(info, WeatherInfo::class.java)
                                if (weatherInfo != null) {
                                }
                            }
                        }
                    }
                })
        }

    private val countDownList: Unit
        get() {
            GeeUiNetManager.getCountDownList(
                mContext,
                SystemFunctionUtil.isChinese,
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        if (response.body != null) {
                            var countDownListInfo: CountDownListInfo? = null
                            var info: String? = ""

                            if (response.body != null) {
                                info = response.body!!.string()
                            }
                            if (info != null) {
                                countDownListInfo = gson!!.fromJson(
                                    info,
                                    CountDownListInfo::class.java
                                )
                            }
                        }
                    }
                })
        }

    fun setFansInfo(fansInfo: FansInfo?) {
        this.fansInfo = fansInfo
    }

    fun setCalenderInfo(calenderInfo: CalenderInfo?) {
        this.calenderInfo = calenderInfo
    }


    private fun getDisplayInformation(delay: Long) {
        val message: Message = Message()
        message.what = GET_DISPLAY_INFO
        if (delay == 0L) {
            handler!!.sendMessage(message)
        } else {
            handler!!.sendMessageDelayed(message, delay)
        }
    }

    private inner class NetRequestHandler(context: Context) : Handler() {
        private val context: WeakReference<Context> = WeakReference(context)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == GET_DISPLAY_INFO) {
                displayInfo
            } else if (msg.what == SHOW_GESTURES_STR) {
            }
        }
    }

    companion object {
        private var instance: GeeUINetResponseManager? = null
        private const val GET_DISPLAY_INFO: Int = 2
        private const val SHOW_GESTURES_STR: Int = 3
        fun getInstance(context: Context): GeeUINetResponseManager {
            synchronized(GeeUINetResponseManager::class.java) {
                if (instance == null) {
                    instance = GeeUINetResponseManager(context.getApplicationContext())
                }
                return instance!!
            }
        }
    }
}
