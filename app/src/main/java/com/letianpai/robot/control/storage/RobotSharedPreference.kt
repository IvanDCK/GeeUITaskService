package com.letianpai.robot.control.storage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/**
 * @author liujunbin
 */
class RobotSharedPreference(context: Context?, fileName: String?, action: String?) {
    private var mContext: Context? = null
    private var mFileName: String? = null

    /**
     * The system SharedPreferences object in use
     */
    private var mEditor: SharedPreferences.Editor? = null
    private var mSharedPref: SharedPreferences? = null

    /**
     * The id of the resource for which sharedpreference is used, default -1.
     */
    private var mMode: Int = Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS

    /**
     * 内存数据的map
     */
    private var mMap: MutableMap<String?, Any?>? = null

    /**
     * Indicates whether the memory data has been changed or not, to avoid unnecessary write file operations
     */
    private var mHasChanged: Boolean = false

    init {
        mContext = context

        mMode = Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS

        this.mFileName = fileName
        reloadSharedPref(false)
    }

    /**
     * File operations, reloading configuration files
     *
     * @param syncIPCFlag true will notify all processes to reload, otherwise only the calling process will be loaded.
     */
    fun reloadSharedPref(syncIPCFlag: Boolean) {
        mSharedPref = mContext!!.getSharedPreferences(mFileName, mMode)
        mEditor = mSharedPref!!.edit()
        mHasChanged = false
        reloadMap()

        if (syncIPCFlag) {
            //sendIPCSyncBroadcast();
            sendSettingChangeBroadcast()
        }
    }

    private fun sendSettingChangeBroadcast() {
        val intent: Intent = Intent(ACTION_INTENT_CONFIG_CHANGE)
        mContext!!.sendBroadcast(intent)
    }

    private fun sendMessageDelay(handleid: Int, delay: Long) {
    }

    fun reloadMap() {
        if (mMap != null) {
            mMap!!.clear()
        }
        mMap = mSharedPref!!.getAll() as MutableMap<String?, Any?>?
    }

    val map: Map<String?, Any?>?
        get() {
            return this.mMap
        }

    /**
     * Memory operations, releasing resources occupied by objects, cancelling broadcasts, clearing memory data
     */
    fun terminate() {
        try {
            // mContext.unregisterReceiver(mConfigChangeReceiver);
            // if (mMap != null) {
            // mMap.clear();
            // mMap = null;
            // }
        } catch (e: Exception) {
        }
    }

    /**
     * Determine if a Map contains the specified key
     *
     * @param key
     * @return boolean
     */
    fun contains(key: String?): Boolean {
        return mMap!!.containsKey(key)
    }

    /**
     * File operations, submit data to the file, this function for the disk io write file,
     * after the success of the data will notify the use of the file data to reload data
     *
     * @return boolean true Write file success; false Write file failure
     */
    fun commit(): Boolean {
        if (!mHasChanged) {
            return false
        }
        if (mEditor != null) {
            if (mEditor!!.commit()) {
                mHasChanged = false
                sendMessageDelay(HANDLE_SETTING_CHANGED, DELAY_SEND_BROADCAST)
                //sendSettingChangeBroadcast();
                return true
            }
        }
        return false
    }

    /**
     * In-memory operation to remove data containing a specific key
     *
     * @param key void
     */
    fun remove(key: String?) {
        mEditor = mEditor!!.remove(key)
        mMap!!.remove(key)
        mHasChanged = true
    }

    /**
     * Memory operations, clearing data
     *
     * @return boolean true success; false failure
     */
    fun clear(): Boolean {
        if (mEditor != null) {
            mEditor!!.clear()
            mMap!!.clear()
            mHasChanged = true
            return true
        }
        return false
    }

    /**
     * Private public method, add data, value is object
     *
     * @param key
     * @param defValue
     * @return boolean true succeeds, false fails
     */
    private fun setValue(key: String?, defValue: Any): Boolean {
        val preValue: Any? = mMap!!.put(key, defValue)
        if (preValue == null || preValue != defValue) {
            mHasChanged = true
            return true
        }
        return false
    }

    /**
     * Memory operation, add data, value is boolean
     *
     * @param key
     * @param defValue
     */
    fun putBoolean(key: String?, defValue: Boolean) {
        if (setValue(key, defValue)) {
            mEditor = mEditor!!.putBoolean(key, defValue)
        }
    }

    /**
     * Memory operation, add data, value is int
     *
     * @param key
     * @param defValue void
     */
    fun putInt(key: String?, defValue: Int) {
        if (setValue(key, defValue)) {
            mEditor = mEditor!!.putInt(key, defValue)
        }
    }

    /**
     * Memory operations, add data, value is long
     *
     * @param key
     * @param defValue void
     */
    fun putLong(key: String?, defValue: Long) {
        if (setValue(key, defValue)) {
            mEditor = mEditor!!.putLong(key, defValue)
        }
    }

    /**
     * Memory operation, add data, value is float
     *
     * @param key
     * @param defValue void
     */
    fun putFloat(key: String?, defValue: Float) {
        if (setValue(key, defValue)) {
            mEditor = mEditor!!.putFloat(key, defValue)
        }
    }

    /**
     * Memory operation, add data, value is STRING
     *
     * @param key
     * @param defValue void
     */
    fun putString(key: String?, defValue: String) {
        if (setValue(key, defValue)) {
            mEditor = mEditor!!.putString(key, defValue)
        }
    }

    /**
     * Memory manipulation to get data of type boolean
     *
     * @param key
     * @param defValue 默认值
     * @return boolean
     */
    fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return mMap!![key] as Boolean? ?: defValue
    }

    /**
     * Memory manipulation to get data of type FLOAT
     *
     * @param key
     * @param defValue default value
     * @return float
     */
    fun getFloat(key: String?, defValue: Float): Float {
        return mMap!![key] as Float? ?: defValue
    }

    /**
     * Memory manipulation to get data of type INT
     *
     * @param key
     * @param defValue default value
     * @return int
     */
    fun getInt(key: String?, defValue: Int): Int {
        return mMap!![key] as Int? ?: defValue
    }

    /**
     * Memory manipulation to get data of type long
     *
     * @param key
     * @param defValue default value
     * @return long
     */
    fun getLong(key: String?, defValue: Long): Long {
        return mMap!![key] as Long? ?: defValue
    }

    /**
     * Memory manipulation to get data of type string
     *
     * @param key
     * @param defValue default value
     * @return String
     */
    fun getString(key: String?, defValue: String?): String {
        return mMap!![key] as String? ?: defValue!!
    }

    companion object {
        const val UPDATE_TYPE_NONE: Int = 0

        /**
         *  Broadcasting-related
         */
        const val ACTION_INTENT_CONFIG_CHANGE: String = "com.letianpai.robot.SETTING_CHANGE"


        const val SHARE_PREFERENCE_NAME: String = "RobotConfig"

        private const val HANDLE_SETTING_CHANGED: Int = 10
        private const val DELAY_SEND_BROADCAST: Long = 200
    }
}
