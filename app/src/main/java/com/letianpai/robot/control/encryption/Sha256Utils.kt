package com.letianpai.robot.control.encryption

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * @author liujunbin
 */
object Sha256Utils {
    /**
     * sha256 encryption
     *
     * @param str The string to be encrypted
     * @return encrypted string
     */
    fun getSha256Str(str: String): String {
        val messageDigest: MessageDigest
        var encodeStr: String = ""
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(str.toByteArray(StandardCharsets.UTF_8))
            encodeStr = byte2Hex(messageDigest.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return encodeStr
    }

    /**
     * sha256 encryption Converts byte to hexadecimal.
     *
     * @param bytes byte code
     * @return encrypted string
     */
    private fun byte2Hex(bytes: ByteArray): String {
        val stringBuilder: StringBuilder = StringBuilder()
        var temp: String
        for (aByte: Byte in bytes) {
            temp = Integer.toHexString(aByte.toInt() and 0xFF)
            if (temp.length == 1) {
                //1 Get a one-bit complementary 0 operation
                stringBuilder.append("0")
            }
            stringBuilder.append(temp)
        }
        return stringBuilder.toString()
    }
}
