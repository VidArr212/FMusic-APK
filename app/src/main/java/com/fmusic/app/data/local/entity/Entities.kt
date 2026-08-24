package com.fmusic.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fmusic.app.data.model.TrackItem

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val subtitle: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null,
    val browseId: String? = null,
    val lastPlayedAt: Long = System.currentTimeMillis()
) {
    fun toTrackItem(): TrackItem {
        return TrackItem(
            videoId = videoId,
            title = title,
            artist = artist,
            subtitle = subtitle ?: artist,
            thumbnail = thumbnail,
            duration = duration,
            browseId = browseId
        )
    }
}

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val subtitle: String? = null,
    val thumbnail: String? = null,
    val duration: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrackItem(): TrackItem {
        return TrackItem(
            videoId = videoId,
            title = title,
            artist = artist,
            subtitle = subtitle ?: artist,
            thumbnail = thumbnail,
            duration = duration
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "videoId"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val duration: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrackItem(): TrackItem {
        return TrackItem(
            videoId = videoId,
            title = title,
            artist = artist,
            subtitle = artist,
            thumbnail = thumbnail,
            duration = duration
        )
    }
}

@Entity(tableName = "offline_tracks")
data class OfflineTrackEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String? = null,
    val duration: String? = null,
    val localFilePath: String,
    val fileSize: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toTrackItem(): TrackItem {
        return TrackItem(
            videoId = videoId,
            title = title,
            artist = artist,
            subtitle = artist,
            thumbnail = thumbnail,
            duration = duration,
            isOffline = true,
            localPath = localFilePath
        )
    }
}
