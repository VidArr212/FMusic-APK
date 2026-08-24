package com.fmusic.app.data.innertube

import com.fmusic.app.data.model.*
import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object InnerTubeClient {

    private const val YTM_BASE = "https://music.youtube.com/youtubei/v1"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getContextJson(): JsonObject {
        val client = JsonObject().apply {
            addProperty("clientName", "WEB_REMIX")
            addProperty("clientVersion", "1.20240101.00.00")
            addProperty("hl", "id")
            addProperty("gl", "ID")
        }
        return JsonObject().apply {
            add("client", client)
        }
    }

    private suspend fun ytPost(endpoint: String, body: JsonObject, query: String = ""): JsonObject = withContext(Dispatchers.IO) {
        val fullBody = JsonObject().apply {
            add("context", getContextJson())
            for ((k, v) in body.entrySet()) {
                add(k, v)
            }
        }

        val request = Request.Builder()
            .url("$YTM_BASE/$endpoint?prettyPrint=false$query")
            .header("Content-Type", "application/json")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
            .post(fullBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) {
            throw Exception("YTM $endpoint -> ${response.code}")
        }
        JsonParser.parseString(response.body!!.string()).asJsonObject
    }

    // Helper to extract nested text from runs or simpleText
    private fun extractText(json: JsonElement?): String {
        if (json == null || json.isJsonNull) return ""
        if (json.isJsonObject) {
            val obj = json.asJsonObject
            if (obj.has("runs")) {
                val runs = obj.getAsJsonArray("runs")
                val sb = StringBuilder()
                for (r in runs) {
                    if (r.isJsonObject && r.asJsonObject.has("text")) {
                        sb.append(r.asJsonObject.get("text").asString)
                    }
                }
                return sb.toString()
            }
            if (obj.has("simpleText")) {
                return obj.get("simpleText").asString
            }
        }
        return ""
    }

    // Helper to extract thumbnails
    private fun extractThumbnails(json: JsonElement?): String? {
        if (json == null || json.isJsonNull) return null
        val urls = mutableListOf<String>()
        fun findThumbs(el: JsonElement) {
            if (el.isJsonObject) {
                val obj = el.asJsonObject
                if (obj.has("thumbnails") && obj.get("thumbnails").isJsonArray) {
                    for (t in obj.getAsJsonArray("thumbnails")) {
                        if (t.isJsonObject && t.asJsonObject.has("url")) {
                            urls.add(t.asJsonObject.get("url").asString)
                        }
                    }
                }
                for ((_, v) in obj.entrySet()) {
                    findThumbs(v)
                }
            } else if (el.isJsonArray) {
                for (v in el.asJsonArray) findThumbs(v)
            }
        }
        findThumbs(json)
        if (urls.isEmpty()) return null
        val best = urls.last()
        return if (best.contains("googleusercontent.com")) {
            best.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
        } else best
    }

    private fun parseTwoRowItem(obj: JsonObject): TrackItem? {
        val title = extractText(obj.get("title"))
        val subtitle = extractText(obj.get("subtitle"))
        val thumbnail = extractThumbnails(obj.get("thumbnailRenderer"))

        var videoId: String? = null
        var playlistId: String? = null
        var browseId: String? = null
        var browseType: String? = "song"

        if (obj.has("navigationEndpoint")) {
            val nav = obj.getAsJsonObject("navigationEndpoint")
            if (nav.has("watchEndpoint")) {
                val we = nav.getAsJsonObject("watchEndpoint")
                videoId = we.get("videoId")?.asString
                playlistId = we.get("playlistId")?.asString
            }
            if (nav.has("browseEndpoint")) {
                val be = nav.getAsJsonObject("browseEndpoint")
                browseId = be.get("browseId")?.asString
                if (browseId != null) {
                    browseType = when {
                        browseId.startsWith("MPRE") -> "album"
                        browseId.startsWith("UC") || browseId.startsWith("MPLA") -> "artist"
                        else -> "playlist"
                    }
                }
            }
        }

        if (title.isBlank()) return null
        return TrackItem(
            videoId = videoId,
            title = title,
            subtitle = subtitle,
            artist = subtitle,
            thumbnail = thumbnail,
            type = if (videoId != null) "song" else browseType,
            browseId = browseId,
            browseType = browseType,
            playlistId = playlistId
        )
    }

    private fun parseListItem(obj: JsonObject): TrackItem? {
        var title = ""
        var subtitle = ""
        var videoId: String? = null
        var browseId: String? = null
        var duration: String? = null

        if (obj.has("flexColumns")) {
            val cols = obj.getAsJsonArray("flexColumns")
            if (cols.size() > 0) {
                val c0 = cols[0].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text")
                title = extractText(c0)
            }
            if (cols.size() > 1) {
                val c1 = cols[1].asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text")
                subtitle = extractText(c1)
            }
        }

        if (obj.has("playlistItemData")) {
            videoId = obj.getAsJsonObject("playlistItemData").get("videoId")?.asString
        }

        if (obj.has("fixedColumns")) {
            val fix = obj.getAsJsonArray("fixedColumns")
            if (fix.size() > 0) {
                val f0 = fix[0].asJsonObject.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")?.get("text")
                duration = extractText(f0).replace(".", ":")
            }
        }

        if (obj.has("navigationEndpoint")) {
            val nav = obj.getAsJsonObject("navigationEndpoint")
            if (videoId == null && nav.has("watchEndpoint")) {
                videoId = nav.getAsJsonObject("watchEndpoint").get("videoId")?.asString
            }
            if (nav.has("browseEndpoint")) {
                browseId = nav.getAsJsonObject("browseEndpoint").get("browseId")?.asString
            }
        }

        val thumbnail = extractThumbnails(obj.get("thumbnail"))
        if (title.isBlank()) return null

        return TrackItem(
            videoId = videoId,
            title = title,
            subtitle = subtitle,
            artist = subtitle,
            thumbnail = thumbnail,
            duration = duration,
            browseId = browseId,
            type = if (videoId != null) "song" else "playlist"
        )
    }

    private fun parseSections(contents: JsonArray?): List<SectionItem> {
        val sections = mutableListOf<SectionItem>()
        if (contents == null) return sections

        for (elem in contents) {
            if (!elem.isJsonObject) continue
            val sObj = elem.asJsonObject

            if (sObj.has("musicCarouselShelfRenderer")) {
                val car = sObj.getAsJsonObject("musicCarouselShelfRenderer")
                val header = extractText(car.getAsJsonObject("header")?.get("title"))
                val itemsList = mutableListOf<TrackItem>()
                if (car.has("contents")) {
                    for (c in car.getAsJsonArray("contents")) {
                        if (!c.isJsonObject) continue
                        val itemObj = c.asJsonObject
                        if (itemObj.has("musicTwoRowItemRenderer")) {
                            parseTwoRowItem(itemObj.getAsJsonObject("musicTwoRowItemRenderer"))?.let { itemsList.add(it) }
                        } else if (itemObj.has("musicResponsiveListItemRenderer")) {
                            parseListItem(itemObj.getAsJsonObject("musicResponsiveListItemRenderer"))?.let { itemsList.add(it) }
                        }
                    }
                }
                if (itemsList.isNotEmpty()) {
                    sections.add(SectionItem(title = header.ifBlank { "Rekomendasi" }, items = itemsList, isList = false))
                }
            } else if (sObj.has("musicShelfRenderer")) {
                val shelf = sObj.getAsJsonObject("musicShelfRenderer")
                val title = extractText(shelf.get("title"))
                val itemsList = mutableListOf<TrackItem>()
                if (shelf.has("contents")) {
                    for (c in shelf.getAsJsonArray("contents")) {
                        if (!c.isJsonObject) continue
                        val itemObj = c.asJsonObject
                        if (itemObj.has("musicResponsiveListItemRenderer")) {
                            parseListItem(itemObj.getAsJsonObject("musicResponsiveListItemRenderer"))?.let { itemsList.add(it) }
                        }
                    }
                }
                if (itemsList.isNotEmpty()) {
                    sections.add(SectionItem(title = title.ifBlank { "Lagu Populer" }, items = itemsList, isList = true))
                }
            }
        }
        return sections
    }

    /* ---------------- Public API Methods ---------------- */

    suspend fun getHome(): HomeResponse = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply { addProperty("browseId", "FEmusic_home") }
            val json = ytPost("browse", body)

            var contents: JsonArray? = null
            fun findSectionList(el: JsonElement) {
                if (contents != null) return
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("sectionListRenderer") && obj.getAsJsonObject("sectionListRenderer").has("contents")) {
                        contents = obj.getAsJsonObject("sectionListRenderer").getAsJsonArray("contents")
                        return
                    }
                    for ((_, v) in obj.entrySet()) findSectionList(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findSectionList(v)
                }
            }
            findSectionList(json)

            val sections = parseSections(contents)
            HomeResponse(sections = sections)
        } catch (e: Exception) {
            HomeResponse(error = e.message)
        }
    }

    suspend fun getCharts(): ChartResponse = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply { addProperty("browseId", "FEmusic_charts") }
            val json = ytPost("browse", body)

            var contents: JsonArray? = null
            fun findSectionList(el: JsonElement) {
                if (contents != null) return
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("sectionListRenderer") && obj.getAsJsonObject("sectionListRenderer").has("contents")) {
                        contents = obj.getAsJsonObject("sectionListRenderer").getAsJsonArray("contents")
                        return
                    }
                    for ((_, v) in obj.entrySet()) findSectionList(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findSectionList(v)
                }
            }
            findSectionList(json)

            val sections = parseSections(contents)
            ChartResponse(sections = sections)
        } catch (e: Exception) {
            ChartResponse(error = e.message)
        }
    }

    suspend fun getMoods(): MoodResponse = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply { addProperty("browseId", "FEmusic_moods_and_genres") }
            val json = ytPost("browse", body)

            val categories = mutableListOf<MoodCategory>()
            fun findButtons(el: JsonElement) {
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("musicNavigationButtonRenderer")) {
                        val btn = obj.getAsJsonObject("musicNavigationButtonRenderer")
                        val title = extractText(btn.get("buttonText"))
                        var color: String? = null
                        if (btn.has("solid") && btn.getAsJsonObject("solid").has("leftStripeColor")) {
                            val colorLong = btn.getAsJsonObject("solid").get("leftStripeColor").asLong
                            color = "#" + java.lang.Long.toHexString(colorLong).padStart(8, '0').takeLast(6)
                        }
                        var browseId: String? = null
                        var params: String? = null
                        if (btn.has("clickCommand") && btn.getAsJsonObject("clickCommand").has("browseEndpoint")) {
                            val be = btn.getAsJsonObject("clickCommand").getAsJsonObject("browseEndpoint")
                            browseId = be.get("browseId")?.asString
                            params = be.get("params")?.asString
                        }
                        if (!browseId.isNullOrBlank() && title.isNotBlank()) {
                            categories.add(MoodCategory(title = title, color = color, browseId = browseId, params = params))
                        }
                    }
                    for ((_, v) in obj.entrySet()) findButtons(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findButtons(v)
                }
            }
            findButtons(json)

            MoodResponse(categories = categories)
        } catch (e: Exception) {
            MoodResponse(error = e.message)
        }
    }

    suspend fun search(query: String, filter: String? = null): SearchResponse = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply {
                addProperty("query", query)
                if (filter != null) {
                    val param = when (filter) {
                        "songs" -> "EgWKAQIIAWoMEA4QChADEAQQCRAF"
                        "videos" -> "EgWKAQIQAWoMEA4QChADEAQQCRAF"
                        "albums" -> "EgWKAQIYAWoMEA4QChADEAQQCRAF"
                        "artists" -> "EgWKAQIgAWoMEA4QChADEAQQCRAF"
                        "playlists" -> "EgeKAQQoAEABagwQDhAKEAMQBBAJEAU="
                        else -> null
                    }
                    if (param != null) addProperty("params", param)
                }
            }
            val json = ytPost("search", body)

            val itemsList = mutableListOf<TrackItem>()
            fun findListItems(el: JsonElement) {
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        parseListItem(obj.getAsJsonObject("musicResponsiveListItemRenderer"))?.let { itemsList.add(it) }
                    }
                    for ((_, v) in obj.entrySet()) findListItems(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findListItems(v)
                }
            }
            findListItems(json)

            val sections = if (itemsList.isNotEmpty()) {
                listOf(SectionItem(title = "Hasil Pencarian", items = itemsList, isList = true))
            } else emptyList()

            SearchResponse(sections = sections)
        } catch (e: Exception) {
            SearchResponse(error = e.message)
        }
    }

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val body = JsonObject().apply { addProperty("input", query) }
            val json = ytPost("music/get_search_suggestions", body)

            val suggestions = mutableListOf<String>()
            fun findSuggs(el: JsonElement) {
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("searchSuggestionRenderer")) {
                        val s = obj.getAsJsonObject("searchSuggestionRenderer")
                        val text = extractText(s.get("suggestion"))
                        if (text.isNotBlank()) suggestions.add(text)
                    }
                    for ((_, v) in obj.entrySet()) findSuggs(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findSuggs(v)
                }
            }
            findSuggs(json)
            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun browse(rawId: String, params: String? = null): BrowseResponse = withContext(Dispatchers.IO) {
        try {
            var id = rawId
            if (Regex("^(PL|RDCLAK|VLPL|OLAK)").containsMatchIn(id) && !id.startsWith("VL")) {
                id = "VL$id"
            }
            val body = JsonObject().apply {
                addProperty("browseId", id)
                if (params != null) addProperty("params", params)
            }
            val json = ytPost("browse", body)

            // Tracks
            val tracks = mutableListOf<TrackItem>()
            fun findTracks(el: JsonElement) {
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("musicResponsiveListItemRenderer")) {
                        parseListItem(obj.getAsJsonObject("musicResponsiveListItemRenderer"))?.let { tracks.add(it) }
                    }
                    for ((_, v) in obj.entrySet()) findTracks(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findTracks(v)
                }
            }
            findTracks(json)

            // Header
            var headerTitle = ""
            var headerSubtitle = ""
            var headerThumb: String? = null

            fun findHeader(el: JsonElement) {
                if (headerTitle.isNotBlank()) return
                if (el.isJsonObject) {
                    val obj = el.asJsonObject
                    if (obj.has("musicResponsiveHeaderRenderer") || obj.has("musicDetailHeaderRenderer")) {
                        val h = (obj.get("musicResponsiveHeaderRenderer") ?: obj.get("musicDetailHeaderRenderer")).asJsonObject
                        headerTitle = extractText(h.get("title"))
                        headerSubtitle = extractText(h.get("subtitle"))
                        headerThumb = extractThumbnails(h)
                        return
                    }
                    for ((_, v) in obj.entrySet()) findHeader(v)
                } else if (el.isJsonArray) {
                    for (v in el.asJsonArray) findHeader(v)
                }
            }
            findHeader(json)

            if (headerThumb == null && tracks.isNotEmpty()) {
                headerThumb = tracks.first().thumbnail
            }

            BrowseResponse(
                header = BrowseHeader(
                    title = headerTitle.ifBlank { "Detail Album" },
                    subtitle = headerSubtitle,
                    thumbnail = headerThumb
                ),
                tracks = tracks
            )
        } catch (e: Exception) {
            BrowseResponse(error = e.message)
        }
    }

    suspend fun getLyrics(title: String, artist: String, duration: Long? = null): LyricsResponse = withContext(Dispatchers.IO) {
        try {
            // 1. Direct query to LRCLIB (synced lyrics provider)
            val encTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encArtist = java.net.URLEncoder.encode(artist, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "FMusic/1.0")
                .build()

            val res = httpClient.newCall(req).execute()
            if (res.isSuccessful && res.body != null) {
                val json = JsonParser.parseString(res.body!!.string()).asJsonObject
                val synced = json.get("syncedLyrics")?.asString
                val plain = json.get("plainLyrics")?.asString
                if (!synced.isNullOrBlank() || !plain.isNullOrBlank()) {
                    return@withContext LyricsResponse(synced = synced, plain = plain, source = "LRCLIB")
                }
            }

            // 2. NetEase / Secondary fallback
            LyricsResponse(synced = null, plain = null, source = null)
        } catch (e: Exception) {
            LyricsResponse(error = e.message)
        }
    }
}
