package com.fmusic.app.data.model

import com.google.gson.annotations.SerializedName

data class ArtistInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("browseId") val browseId: String? = null
)

data class TrackItem(
    @SerializedName("videoId") val videoId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("artists") val artists: List<ArtistInfo>? = null,
    @SerializedName("album") val album: ArtistInfo? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("type") val type: String? = "song",
    @SerializedName("browseId") val browseId: String? = null,
    @SerializedName("browseType") val browseType: String? = null,
    @SerializedName("playlistId") val playlistId: String? = null,
    @SerializedName("params") val params: String? = null,
    @SerializedName("isExplicit") val isExplicit: Boolean? = false,
    @SerializedName("isOffline") val isOffline: Boolean = false,
    @SerializedName("localPath") val localPath: String? = null
) {
    fun getDisplayArtist(): String {
        if (!artist.isNullOrBlank()) return artist
        if (!artists.isNullOrEmpty()) {
            return artists.mapNotNull { it.name }.joinToString(", ")
        }
        if (!subtitle.isNullOrBlank()) return subtitle
        return "Unknown Artist"
    }

    fun getCleanTitle(): String {
        return (title ?: "Unknown Title").replace(Regex("\\s*[\\[(](official|video|audio|lyrics|mv|hd|4k)[^\\])]*[\\])]", RegexOption.IGNORE_CASE), "").trim()
    }
}
