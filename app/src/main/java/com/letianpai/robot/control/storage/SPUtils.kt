package com.letianpai.robot.control.storage

import android.content.Context
import android.content.SharedPreferences

class SPUtils private constructor(context: Context) {
    private var sharedPreferences: SharedPreferences? = null
    val editor: SharedPreferences.Editor
        get() {
            return sharedPreferences!!.edit()
        }

    //sp fetch data
    fun putString(key: String?, value: String?) {
        editor.putString(key, value).apply()
    }

    fun putBoolean(key: String?, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun putInt(key: String?, value: Int) {
        editor.putInt(key, value).apply()
    }

    fun putDouble(key: String?, value: Double) {
        editor.putFloat(key, value.toFloat()).apply()
    }

    //sp fetch data
    fun getString(key: String?): String {
        return sharedPreferences!!.getString(key, "")!!
        //"No saved data’ is the default return value if the key is empty.
    }

    fun getInt(key: String?): Float {
        return sharedPreferences!!.getFloat(key, 0f)
    }

    fun getDouble(key: String?): Double {
        return sharedPreferences!!.getFloat(key, 0f).toDouble()
    }

    fun getLong(key: String?): Long {
        return sharedPreferences!!.getLong(key, 0)
    }

    fun getBoolean(key: String?): Boolean {
        return sharedPreferences!!.getBoolean(key, false)
    }

    fun getBooleanDefaultTrue(key: String?): Boolean {
        return sharedPreferences!!.getBoolean(key, true)
    }

    init {
        sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val SP_NAME: String = "taskservice"
        private var spUtils: SPUtils? = null

        fun getInstance(context: Context): SPUtils? {
            if (spUtils == null) {
                return newInstance(context)
            }
            return spUtils
        }

        @Synchronized
        private fun newInstance(context: Context): SPUtils {
            if (spUtils == null) {
                spUtils = SPUtils(context)
            }
            return spUtils!!
        }
    }
}

