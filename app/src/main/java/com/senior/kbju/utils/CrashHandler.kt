package com.senior.kbju.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Офлайн-логгер сбоев.
 * Сохраняет стек-трейс падения в локальный файл, чтобы пожилой пользователь или его родные
 * могли в один клик отправить отчёт разработчику без необходимости подключать тяжелые Google-сервисы.
 */
object CrashHandler {

    private const val CRASH_FILE_NAME = "last_crash.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashReport(context, thread, throwable)
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val report = buildString {
            appendLine("=== ОТЧЁТ О СБОЕ ПРИЛОЖЕНИЯ «КБЖУ ДЛЯ СТАРШИХ» ===")
            appendLine("Дата: ${dateFormat.format(Date())}")
            appendLine("Устройство: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Поток: ${thread.name}")
            appendLine()
            appendLine("Стек ошибки:")
            appendLine(stackTrace)
        }

        val file = File(context.filesDir, CRASH_FILE_NAME)
        file.writeText(report)
    }

    fun getLastCrashReport(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) {
            file.readText().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    fun clearCrashReport(context: Context) {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}
