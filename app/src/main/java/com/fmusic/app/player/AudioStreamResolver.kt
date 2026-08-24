package com.fmusic.app.player

import android.content.Context
import com.fmusic.app.data.local.FMusicDatabase
import com.fmusic.app.data.model.TrackItem
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object AudioStreamResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Public reliable Piped / Invidious stream endpoints for YouTube audio resolution
    private val STREAM_PROVIDERS = listOf(
        "https://pipedapi.kavin.rocks/streams/",
        "https://api.piped.private.coffee/streams/",
        "https://pipedapi.tokhmi.xyz/streams/",
        "https://pipedapi.drgns.space/streams/"
    )

    suspend fun resolveStreamUrl(context: Context, track: TrackItem): String? = withContext(Dispatchers.IO) {
        val videoId = track.videoId ?: return@withContext null

        // 1. Check if downloaded locally
        val offlineDao = FMusicDatabase.getDatabase(context).offlineTrackDao()
        val offlineTrack = offlineDao.getOfflineTrack(videoId)
        if (offlineTrack != null && File(offlineTrack.localFilePath).exists()) {
            return@withContext offlineTrack.localFilePath
        }

        if (!track.localPath.isNullOrBlank() && File(track.localPath).exists()) {
            return@withContext track.localPath
        }

        // 2. Query Piped stream instances for direct audio stream (Opus/M4A)
        for (base in STREAM_PROVIDERS) {
            try {
                val req = Request.Builder()
                    .url("$base$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()

                val res = client.newCall(req).execute()
                if (res.isSuccessful && res.body != null) {
                    val bodyStr = res.body!!.string()
                    val json = JsonParser.parseString(bodyStr).asJsonObject
                    if (json.has("audioStreams")) {
                        val audioStreams = json.getAsJsonArray("audioStreams")
                        var bestUrl: String? = null
                        var bestBitrate = 0

                        for (elem in audioStreams) {
                            val streamObj = elem.asJsonObject
                            val url = streamObj.get("url")?.asString
                            val bitrate = streamObj.get("bitrate")?.asInt ?: 0
                            if (!url.isNullOrBlank() && bitrate >= bestBitrate) {
                                bestBitrate = bitrate
                                bestUrl = url
                            }
                        }

                        if (!bestUrl.isNullOrBlank()) {
                            return@withContext bestUrl
                        }
                    }
                }
            } catch (ignored: Exception) {
                // Try next provider
            }
        }

        // 3. Fallback to Invidious audio proxy
        try {
            val invReq = Request.Builder()
                .url("https://inv.nadeko.net/api/v1/videos/$videoId")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val res = client.newCall(invReq).execute()
            if (res.isSuccessful && res.body != null) {
                val json = JsonParser.parseString(res.body!!.string()).asJsonObject
                if (json.has("adaptiveFormats")) {
                    val formats = json.getAsJsonArray("adaptiveFormats")
                    for (fmt in formats) {
                        val obj = fmt.asJsonObject
                        val type = obj.get("type")?.asString ?: ""
                        if (type.startsWith("audio/")) {
                            val url = obj.get("url")?.asString
                            if (!url.isNullOrBlank()) return@withContext url
                        }
                    }
                }
            }
        } catch (ignored: Exception) {}

        // 4. Ultimate fallback: Return stream proxy url
        return@withContext "https://pipedproxy.tokhmi.xyz/videoplayback?id=$videoId&itag=140"
    }
}
