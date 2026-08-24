package com.fmusic.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val changelog: String,
    val apkDownloadUrl: String?
)

sealed class UpdateProgress {
    object Idle : UpdateProgress()
    data class Downloading(val percent: Int) : UpdateProgress()
    object ReadyToInstall : UpdateProgress()
    data class Error(val message: String) : UpdateProgress()
}

object AppUpdateManager {

    val CURRENT_VERSION: String get() = com.fmusic.app.BuildConfig.VERSION_NAME
    private const val GITHUB_REPO_API = "https://api.github.com/repos/VidArr212/FMusic-APK/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(GITHUB_REPO_API)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "FMusic-App")
                .build()

            val res = httpClient.newCall(req).execute()
            if (!res.isSuccessful || res.body == null) {
                return@withContext UpdateInfo(false, CURRENT_VERSION, "", null)
            }

            val json = JsonParser.parseString(res.body!!.string()).asJsonObject
            val tagName = json.get("tag_name")?.asString ?: ""
            val cleanLatestVer = tagName.replace("v", "").trim()
            val bodyNotes = json.get("body")?.asString ?: "Perbaikan performa dan pembaruan fitur."

            var apkUrl: String? = null
            if (json.has("assets")) {
                val assets = json.getAsJsonArray("assets")
                for (a in assets) {
                    val obj = a.asJsonObject
                    val name = obj.get("name")?.asString ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = obj.get("browser_download_url")?.asString
                        break
                    }
                }
            }

            val hasUpdate = isNewerVersion(cleanLatestVer, CURRENT_VERSION)
            UpdateInfo(
                hasUpdate = hasUpdate,
                latestVersion = cleanLatestVer,
                changelog = bodyNotes,
                apkDownloadUrl = apkUrl
            )
        } catch (e: Exception) {
            UpdateInfo(false, CURRENT_VERSION, "", null)
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest.isBlank() || latest == current) return false
        val lParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(lParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val l = lParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (UpdateProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress(UpdateProgress.Downloading(5))

            val updateDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(updateDir, "FMusic_Update.apk")
            if (apkFile.exists()) apkFile.delete()

            val req = Request.Builder().url(apkUrl).build()
            val res = httpClient.newCall(req).execute()

            if (!res.isSuccessful || res.body == null) {
                onProgress(UpdateProgress.Error("Gagal mengunduh file update"))
                return@withContext
            }

            val body = res.body!!
            val totalBytes = body.contentLength()
            var bytesCopied = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (totalBytes > 0) {
                            val percent = ((bytesCopied.toFloat() / totalBytes) * 100).toInt()
                            onProgress(UpdateProgress.Downloading(percent.coerceIn(5, 98)))
                        }
                        bytes = input.read(buffer)
                    }
                }
            }

            onProgress(UpdateProgress.ReadyToInstall)

            // Trigger In-App Installer via FileProvider
            installApk(context, apkFile)

        } catch (e: Exception) {
            onProgress(UpdateProgress.Error(e.message ?: "Gagal mengunduh update"))
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }
}
