package org.openmw.utils

import android.content.Context
import android.os.Build
import org.openmw.Constants
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CaptureCrash : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Save crash log to a file
        saveCrashLog(throwable)

        // Terminate the app or perform any other necessary action
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }

    private fun saveCrashLog(throwable: Throwable) {
        try {
            val logFile = File(Constants.CRASH_FILE)
            if (!logFile.exists())
                logFile.createNewFile()


            FileWriter(logFile, true).use { writer ->
                writer.append("Device: ${Build.MODEL} (API ${Build.VERSION.SDK_INT})\n")
                writer.append("${getCurrentDateTime()}:\t")
                printFullStackTrace(throwable, PrintWriter(writer))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printFullStackTrace(throwable: Throwable, printWriter: PrintWriter) {
        printWriter.println(throwable.toString())
        throwable.stackTrace.forEach { element ->
            printWriter.print("\t $element \n")
        }
        val cause = throwable.cause
        if (cause != null) {
            printWriter.print("Caused by:\t")
            printFullStackTrace(cause, printWriter)
        }
        printWriter.print("\n")
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}

class LogCat(val context: Context) {
    fun enableLogcat() {
        val logcatFile = File(Constants.LOGCAT_FILE)
        if (logcatFile.exists()) {
            logcatFile.delete()
        }

        val processBuilder = ProcessBuilder()
        val commandToExecute = arrayOf(
            "/system/bin/sh",
            "-c",
            "logcat *:W -d -f ${Constants.LOGCAT_FILE}"
        )
        processBuilder.command(*commandToExecute)
        processBuilder.redirectErrorStream(true)
        processBuilder.start()
    }
}