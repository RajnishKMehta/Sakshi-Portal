/*
 * Copyright 2026 Rajnish Kumar
 * SPDX-License-Identifier: Apache-2.0
 */
package rajnishkmehta.sakshi.portal.debug

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A specialized logger used exclusively in debug builds for the Sakshi Portal application.
 *
 * This object manages the categorization and persistent storage of logs to the internal storage.
 * It writes all logs to an `all_logs.txt` file and categorizes them into `info_logs.txt`,
 * `warning_logs.txt`, and `error_logs.txt` based on the log level.
 */
object DebugLogger {
    private const val TAG = "SakshiPortal"
    @Volatile
    private var logsDir: File? = null

    /**
     * Initializes the debug logger and creates the destination directory for log files if it does not exist.
     *
     * @param context The application context used to resolve the external files directory.
     */
    @JvmStatic
    fun init(context: Context) {
        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir != null) {
            val dir = File(externalFilesDir, "debug_logs")
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    logsDir = dir
                } else {
                    logsDir = null
                }
            } else if (dir.isDirectory) {
                logsDir = dir
            } else {
                logsDir = null
            }
        } else {
            logsDir = null
        }
    }

    @JvmStatic
    fun d(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
        writeToFile("debug_logs.txt", "DEBUG", tag, message, throwable)
        writeToFile("all_logs.txt", "DEBUG", tag, message, throwable)
    }

    @JvmStatic
    fun i(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.i(tag, message, throwable)
        } else {
            Log.i(tag, message)
        }
        writeToFile("info_logs.txt", "INFO", tag, message, throwable)
        writeToFile("all_logs.txt", "INFO", tag, message, throwable)
    }

    @JvmStatic
    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
        writeToFile("warning_logs.txt", "WARN", tag, message, throwable)
        writeToFile("all_logs.txt", "WARN", tag, message, throwable)
    }

    @JvmStatic
    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        writeToFile("error_logs.txt", "ERROR", tag, message, throwable)
        writeToFile("all_logs.txt", "ERROR", tag, message, throwable)
    }

    @JvmStatic
    @Synchronized
    private fun writeToFile(fileName: String, level: String, tag: String, message: String, throwable: Throwable? = null) {
        val dir = logsDir ?: return
        try {
            var file = File(dir, fileName)
            if (file.exists() && file.length() >= 512 * 1024) {
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                val extWithDot = if (ext.isNotEmpty()) ".$ext" else ""

                var n = 1
                var rotatedFile = File(dir, "${nameWithoutExt}_$n$extWithDot")
                while (rotatedFile.exists()) {
                    n++
                    rotatedFile = File(dir, "${nameWithoutExt}_$n$extWithDot")
                }
                file.renameTo(rotatedFile)
                file = File(dir, fileName)
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val logLine = "$timestamp [$level] $tag: $message\n"

            FileOutputStream(file, true).use {
                it.write(logLine.toByteArray())
                throwable?.let { t ->
                    it.write((Log.getStackTraceString(t) + "\n").toByteArray())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to log file: $fileName", e)
        }
    }

    @JvmStatic
    fun getLogFiles(): List<File> {
        return logsDir?.listFiles()?.toList() ?: emptyList()
    }

    @JvmStatic
    fun clearLogs(): Boolean {
        val files = logsDir?.listFiles()
        if (files == null || files.isEmpty()) {
            return true
        }
        val rotatedPattern = Regex(".*_\\d+\\.txt$")
        var allDeleted = true
        for (file in files) {
            if (!rotatedPattern.matches(file.name)) {
                if (!file.delete()) {
                    allDeleted = false
                }
            }
        }
        return allDeleted
    }
}
