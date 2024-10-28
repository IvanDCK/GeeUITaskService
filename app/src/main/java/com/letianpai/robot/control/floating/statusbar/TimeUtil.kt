package com.letianpai.robot.control.floating.statusbar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtil {
    fun getWeek(time: Long): String {
        val cd: Calendar = Calendar.getInstance()
        cd.setTime(Date(time))

        val year: Int = cd.get(Calendar.YEAR) //获取年份
        val month: Int = cd.get(Calendar.MONTH) //获取月份
        val day: Int = cd.get(Calendar.DAY_OF_MONTH) //获取日期
        val week: Int = cd.get(Calendar.DAY_OF_WEEK) //获取星期
        val weekString: String = when (week) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            else -> "周六"
        }

        return weekString
    }

    /**
     *
     * @param time  1541569323155
     * @param pattern yyyy-MM-dd HH:mm:ss
     * @return 2018-11-07 13:42:03
     */
    fun getDate2String(time: Long, pattern: String?): String {
        val date: Date = Date(time)
        val format: SimpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(date)
    }

    val correctTime: String
        get() {
            val pattern1: String = "yyyy/MM/dd"
            val pattern2: String = "HH:mm"
            val currentTime: Long = System.currentTimeMillis()
            val a: String =
                getDate2String(
                    currentTime,
                    pattern1
                )
            val b: String =
                getDate2String(
                    currentTime,
                    pattern2
                )
            val c: String =
                getWeek(currentTime)
            return "$a $c $b"
        }

    val noticeKeyTime: String
        get() {
            val pattern1 = "yyyyMMdd"
            val pattern2 = "HH:mm"
            val currentTime: Long = System.currentTimeMillis()
            val a: String =
                getDate2String(
                    currentTime,
                    pattern1
                )
            val b: String =
                getDate2String(
                    currentTime,
                    pattern2
                )
            val d = "-"
            return b + d + a
        }

    fun getNoticeKeyTime(time: Long): String {
        val pattern1 = "yyyyMMdd"
        val pattern2 = "HH:mm"
        val a: String = getDate2String(time, pattern1)
        val b: String = getDate2String(time, pattern2)
        val d = "-"
        return b + d + a
    }

    fun getNoticeDayTime(time: Long): String {
        val pattern1 = "yyyyMMdd"
        val a: String = getDate2String(time, pattern1)
        return a
    }

    fun getNoticeTime(time: Long): String {
        val pattern2 = "HH:mm"
        val b: String = getDate2String(time, pattern2)
        return b
    }
}
