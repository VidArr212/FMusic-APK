package com.fmusic.app.player

import android.content.Context
import com.fmusic.app.data.local.FMusicDatabase
import com.fmusic.app.data.model.TrackItem
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AudioStreamResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Piped instances - ranked by reliability
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.private.coffee",
        "https://pipedapi.tokhmi.xyz",
        "https://piped-api.garudalinux.org",
        "https://pipedapi.drgns.space",
        "https://eu.piped.yt/api"
    )

    // Invidious instances for fallback
    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.privacydev.net",
        "https://invidious.fdn.fr",
        "https://yt.cdaut.de"
    )

    suspend fun resolveStreamUrl(context: Context, track: TrackItem): String? = withContext(Dispatchers.IO) {
        val videoId = track.videoId ?: return@withContext null

        // 1. Check offline first (no network needed)
        try {
            val offlineDao = FMusicDatabase.getDatabase(context).offlineTrackDao()
            val offlineTrack = offlineDao.getOfflineTrack(videoId)
            if (offlineTrack != null && File(offlineTrack.localFilePath).exists()) {
                return@withContext offlineTrack.localFilePath
            }
        } catch (ignored: Exception) {}

        if (!track.localPath.isNullOrBlank() && File(track.localPath).exists()) {
            return@withContext track.localPath
        }

        // 2. Try Piped instances for best audio stream
        for (base in PIPED_INSTANCES) {
            val url = tryPiped(base, videoId)
            if (!url.isNullOrBlank()) return@withContext url
        }

        // 3. Try Invidious instances
        for (base in INVIDIOUS_INSTANCES) {
            val url = tryInvidious(base, videoId)
            if (!url.isNullOrBlank()) return@withContext url
        }

        // 4. Try cobalt.tools API (another option)
        val cobaltUrl = tryCobalt(videoId)
        if (!cobaltUrl.isNullOrBlank()) return@withContext cobaltUrl

        return@withContext null
    }

    private fun tryPiped(baseUrl: String, videoId: String): String? {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/streams/$videoId")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .header("Accept", "application/json")
                .build()

            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null

            val json = JsonParser.parseString(body)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject

            // Check for error
            if (obj.has("message") && !obj.has("audioStreams")) return null

            val audioStreams = obj.getAsJsonArray("audioStreams") ?: return null

            // Pick highest quality audio-only stream (prefer opus/ogg, then m4a)
            var bestUrl: String? = null
            var bestBitrate = 0
            var opusUrl: String? = null
            var opusBitrate = 0

            for (elem in audioStreams) {
                if (!elem.isJsonObject) continue
                val s = elem.asJsonObject
                val url = s.get("url")?.asString ?: continue
                if (url.isBlank()) continue
                val bitrate = s.get("bitrate")?.asInt ?: 0
                val mimeType = s.get("mimeType")?.asString ?: ""
                val codec = s.get("codec")?.asString ?: ""

                if (mimeType.contains("opus", ignoreCase = true) || codec.contains("opus", ignoreCase = true)) {
                    if (bitrate >= opusBitrate) {
                        opusBitrate = bitrate
                        opusUrl = url
                    }
                } else if (bitrate >= bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = url
                }
            }

            // Prefer m4a/mp4 over opus for ExoPlayer compatibility on some devices
            return bestUrl ?: opusUrl
        } catch (e: Exception) {
            null
        }
    }

    private fun tryInvidious(baseUrl: String, videoId: String): String? {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/videos/$videoId?fields=adaptiveFormats")
                .header("User-Agent", "FMusic/1.0")
                .build()

            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            val body = res.body?.string() ?: return null

            val json = JsonParser.parseString(body)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject

            val formats = obj.getAsJsonArray("adaptiveFormats") ?: return null

            var bestUrl: String? = null
            var bestBitrate = 0

            for (elem in formats) {
                if (!elem.isJsonObject) continue
                val fmt = elem.asJsonObject
                val type = fmt.get("type")?.asString ?: ""
                if (!type.startsWith("audio/")) continue
                val url = fmt.get("url")?.asString ?: continue
                if (url.isBlank()) continue
                val bitrate = fmt.get("bitrate")?.asInt ?: 0
                if (bitrate >= bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = url
                }
            }
            bestUrl
        } catch (e: Exception) {
            null
        }
    }

    private fun tryCobalt(videoId: String): String? {
        return try {
            val reqBody = """{"url":"https://www.youtube.com/watch?v=$videoId","aFormat":"mp3","isAudioOnly":true}"""
            val body = reqBody.toByteArray()

            val req = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FMusic/1.0")
                .post(okhttp3.RequestBody.create("application/json".toMediaType(), body))
                .build()

            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            val resBody = res.body?.string() ?: return null

            val json = JsonParser.parseString(resBody)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject

            obj.get("url")?.asString
        } catch (e: Exception) {
            null
        }
    }
}

private fun String.toMediaType() = okhttp3.MediaType.Companion.parse(this)!!
