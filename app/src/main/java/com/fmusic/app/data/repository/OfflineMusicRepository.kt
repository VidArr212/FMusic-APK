package com.fmusic.app.data.repository

import android.content.Context
import com.fmusic.app.data.api.ApiClient
import com.fmusic.app.data.local.FMusicDatabase
import com.fmusic.app.data.local.entity.OfflineTrackEntity
import com.fmusic.app.data.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val percentage: Int, val message: String) : DownloadState()
    data class Success(val localPath: String) : DownloadState()
    data class Error(val error: String) : DownloadState()
}

class OfflineMusicRepository(private val context: Context) {

    private val api = ApiClient.getService(context)
    private val db = FMusicDatabase.getDatabase(context)
    private val offlineDao = db.offlineTrackDao()
    private val httpClient = OkHttpClient()

    suspend fun downloadTrack(
        track: TrackItem,
        onProgress: (DownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val videoId = track.videoId ?: run {
            onProgress(DownloadState.Error("Invalid video ID"))
            return@withContext
        }

        try {
            onProgress(DownloadState.Progress(10, "Starting download converter..."))

            // Step 1: Call /api/download-start
            val startRes = api.startDownload(videoId)
            if (!startRes.isSuccessful || startRes.body()?.progressUrl == null) {
                // Fallback direct download if available or error
                onProgress(DownloadState.Error(startRes.body()?.error ?: "Failed to initialize converter"))
                return@withContext
            }

            val progressUrl = startRes.body()!!.progressUrl!!
            var downloadUrl: String? = null
            var attempts = 0

            // Step 2: Poll /api/download-progress
            while (downloadUrl == null && attempts < 30) {
                delay(2000)
                attempts++
                val pollRes = api.pollDownloadProgress(progressUrl)
                if (pollRes.isSuccessful && pollRes.body() != null) {
                    val body = pollRes.body()!!
                    val progressVal = (body.progress ?: 0) / 10 // 0-100
                    onProgress(DownloadState.Progress(20 + (progressVal * 0.4).toInt(), body.text ?: "Converting audio..."))

                    if (body.done == true && !body.url.isNullOrBlank()) {
                        downloadUrl = body.url
                        break
                    }
                }
            }

            if (downloadUrl == null) {
                onProgress(DownloadState.Error("Conversion timeout or server busy"))
                return@withContext
            }

            // Step 3: Download MP3 file to local storage
            onProgress(DownloadState.Progress(65, "Downloading MP3 file..."))
            val musicDir = File(context.filesDir, "offline_music")
            if (!musicDir.exists()) musicDir.mkdirs()

            val safeFileName = "${videoId}_${System.currentTimeMillis()}.mp3"
            val targetFile = File(musicDir, safeFileName)

            val request = Request.Builder().url(downloadUrl).build()
            val downloadRes = httpClient.newCall(request).execute()

            if (!downloadRes.isSuccessful || downloadRes.body == null) {
                onProgress(DownloadState.Error("Failed to fetch audio stream"))
                return@withContext
            }

            val body = downloadRes.body!!
            val totalBytes = body.contentLength()
            var bytesCopied = 0L

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        if (totalBytes > 0) {
                            val percent = 65 + ((bytesCopied.toFloat() / totalBytes) * 30).toInt()
                            onProgress(DownloadState.Progress(percent, "Saving to storage..."))
                        }
                        bytes = input.read(buffer)
                    }
                }
            }

            // Step 4: Save to Room DB
            val entity = OfflineTrackEntity(
                videoId = videoId,
                title = track.title ?: "Unknown Title",
                artist = track.getDisplayArtist(),
                thumbnail = track.thumbnail,
                duration = track.duration,
                localFilePath = targetFile.absolutePath,
                fileSize = targetFile.length(),
                downloadedAt = System.currentTimeMillis()
            )
            offlineDao.insertOfflineTrack(entity)

            onProgress(DownloadState.Success(targetFile.absolutePath))

        } catch (e: Exception) {
            onProgress(DownloadState.Error(e.message ?: "Unknown error downloading audio"))
        }
    }
}
