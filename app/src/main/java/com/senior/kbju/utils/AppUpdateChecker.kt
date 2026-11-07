package com.senior.kbju.utils

import com.senior.kbju.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?
)

object AppUpdateChecker {

    const val GITHUB_REPO = "kudrluba2003/kbju_dlya_pojulikh"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkLatestRelease(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(LATEST_RELEASE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SeniorKbjuApp")
            }

            if (connection.responseCode != 200) {
                return@withContext checkLatestReleaseViaRedirect()
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val rawTag = json.optString("tag_name", "").trim()
            val tagName = rawTag.removePrefix("v").trim()
            val title = json.optString("name", "Новое обновление").ifBlank { "Версия $rawTag" }
            val releaseNotes = json.optString("body", "").trim()
            val htmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")

            // Находим APK среди прикреплённых файлов релиза
            var apkDownloadUrl: String? = null
            if (json.has("assets")) {
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkDownloadUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            if (apkDownloadUrl.isNullOrBlank()) {
                val tagFormatted = if (rawTag.startsWith("v")) rawTag else "v$rawTag"
                apkDownloadUrl = "https://github.com/$GITHUB_REPO/releases/download/$tagFormatted/app-release.apk"
            }

            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            if (isNewerVersion(remote = tagName, current = currentVersion)) {
                AppUpdateInfo(
                    tagName = tagName,
                    title = title,
                    releaseNotes = releaseNotes,
                    releaseUrl = htmlUrl,
                    apkDownloadUrl = apkDownloadUrl
                )
            } else {
                null
            }
        } catch (_: Exception) {
            checkLatestReleaseViaRedirect()
        }
    }

    private fun checkLatestReleaseViaRedirect(): AppUpdateInfo? {
        try {
            val url = URL("https://github.com/$GITHUB_REPO/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "HEAD"
                setRequestProperty("User-Agent", "SeniorKbjuApp")
            }
            val location = conn.getHeaderField("Location") ?: return null
            val rawTag = location.substringAfterLast("/tag/", "").trim()
            if (rawTag.isBlank()) return null
            val tagName = rawTag.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            if (isNewerVersion(remote = tagName, current = currentVersion)) {
                return AppUpdateInfo(
                    tagName = tagName,
                    title = "Версия $rawTag",
                    releaseNotes = "Новая версия приложения доступна на GitHub.",
                    releaseUrl = location,
                    apkDownloadUrl = "https://github.com/$GITHUB_REPO/releases/download/$rawTag/app-release.apk"
                )
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Сравнивает семантические версии (например, "1.1" > "1.0", "1.0.1" > "1.0").
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val rParts = remote.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
        val cParts = current.removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
