package com.fmusic.app.player

import android.content.Context
import com.fmusic.app.data.model.TrackItem
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
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
        "https://pipedapi.drgns.space"
    )

    // Invidious instances for fallback
    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.privacydev.net",
        "https://invidious.fdn.fr",
        "https://yt.cdaut.de"
    )

    suspend fun resolveStreamUrl(context: Context? = null, track: TrackItem): String? = withContext(Dispatchers.IO) {
        val videoId = track.videoId ?: return@withContext null

        // 1. Check local file first (offline mode)
        if (!track.localPath.isNullOrBlank() && File(track.localPath).exists()) {
            return@withContext track.localPath
        }

        if (context != null) {
            val cachedFile = File(context.cacheDir, "audio_cache/${videoId}.m4a")
            if (cachedFile.exists()) return@withContext cachedFile.absolutePath
        }

        // 2. Try Piped instances
        for (base in PIPED_INSTANCES) {
            val url = tryPiped(base, videoId)
            if (!url.isNullOrBlank()) return@withContext url
        }

        // 3. Try Invidious instances
        for (base in INVIDIOUS_INSTANCES) {
            val url = tryInvidious(base, videoId)
            if (!url.isNullOrBlank()) return@withContext url
        }

        // 4. Try cobalt.tools as last resort
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

            // If error/no audioStreams field, skip
            if (!obj.has("audioStreams")) return null

            val audioStreams = obj.getAsJsonArray("audioStreams") ?: return null

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
                    if (bitrate >= opusBitrate) { opusBitrate = bitrate; opusUrl = url }
                } else {
                    if (bitrate >= bestBitrate) { bestBitrate = bitrate; bestUrl = url }
                }
            }

            bestUrl ?: opusUrl
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
                if (bitrate >= bestBitrate) { bestBitrate = bitrate; bestUrl = url }
            }
            bestUrl
        } catch (e: Exception) {
            null
        }
    }

    private fun tryCobalt(videoId: String): String? {
        return try {
            val reqBodyStr = """{"url":"https://www.youtube.com/watch?v=$videoId","aFormat":"mp3","isAudioOnly":true}"""
            val requestBody = reqBodyStr.toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .header("Accept", "application/json")
                .header("User-Agent", "FMusic/1.0")
                .post(requestBody)
                .build()

            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            val resBody = res.body?.string() ?: return null

            val json = JsonParser.parseString(resBody)
            if (!json.isJsonObject) return null
            json.asJsonObject.get("url")?.asString
        } catch (e: Exception) {
            null
        }
    }
}
