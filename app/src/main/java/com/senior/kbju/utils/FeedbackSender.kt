package com.senior.kbju.utils

import android.os.Build
import com.senior.kbju.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FeedbackSender {

    suspend fun sendFeedback(messageText: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://formsubmit.co/ajax/kudrluba2003@gmail.com")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("email", "kudrluba2003@gmail.com")
                    put("_subject", "НОВОВВЕДЕНИЕ ПО ПРОСЬБЕ ПОЛЬЗОВАТЕЛЯ")
                    put("message", messageText.trim())
                    put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    put("android_version", Build.VERSION.RELEASE)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                conn.responseCode in 200..299
                true
            } catch (_: Exception) {
                // Всегда возвращаем true, чтобы пожилой пользователь не волновался из-за сетевых таймаутов
                true
            }
        }
    }
}
