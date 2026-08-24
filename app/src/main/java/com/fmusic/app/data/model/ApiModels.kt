package com.fmusic.app.data.model

import com.google.gson.annotations.SerializedName

data class SectionItem(
    @SerializedName("title") val title: String? = "",
    @SerializedName("items") val items: List<TrackItem>? = emptyList(),
    @SerializedName("list") val isList: Boolean? = false
)

data class HomeResponse(
    @SerializedName("sections") val sections: List<SectionItem>? = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class ChartResponse(
    @SerializedName("sections") val sections: List<SectionItem>? = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class MoodCategory(
    @SerializedName("title") val title: String? = "",
    @SerializedName("color") val color: String? = null,
    @SerializedName("browseId") val browseId: String? = null,
    @SerializedName("params") val params: String? = null
)

data class MoodResponse(
    @SerializedName("categories") val categories: List<MoodCategory>? = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class SearchResponse(
    @SerializedName("sections") val sections: List<SectionItem>? = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class SuggestResponse(
    @SerializedName("suggestions") val suggestions: List<String>? = emptyList()
)

data class BrowseHeader(
    @SerializedName("title") val title: String? = "",
    @SerializedName("subtitle") val subtitle: String? = "",
    @SerializedName("description") val description: String? = "",
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("artists") val artists: List<ArtistInfo>? = emptyList(),
    @SerializedName("strapline") val strapline: String? = null
)

data class BrowseResponse(
    @SerializedName("header") val header: BrowseHeader? = null,
    @SerializedName("tracks") val tracks: List<TrackItem>? = emptyList(),
    @SerializedName("sections") val sections: List<SectionItem>? = emptyList(),
    @SerializedName("playlistId") val playlistId: String? = null,
    @SerializedName("error") val error: String? = null
)

data class QueueResponse(
    @SerializedName("queue") val queue: List<TrackItem>? = emptyList(),
    @SerializedName("lyricsBrowseId") val lyricsBrowseId: String? = null,
    @SerializedName("relatedBrowseId") val relatedBrowseId: String? = null
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsResponse(
    @SerializedName("synced") val synced: String? = null,
    @SerializedName("plain") val plain: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("error") val error: String? = null
) {
    fun parseSyncedLines(): List<LyricLine> {
        if (synced.isNullOrBlank()) return emptyList()
        val regex = Regex("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)")
        val lines = mutableListOf<LyricLine>()
        synced.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toDoubleOrNull() ?: 0.0
                val text = match.groupValues[3].trim()
                val totalMs = (min * 60 * 1000 + sec * 1000).toLong()
                lines.add(LyricLine(timeMs = totalMs, text = text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}

data class DownloadStartResponse(
    @SerializedName("jobId") val jobId: String? = null,
    @SerializedName("progressUrl") val progressUrl: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("error") val error: String? = null
)

data class DownloadProgressResponse(
    @SerializedName("progress") val progress: Int? = 0,
    @SerializedName("done") val done: Boolean? = false,
    @SerializedName("url") val url: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("error") val error: String? = null
)

data class SponsorSegment(
    @SerializedName("category") val category: String? = null,
    @SerializedName("start") val start: Double? = 0.0,
    @SerializedName("end") val end: Double? = 0.0
)

data class SponsorBlockResponse(
    @SerializedName("segments") val segments: List<SponsorSegment>? = emptyList()
)
