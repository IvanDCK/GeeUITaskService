package com.letianpai.robot.control.encryption

import java.security.MessageDigest

/**
 * @author liujunbin
 */
object MD5 {
    fun encode(string: String): String? {
        try {
            val md: MessageDigest = MessageDigest.getInstance("MD5")
            md.update(string.toByteArray())
            val b: ByteArray = md.digest()
            var i: Int
            val buf: StringBuffer = StringBuffer("")
            for (offset in b.indices) {
                i = b.get(offset).toInt()
                if (i < 0) {
                    i += 256
                }
                if (i < 16) {
                    buf.append("0")
                }
                buf.append(Integer.toHexString(i))
            }
            return buf.toString()
            // if (type) {
            // return buf.toString(); // 32
            // } else {
            // return buf.toString().substring(8, 24);// 16
            // }
        } catch (e: Exception) {
            return null
        }
    }

    fun encode(bytes: ByteArray): String? {
        try {
            val md: MessageDigest = MessageDigest.getInstance("MD5")
            md.update(bytes)
            val b: ByteArray = md.digest()
            var i: Int
            val buf: StringBuffer = StringBuffer("")
            for (offset in b.indices) {
                i = b.get(offset).toInt()
                if (i < 0) {
                    i += 256
                }
                if (i < 16) {
                    buf.append("0")
                }
                buf.append(Integer.toHexString(i))
            }
            return buf.toString()
            // if (type) {
            // return buf.toString(); // 32
            // } else {
            // return buf.toString().substring(8, 24);// 16
            // }
        } catch (e: Exception) {
            return null
        }
    }
}
