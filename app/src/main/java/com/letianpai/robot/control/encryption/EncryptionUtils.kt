package com.letianpai.robot.control.encryption

import android.content.Context
import com.google.gson.Gson
import com.letianpai.robot.components.network.system.SystemUtil
import java.util.Locale

/**
 * @author liujunbin
 */
class EncryptionUtils private constructor(private val mContext: Context) {
    private var mGson: Gson? = null

    init {
        init()
    }

    private fun init() {
        mGson = Gson()
    }

    private val hardcode: Unit
        get() {
        }

    companion object {
        private var instance: EncryptionUtils? = null
        private const val partSecretKey: String = "your partSecretKey"

        fun getInstance(context: Context): EncryptionUtils {
            synchronized(EncryptionUtils::class.java) {
                if (instance == null) {
                    instance = EncryptionUtils(context.applicationContext)
                }
                return instance!!
            }
        }

        /**
         * Ways to obtain a signature
         *
         * @param inputValue
         * @param ts
         * @return
         */
        private fun getDeviceSign(inputValue: String, ts: String): String {
            val deviceSecretKey: String? = MD5.encode(inputValue + ts + partSecretKey)
            val macSign: String = Sha256Utils.getSha256Str(inputValue + ts + deviceSecretKey)
            return macSign
        }

        val robotSign: String
            get() {
                val mac: String =
                    robotMac
                val ts: String =
                    ts
                var robotSign: String =
                    getDeviceSign(
                        mac,
                        ts
                    )
                robotSign = "Bearer $robotSign"
                return robotSign
            }

        fun getHardCodeSign(ts: String): String {
            val mac: String =
                robotMac
            var robotSign: String = getDeviceSign(mac, ts)
            robotSign = "Bearer $robotSign"
            return robotSign
        }

        /**
         * Getting the robot signature
         *
         * @param sn
         * @param hardcode
         * @param ts
         * @return
         */
        fun getRobotSign(sn: String, hardcode: String, ts: String): String {
            var robotSign: String = getDeviceSign(sn + hardcode, ts)
            robotSign = "Bearer $robotSign"
            return robotSign
        }

        fun getDeviceSign(sn: String, hardcode: String, ts: String): String {
            val robotSign: String = getDeviceSign(sn + hardcode, ts)
            return robotSign
        }

        val robotMac: String
            get() = (SystemUtil.getWlanMacAddress()).lowercase(
                Locale.getDefault()
            )

        val ts: String
            get() {
                return (System.currentTimeMillis() / 1000).toString() + ""
            }
    }
}
