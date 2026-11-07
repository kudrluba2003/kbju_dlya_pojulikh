package com.senior.kbju.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateInstaller {

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val percent: Int) : DownloadState()
        data class ReadyToInstall(val file: File) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (DownloadState) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                onProgress(DownloadState.Downloading(0))
                val apkFile = File(context.cacheDir, "senior_kbju_update.apk")
                if (apkFile.exists()) apkFile.delete()

                var currentUrl = downloadUrl
                var redirects = 0
                var conn: HttpURLConnection? = null

                while (redirects < 5) {
                    if (!currentUrl.startsWith("https://", ignoreCase = true)) {
                        throw SecurityException("Небезопасный протокол: разрешён только HTTPS")
                    }
                    val u = URL(currentUrl)
                    conn = (u.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = false
                    }
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val newLocation = conn.getHeaderField("Location") ?: break
                        conn.disconnect()
                        currentUrl = newLocation
                        redirects++
                    } else {
                        break
                    }
                }

                val finalConn = conn ?: throw IllegalStateException("Не удалось подключиться к серверу")
                val totalLength = finalConn.contentLengthLong
                var bytesReadTotal = 0L

                finalConn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(16384)
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesReadTotal += bytes
                            val percent = if (totalLength > 0) ((bytesReadTotal * 100) / totalLength).toInt() else -1
                            onProgress(DownloadState.Downloading(percent))
                            bytes = input.read(buffer)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onProgress(DownloadState.ReadyToInstall(apkFile))
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onProgress(DownloadState.Error("Не удалось скачать: ${e.localizedMessage ?: "ошибка сети"}"))
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
