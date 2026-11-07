package com.senior.kbju.utils

import android.os.Build
import com.senior.kbju.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class FeedbackSendResult {
    data object Success : FeedbackSendResult()
    data class Failure(val errorMessage: String) : FeedbackSendResult()
}

object FeedbackSender {

    // Резервный URL: если у разработчика настроен Telegram бот или Google Форма / Webhook
    // Можно переопределить через BuildConfig или константу
    var customWebhookUrl: String? = null

    suspend fun sendFeedback(messageText: String): FeedbackSendResult {
        return withContext(Dispatchers.IO) {
            val trimmed = messageText.trim()
            if (trimmed.isBlank()) {
                return@withContext FeedbackSendResult.Failure("Текст отзыва пуст")
            }

            // 1. Если настроен собственный Webhook (Google Form, Telegram Bot, Cloudflare Worker)
            val webhook = customWebhookUrl
            if (!webhook.isNullOrBlank()) {
                try {
                    val success = postJson(
                        webhook,
                        JSONObject().apply {
                            put("message", trimmed)
                            put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            put("android_version", Build.VERSION.RELEASE)
                        }
                    )
                    if (success) return@withContext FeedbackSendResult.Success
                } catch (e: Exception) {
                    // Fallthrough to standard channels
                }
            }

            // 2. Пробуем отправить через доступные открытые шлюзы
            val gateways = listOf(
                "https://api.form-data.com/f/kudrluba_senior_kbju",
                "https://formsubmit.co/ajax/kudrluba2003@gmail.com"
            )

            var lastError = "Не удалось доставить отзыв через интернет. Проверьте подключение."

            for (endpoint in gateways) {
                try {
                    val url = URL(endpoint)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "SeniorKbju/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})")
                        connectTimeout = 8000
                        readTimeout = 8000
                        doOutput = true
                    }

                    val payload = JSONObject().apply {
                        put("email", "kudrluba2003@gmail.com")
                        put("_subject", "Отзыв SeniorKbju от пользователя")
                        put("message", trimmed)
                        put("app_version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        put("android_version", Build.VERSION.RELEASE)
                    }

                    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }

                    val code = conn.responseCode
                    if (code in 200..299) {
                        return@withContext FeedbackSendResult.Success
                    } else {
                        lastError = "Сервер ответил кодом $code"
                    }
                } catch (e: Exception) {
                    lastError = e.localizedMessage ?: "Ошибка сети"
                }
            }

            FeedbackSendResult.Failure(lastError)
        }
    }

    private fun postJson(endpoint: String, json: JSONObject): Boolean {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
        }
        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(json.toString())
            writer.flush()
        }
        return conn.responseCode in 200..299
    }
}
