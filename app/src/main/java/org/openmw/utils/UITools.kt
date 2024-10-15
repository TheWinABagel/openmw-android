package org.openmw.utils

import android.os.Environment
import android.content.Context
import android.os.StatFs

fun getAvailableStorageSpace(context: Context): Long {
    val storageDirectory = Environment.getExternalStorageDirectory()
    val stat = StatFs(storageDirectory.toString())
    val availableBytes = stat.availableBytes
    return availableBytes
}
/*
private fun humanReadableByteCountBin(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp-1] + "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}



runnable = Runnable {
    val memoryInfo = ActivityManager.MemoryInfo()
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.getMemoryInfo(memoryInfo)

    val totalMemory = memoryInfo.totalMem
    val availableMemory = memoryInfo.availMem
    val usedMemory = totalMemory - availableMemory

    textView.text = "Total memory: ${humanReadableByteCountBin(totalMemory)}\n" +
            "Available memory: ${humanReadableByteCountBin(availableMemory)}\n" +
            "Used memory: ${humanReadableByteCountBin(usedMemory)}"

    handler.postDelayed(runnable, 1000)
}

handler.post(runnable)
}



 */