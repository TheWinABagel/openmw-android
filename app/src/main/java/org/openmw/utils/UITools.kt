package org.openmw.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import org.openmw.ui.overlay.MemoryInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.ln
import kotlin.math.pow

fun getAvailableStorageSpace(context: Context): String {
    val storageDirectory = Environment.getExternalStorageDirectory()
    val stat = StatFs(storageDirectory.toString())
    val availableBytes = stat.availableBytes
    return humanReadableByteCountBin(availableBytes)
}

fun getMemoryInfo(context: Context): MemoryInfo {
    val memoryInfo = ActivityManager.MemoryInfo()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.getMemoryInfo(memoryInfo)
    val totalMemory = humanReadableByteCountBin(memoryInfo.totalMem)
    val availableMemory = humanReadableByteCountBin(memoryInfo.availMem)
    val usedMemory = humanReadableByteCountBin(memoryInfo.totalMem - memoryInfo.availMem)

    return MemoryInfo(totalMemory, availableMemory, usedMemory)
}

@SuppressLint("DefaultLocale")
fun humanReadableByteCountBin(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "i"
    return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
}

fun getBatteryStatus(context: Context): String {
    val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0
    val batteryPct = (level / scale.toFloat()) * 100

    return "Battery: ${batteryPct.toInt()}%${if (isCharging) " (Charging)" else ""}"
}

fun getMessages(): List<String> {
    val logMessages = mutableListOf<String>()
    try {
        val process = Runtime.getRuntime().exec("logcat -d")
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            logMessages.add(line!!)
        }
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return logMessages
}
