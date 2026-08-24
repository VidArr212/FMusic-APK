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

/**
 * Embedded YouTube Music InnerTube client.
 * Mirrors the logic of server.js but runs directly inside the APK.
 * This means NO external server is required.
 */
object InnerTubeClient {

    private const val YTM_BASE = "https://music.youtube.com/youtubei/v1"
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val CONTEXT_JSON = """
        {"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.00.00","hl":"id","gl":"ID"}}
    """.trimIndent()

    // ─── Core POST helper ────────────────────────────────────────────────────────

    private suspend fun ytPost(endpoint: String, bodyMap: Map<String, Any?>, query: String = ""): JsonObject =
        withContext(Dispatchers.IO) {
            val fullBody = buildJsonObject {
                add("context", JsonParser.parseString(CONTEXT_JSON))
                for ((k, v) in bodyMap) {
                    when (v) {
                        is String -> addProperty(k, v)
                        is Boolean -> addProperty(k, v)
                        is Number -> addProperty(k, v)
                        null -> {} // skip
                    }
                }
            }

            val req = Request.Builder()
                .url("$YTM_BASE/$endpoint?prettyPrint=false$query")
                .header("Content-Type", "application/json")
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0")
                .post(fullBody.toString().toRequestBody(JSON_TYPE))
                .build()

            val res = http.newCall(req).execute()
            if (!res.isSuccessful) throw Exception("YTM $endpoint → ${res.code}")
            val bodyStr = res.body?.string() ?: throw Exception("Empty response from $endpoint")
            JsonParser.parseString(bodyStr).asJsonObject
        }

    private fun buildJsonObject(block: JsonObject.() -> Unit): JsonObject =
        JsonObject().apply(block)

    // ─── Deep helpers (mirrors server.js findAll / findFirst) ────────────────────

    /** Safely find all values for [key] in the JSON tree without recursion overflow */
    private fun findAll(el: JsonElement, key: String, maxDepth: Int = 12, depth: Int = 0): List<JsonElement> {
        if (depth > maxDepth) return emptyList()
        val results = mutableListOf<JsonElement>()
        when {
            el.isJsonObject -> {
                val obj = el.asJsonObject
                for ((k, v) in obj.entrySet()) {
                    if (k == key) results.add(v)
                    results.addAll(findAll(v, key, maxDepth, depth + 1))
                }
            }
            el.isJsonArray -> {
                for (item in el.asJsonArray) {
                    results.addAll(findAll(item, key, maxDepth, depth + 1))
                }
            }
        }
        return results
    }

    private fun findFirst(el: JsonElement, key: String): JsonElement? =
        findAll(el, key).firstOrNull()

    private fun text(el: JsonElement?): String {
        el ?: return ""
        if (el.isJsonNull) return ""
        if (!el.isJsonObject) return ""
        val obj = el.asJsonObject
        if (obj.has("runs")) {
            val sb = StringBuilder()
            val runs = obj.getAsJsonArray("runs") ?: return ""
            for (r in runs) {
                if (r.isJsonObject) sb.append(r.asJsonObject.get("text")?.asString ?: "")
            }
            return sb.toString()
        }
        if (obj.has("simpleText")) return obj.get("simpleText").asString
        return ""
    }

    private fun thumbs(el: JsonElement?): String? {
        el ?: return null
        val all = findAll(el, "thumbnails")
        val flatUrls = mutableListOf<Pair<Int, String>>()
        for (arr in all) {
            if (!arr.isJsonArray) continue
            for (t in arr.asJsonArray) {
                if (!t.isJsonObject) continue
                val obj = t.asJsonObject
                val url = obj.get("url")?.asString ?: continue
                val w = obj.get("width")?.asInt ?: 0
                flatUrls.add(w to url)
            }
        }
        if (flatUrls.isEmpty()) return null
        val best = flatUrls.maxByOrNull { it.first }?.second ?: return null
        return if (best.contains("googleusercontent.com")) {
            best.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj")
        } else best
    }

    private fun endpointInfo(nav: JsonElement?): EndpointInfo {
        nav ?: return EndpointInfo()
        if (!nav.isJsonObject) return EndpointInfo()
        val obj = nav.asJsonObject
        val we = obj.getAsJsonObject("watchEndpoint")
        val be = obj.getAsJsonObject("browseEndpoint")
        val wpe = obj.getAsJsonObject("watchPlaylistEndpoint")
        if (we != null) return EndpointInfo(videoId = we.get("videoId")?.asString, playlistId = we.get("playlistId")?.asString)
        if (wpe != null) return EndpointInfo(playlistId = wpe.get("playlistId")?.asString)
        if (be != null) {
            val id = be.get("browseId")?.asString ?: return EndpointInfo()
            val type = when {
                id.startsWith("MPRE") -> "album"
                id.startsWith("UC") || id.startsWith("MPLA") -> "artist"
                id.startsWith("VL") || id.startsWith("PL") || id.startsWith("RDCLAK") -> "playlist"
                else -> "browse"
            }
            return EndpointInfo(browseId = id, browseType = type)
        }
        return EndpointInfo()
    }

    private fun runsInfo(el: JsonElement?): List<ArtistInfo> {
        el ?: return emptyList()
        if (!el.isJsonObject) return emptyList()
        val runs = el.asJsonObject.getAsJsonArray("runs") ?: return emptyList()
        val result = mutableListOf<ArtistInfo>()
        for (r in runs) {
            if (!r.isJsonObject) continue
            val nav = r.asJsonObject.getAsJsonObject("navigationEndpoint") ?: continue
            val be = nav.getAsJsonObject("browseEndpoint") ?: continue
            val browseId = be.get("browseId")?.asString ?: continue
            val name = r.asJsonObject.get("text")?.asString ?: continue
            result.add(ArtistInfo(name = name, browseId = browseId))
        }
        return result
    }

    private data class EndpointInfo(
        val videoId: String? = null,
        val playlistId: String? = null,
        val browseId: String? = null,
        val browseType: String? = null
    )

    private fun normDuration(s: String?): String? {
        if (s.isNullOrBlank()) return null
        val t = s.trim()
        if (Regex("^\\d{1,2}(\\.\\d{2}){1,2}$").matches(t)) return t.replace(".", ":")
        return t
    }

    // ─── Item parsers (mirrors server.js parseTwoRow / parseListItem) ───────────

    private fun parseTwoRowItem(r: JsonObject): TrackItem? {
        val nav = r.getAsJsonObject("navigationEndpoint")
        var info = endpointInfo(nav)

        // title may have browse nav
        if (info.browseId == null) {
            val titleRuns = r.getAsJsonObject("title")?.getAsJsonArray("runs")
            val firstRun = titleRuns?.firstOrNull()?.asJsonObject
            val titleNav = firstRun?.getAsJsonObject("navigationEndpoint")
            val extra = endpointInfo(titleNav)
            if (extra.browseId != null) info = info.copy(browseId = extra.browseId, browseType = extra.browseType)
        }

        val type = when {
            info.browseType in listOf("album", "playlist", "artist") -> info.browseType!!
            info.videoId != null -> "song"
            info.playlistId != null -> "playlist"
            else -> "song"
        }

        val title = text(r.get("title"))
        if (title.isBlank()) return null

        return TrackItem(
            videoId = info.videoId,
            title = title,
            subtitle = text(r.get("subtitle")),
            artist = text(r.get("subtitle")),
            thumbnail = thumbs(r.get("thumbnailRenderer")),
            artists = runsInfo(r.get("subtitle")),
            type = type,
            browseId = info.browseId,
            browseType = info.browseType,
            playlistId = info.playlistId
        )
    }

    private fun parseListItem(r: JsonObject): TrackItem? {
        val cols = (r.getAsJsonArray("flexColumns") ?: return null)
            .mapNotNull { c ->
                c.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.get("text")
            }

        val title = if (cols.isNotEmpty()) text(cols[0]) else ""
        if (title.isBlank()) return null

        val subtitle = cols.drop(1).joinToString(" • ") { text(it) }.trim()

        var videoId: String? = null

        // from playlistItemData
        videoId = videoId ?: r.getAsJsonObject("playlistItemData")?.get("videoId")?.asString

        // from title runs
        if (videoId == null) {
            val firstRun = cols.getOrNull(0)?.asJsonObject?.getAsJsonArray("runs")?.firstOrNull()?.asJsonObject
            videoId = firstRun?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("watchEndpoint")?.get("videoId")?.asString
        }

        // from overlay
        if (videoId == null) {
            val overlay = r.get("overlay")
            videoId = findAll(overlay ?: JsonObject(), "watchEndpoint").firstOrNull()?.asJsonObject?.get("videoId")?.asString
        }

        val navInfo = endpointInfo(r.getAsJsonObject("navigationEndpoint"))

        // Artists from subtitle runs
        val artists = mutableListOf<ArtistInfo>()
        val albums = mutableListOf<ArtistInfo>()
        for (col in cols.drop(1)) {
            for (info in runsInfo(col)) {
                if (info.browseId?.startsWith("MPRE") == true) albums.add(info)
                else artists.add(info)
            }
        }

        // Duration from fixed column
        val fixed = findFirst(r, "musicResponsiveListItemFixedColumnRenderer")
        val duration = normDuration(text(fixed?.asJsonObject?.get("text")))

        return TrackItem(
            videoId = videoId,
            title = title,
            subtitle = subtitle,
            artist = artists.firstOrNull()?.name ?: subtitle.split(" • ").firstOrNull(),
            thumbnail = thumbs(r.get("thumbnail")),
            artists = artists,
            album = albums.firstOrNull(),
            duration = duration,
            type = if (videoId != null) "song" else (navInfo.browseType ?: "song"),
            browseId = navInfo.browseId,
            browseType = navInfo.browseType,
            playlistId = navInfo.playlistId
        )
    }

    private fun parseSections(contents: JsonArray?): List<SectionItem> {
        contents ?: return emptyList()
        val sections = mutableListOf<SectionItem>()

        for (elem in contents) {
            if (!elem.isJsonObject) continue
            val s = elem.asJsonObject

            val car = s.getAsJsonObject("musicCarouselShelfRenderer")
            val shelf = s.getAsJsonObject("musicShelfRenderer")

            if (car != null) {
                val headerTitle = text(findFirst(car.get("header") ?: JsonObject(), "title"))
                val items = (car.getAsJsonArray("contents") ?: continue)
                    .mapNotNull { c ->
                        if (!c.isJsonObject) return@mapNotNull null
                        val co = c.asJsonObject
                        when {
                            co.has("musicTwoRowItemRenderer") -> parseTwoRowItem(co.getAsJsonObject("musicTwoRowItemRenderer"))
                            co.has("musicResponsiveListItemRenderer") -> parseListItem(co.getAsJsonObject("musicResponsiveListItemRenderer"))
                            else -> null
                        }
                    }
                    .filter { it.title?.isNotBlank() == true }

                if (items.isNotEmpty()) sections.add(SectionItem(title = headerTitle.ifBlank { "Rekomendasi" }, items = items, isList = false))

            } else if (shelf != null) {
                val title = text(shelf.get("title"))
                val items = (shelf.getAsJsonArray("contents") ?: continue)
                    .mapNotNull { c ->
                        if (!c.isJsonObject) null
                        else c.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")?.let { parseListItem(it) }
                    }
                    .filter { it.title?.isNotBlank() == true }

                if (items.isNotEmpty()) sections.add(SectionItem(title = title.ifBlank { "Lagu Populer" }, items = items, isList = true))
            }
        }

        return sections
    }

    // ─── Public API ──────────────────────────────────────────────────────────────

    suspend fun getHome(): HomeResponse = withContext(Dispatchers.IO) {
        try {
            val json = ytPost("browse", mapOf("browseId" to "FEmusic_home"))
            val sl = findFirst(json, "sectionListRenderer")
            val contents = sl?.asJsonObject?.getAsJsonArray("contents")
            HomeResponse(sections = parseSections(contents))
        } catch (e: Exception) {
            HomeResponse(error = e.message)
        }
    }

    suspend fun getCharts(): ChartResponse = withContext(Dispatchers.IO) {
        try {
            val json = ytPost("browse", mapOf("browseId" to "FEmusic_charts"))
            val sl = findFirst(json, "sectionListRenderer")
            val contents = sl?.asJsonObject?.getAsJsonArray("contents")
            ChartResponse(sections = parseSections(contents))
        } catch (e: Exception) {
            ChartResponse(error = e.message)
        }
    }

    suspend fun getMoods(): MoodResponse = withContext(Dispatchers.IO) {
        try {
            val json = ytPost("browse", mapOf("browseId" to "FEmusic_moods_and_genres"))
            val buttons = findAll(json, "musicNavigationButtonRenderer")
            val categories = mutableListOf<MoodCategory>()

            for (btn in buttons) {
                if (!btn.isJsonObject) continue
                val b = btn.asJsonObject
                val title = text(b.get("buttonText"))
                if (title.isBlank()) continue

                // color from solid.leftStripeColor (mirror of server.js `b.solid.leftStripeColor >>> 0`)
                var color: String? = null
                val solid = b.getAsJsonObject("solid")
                if (solid != null && solid.has("leftStripeColor")) {
                    try {
                        // Use toInt() then mask, same as JS >>> 0 (unsigned)
                        val colorInt = solid.get("leftStripeColor").asLong.toInt()
                        val hex = Integer.toHexString(colorInt).padStart(8, '0')
                        color = "#${hex.takeLast(6)}"
                    } catch (ignored: Exception) {}
                }

                val clickCmd = b.getAsJsonObject("clickCommand")
                val be = clickCmd?.getAsJsonObject("browseEndpoint")
                val browseId = be?.get("browseId")?.asString
                val params = be?.get("params")?.asString

                if (!browseId.isNullOrBlank()) {
                    categories.add(MoodCategory(title = title, color = color, browseId = browseId, params = params))
                }
            }

            MoodResponse(categories = categories)
        } catch (e: Exception) {
            MoodResponse(error = e.message)
        }
    }

    suspend fun search(query: String, filter: String? = null): SearchResponse = withContext(Dispatchers.IO) {
        try {
            val searchParams: Map<String, String> = mapOf(
                "songs" to "EgWKAQIIAWoMEA4QChADEAQQCRAF",
                "videos" to "EgWKAQIQAWoMEA4QChADEAQQCRAF",
                "albums" to "EgWKAQIYAWoMEA4QChADEAQQCRAF",
                "artists" to "EgWKAQIgAWoMEA4QChADEAQQCRAF",
                "playlists" to "EgeKAQQoAEABagwQDhAKEAMQBBAJEAU="
            )
            val bodyMap = mutableMapOf<String, Any?>("query" to query)
            val paramFilter = filter?.lowercase()
            if (paramFilter != null && searchParams.containsKey(paramFilter)) {
                bodyMap["params"] = searchParams[paramFilter]
            }

            val json = ytPost("search", bodyMap)

            val sections = mutableListOf<SectionItem>()
            // Primary: musicShelfRenderer
            val shelves = findAll(json, "musicShelfRenderer")
            for (shelf in shelves) {
                if (!shelf.isJsonObject) continue
                val s = shelf.asJsonObject
                val items = s.getAsJsonArray("contents")?.mapNotNull { c ->
                    if (!c.isJsonObject) null
                    else c.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")?.let { parseListItem(it) }
                }?.filter { it.title?.isNotBlank() == true } ?: emptyList()
                if (items.isNotEmpty()) sections.add(SectionItem(title = text(s.get("title")).ifBlank { "Hasil" }, items = items, isList = true))
            }

            // Fallback: itemSectionRenderer
            if (sections.isEmpty()) {
                val flat = mutableListOf<TrackItem>()
                val seen = mutableSetOf<String>()
                for (sec in findAll(json, "itemSectionRenderer")) {
                    if (!sec.isJsonObject) continue
                    val contents = sec.asJsonObject.getAsJsonArray("contents") ?: continue
                    for (c in contents) {
                        if (!c.isJsonObject) continue
                        val item = c.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")?.let { parseListItem(it) } ?: continue
                        val key = item.videoId ?: item.browseId ?: item.title ?: continue
                        if (item.title?.isNotBlank() == true && seen.add(key)) flat.add(item)
                    }
                }
                if (flat.isNotEmpty()) sections.add(SectionItem(title = "Hasil Pencarian", items = flat, isList = true))
            }

            SearchResponse(sections = sections)
        } catch (e: Exception) {
            SearchResponse(error = e.message)
        }
    }

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val json = ytPost("music/get_search_suggestions", mapOf("input" to query))
            findAll(json, "searchSuggestionRenderer").mapNotNull { el ->
                if (!el.isJsonObject) null
                else text(el.asJsonObject.get("suggestion")).ifBlank { null }
            }
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
            val bodyMap = mutableMapOf<String, Any?>("browseId" to id)
            if (params != null) bodyMap["params"] = params
            val json = ytPost("browse", bodyMap)

            // Header
            var headerTitle = ""
            var headerSubtitle = ""
            var headerThumb: String? = null
            var headerArtists: List<ArtistInfo> = emptyList()

            val headerRenderers = listOf(
                "musicResponsiveHeaderRenderer",
                "musicDetailHeaderRenderer",
                "musicImmersiveHeaderRenderer",
                "musicVisualHeaderRenderer"
            )
            var hObj: JsonObject? = null
            for (rName in headerRenderers) {
                val found = findFirst(json, rName)
                if (found != null && found.isJsonObject) { hObj = found.asJsonObject; break }
            }
            if (hObj != null) {
                headerTitle = text(hObj.get("title"))
                headerSubtitle = listOf(text(hObj.get("subtitle")), text(hObj.get("secondSubtitle")))
                    .filter { it.isNotBlank() }.joinToString(" • ")
                headerThumb = thumbs(hObj.get("thumbnail") ?: hObj.get("foregroundThumbnail") ?: hObj) ?: thumbs(hObj)
                headerArtists = runsInfo(hObj.get("subtitle"))
            }

            // Tracks
            val tracks = mutableListOf<TrackItem>()
            val trackShelves = findAll(json, "musicShelfRenderer") + findAll(json, "musicPlaylistShelfRenderer")
            for (shelf in trackShelves) {
                if (!shelf.isJsonObject) continue
                val contents = shelf.asJsonObject.getAsJsonArray("contents") ?: continue
                val items = contents.mapNotNull { c ->
                    if (!c.isJsonObject) null
                    else c.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")?.let { parseListItem(it) }
                }.filter { it.title?.isNotBlank() == true }
                if (items.isNotEmpty() && items.count { it.videoId != null } >= items.size / 2 && tracks.isEmpty()) {
                    tracks.addAll(items)
                }
            }

            // Grid sections (mood/genre pages)
            val sections = mutableListOf<SectionItem>()
            val sl = findFirst(json, "sectionListRenderer")
            if (sl != null && sl.isJsonObject) {
                val slContents = sl.asJsonObject.getAsJsonArray("contents")
                sections.addAll(parseSections(slContents))
            }
            for (grid in findAll(json, "gridRenderer")) {
                if (!grid.isJsonObject) continue
                val items = grid.asJsonObject.getAsJsonArray("items")?.mapNotNull { c ->
                    if (!c.isJsonObject) null
                    else c.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer")?.let { parseTwoRowItem(it) }
                } ?: emptyList()
                if (items.isNotEmpty()) {
                    val gridTitle = text(findFirst(grid.asJsonObject.get("header") ?: JsonObject(), "title"))
                    sections.add(SectionItem(title = gridTitle, items = items, isList = false))
                }
            }

            if (headerThumb == null && tracks.isNotEmpty()) headerThumb = tracks.first().thumbnail

            // Copy artist from header to tracks that lack it
            val headerArtist = headerArtists.firstOrNull()
            val enrichedTracks = if (headerArtist?.name != null && tracks.isNotEmpty()) {
                tracks.map { t ->
                    if (t.artist?.isNotBlank() == true || t.artists?.isNotEmpty() == true) t
                    else t.copy(artist = headerArtist.name, artists = listOf(headerArtist))
                }
            } else tracks

            BrowseResponse(
                header = BrowseHeader(
                    title = headerTitle.ifBlank { "Detail" },
                    subtitle = headerSubtitle,
                    thumbnail = headerThumb,
                    artists = headerArtists
                ),
                tracks = enrichedTracks,
                sections = sections
            )
        } catch (e: Exception) {
            BrowseResponse(error = e.message)
        }
    }

    suspend fun getLyrics(title: String, artist: String, duration: Long? = null, browseId: String? = null): LyricsResponse = withContext(Dispatchers.IO) {
        try {
            // 1. If browseId is available, try direct YTM lyrics
            if (!browseId.isNullOrBlank()) {
                try {
                    val ytmJson = ytPost("browse", mapOf("browseId" to browseId))
                    for (shelf in findAll(ytmJson, "musicDescriptionShelfRenderer")) {
                        val lyr = text(shelf.asJsonObject?.get("description"))
                        if (lyr.length > 20) return@withContext LyricsResponse(plain = lyr, source = "YouTube Music")
                    }
                } catch (ignored: Exception) {}
            }

            val encTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encArtist = java.net.URLEncoder.encode(artist, "UTF-8")
            val durParam = if (duration != null && duration > 0) "&duration=${duration / 1000}" else ""
            val url = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist$durParam"

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "FMusic/1.0")
                .build()

            val res = http.newCall(req).execute()
            if (res.isSuccessful && res.body != null) {
                val json = JsonParser.parseString(res.body!!.string()).asJsonObject
                if (!json.has("code") || json.get("code")?.asInt != 404) {
                    val synced = json.get("syncedLyrics")?.asString
                    val plain = json.get("plainLyrics")?.asString
                    if (!synced.isNullOrBlank() || !plain.isNullOrBlank()) {
                        return@withContext LyricsResponse(synced = synced, plain = plain, source = "LRCLIB")
                    }
                }
            }

            // Fuzzy search fallback
            val searchUrl = "https://lrclib.net/api/search?track_name=$encTitle&artist_name=$encArtist"
            val searchReq = Request.Builder().url(searchUrl).header("User-Agent", "FMusic/1.0").build()
            val searchRes = http.newCall(searchReq).execute()
            if (searchRes.isSuccessful && searchRes.body != null) {
                val arr = JsonParser.parseString(searchRes.body!!.string())
                if (arr.isJsonArray) {
                    val first = arr.asJsonArray.firstOrNull { it.isJsonObject }?.asJsonObject
                    if (first != null) {
                        val synced = first.get("syncedLyrics")?.asString
                        val plain = first.get("plainLyrics")?.asString
                        if (!synced.isNullOrBlank() || !plain.isNullOrBlank()) {
                            return@withContext LyricsResponse(synced = synced, plain = plain, source = "LRCLIB")
                        }
                    }
                }
            }

            LyricsResponse(synced = null, plain = null, source = null)
        } catch (e: Exception) {
            LyricsResponse(error = e.message)
        }
    }
}
