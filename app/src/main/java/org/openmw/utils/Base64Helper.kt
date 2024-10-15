package org.openmw.utils

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.IOException


object Base64Encoder {
    fun encodeFileToBase64(filePath: String): String {
        val file = File(filePath)
        val fileBytes = ByteArray(file.length().toInt())
        try {
            FileInputStream(file).use { fis ->
                fis.read(fileBytes)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Base64.encodeToString(fileBytes, Base64.DEFAULT)
    }
}
