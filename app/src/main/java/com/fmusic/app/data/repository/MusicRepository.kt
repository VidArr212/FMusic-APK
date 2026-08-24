package com.fmusic.app.data.repository

import android.content.Context
import com.fmusic.app.data.api.ApiClient
import com.fmusic.app.data.innertube.InnerTubeClient
import com.fmusic.app.data.local.FMusicDatabase
import com.fmusic.app.data.local.entity.*
import com.fmusic.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val db = FMusicDatabase.getDatabase(context)
    private val searchHistoryDao = db.searchHistoryDao()
    private val recentlyPlayedDao = db.recentlyPlayedDao()
    private val favoriteTrackDao = db.favoriteTrackDao()
    private val playlistDao = db.playlistDao()
    private val offlineTrackDao = db.offlineTrackDao()

    // 100% Embedded Direct InnerTube Engine (No external server needed)
    suspend fun getHome(): Result<HomeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = InnerTubeClient.getHome()
            if (response.sections != null && response.sections.isNotEmpty()) {
                Result.success(response)
            } else {
                // Fallback to proxy API if configured
                val apiRes = ApiClient.getService(context).getHome()
                if (apiRes.isSuccessful && apiRes.body() != null) {
                    Result.success(apiRes.body()!!)
                } else {
                    Result.success(response)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCharts(): Result<ChartResponse> = withContext(Dispatchers.IO) {
        try {
            val response = InnerTubeClient.getCharts()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMoods(): Result<MoodResponse> = withContext(Dispatchers.IO) {
        try {
            val response = InnerTubeClient.getMoods()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String, filter: String? = null): Result<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            val response = InnerTubeClient.search(query, filter)
            searchHistoryDao.insertSearch(SearchHistoryEntity(query.trim()))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        InnerTubeClient.getSuggestions(query)
    }

    suspend fun browse(id: String, params: String? = null): Result<BrowseResponse> = withContext(Dispatchers.IO) {
        try {
            val response = InnerTubeClient.browse(id, params)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLyrics(title: String, artist: String, duration: Long? = null, browseId: String? = null): Result<LyricsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = InnerTubeClient.getLyrics(title, artist, duration, browseId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Local DB - Search History
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>> = searchHistoryDao.getAllHistory()
    suspend fun deleteSearchHistory(query: String) = withContext(Dispatchers.IO) { searchHistoryDao.deleteSearch(query) }
    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) { searchHistoryDao.clearAllHistory() }

    // Local DB - Recently Played
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>> = recentlyPlayedDao.getRecentlyPlayed()
    suspend fun recordRecentlyPlayed(track: TrackItem) = withContext(Dispatchers.IO) {
        val vid = track.videoId ?: return@withContext
        recentlyPlayedDao.insertOrUpdate(
            RecentlyPlayedEntity(
                videoId = vid,
                title = track.title ?: "Unknown Title",
                artist = track.getDisplayArtist(),
                subtitle = track.subtitle,
                thumbnail = track.thumbnail,
                duration = track.duration,
                browseId = track.browseId,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }
    suspend fun deleteRecentlyPlayed(videoId: String) = withContext(Dispatchers.IO) { recentlyPlayedDao.deleteRecent(videoId) }
    suspend fun clearRecentlyPlayed() = withContext(Dispatchers.IO) { recentlyPlayedDao.clearAllRecent() }

    // Local DB - Favorites
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>> = favoriteTrackDao.getAllFavorites()
    fun isFavorite(videoId: String): Flow<Boolean> = favoriteTrackDao.isFavorite(videoId)
    suspend fun toggleFavorite(track: TrackItem) = withContext(Dispatchers.IO) {
        val vid = track.videoId ?: return@withContext
        val isFav = favoriteTrackDao.isFavoriteSync(vid)
        if (isFav) {
            favoriteTrackDao.deleteFavorite(vid)
        } else {
            favoriteTrackDao.insertFavorite(
                FavoriteTrackEntity(
                    videoId = vid,
                    title = track.title ?: "Unknown Title",
                    artist = track.getDisplayArtist(),
                    subtitle = track.subtitle,
                    thumbnail = track.thumbnail,
                    duration = track.duration
                )
            )
        }
    }

    // Local DB - Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> = playlistDao.getTracksForPlaylist(playlistId)
    suspend fun createPlaylist(name: String, description: String? = null): Long = withContext(Dispatchers.IO) {
        playlistDao.createPlaylist(PlaylistEntity(name = name, description = description))
    }
    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) { playlistDao.deletePlaylist(id) }
    suspend fun addTrackToPlaylist(playlistId: Long, track: TrackItem) = withContext(Dispatchers.IO) {
        val vid = track.videoId ?: return@withContext
        playlistDao.addTrackToPlaylist(
            PlaylistTrackEntity(
                playlistId = playlistId,
                videoId = vid,
                title = track.title ?: "Unknown",
                artist = track.getDisplayArtist(),
                thumbnail = track.thumbnail,
                duration = track.duration
            )
        )
    }
    suspend fun removeTrackFromPlaylist(playlistId: Long, videoId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, videoId)
    }

    // Local DB - Offline
    fun getAllOfflineTracks(): Flow<List<OfflineTrackEntity>> = offlineTrackDao.getAllOfflineTracks()
    fun isDownloaded(videoId: String): Flow<Boolean> = offlineTrackDao.isDownloaded(videoId)
    suspend fun deleteOfflineTrack(videoId: String) = withContext(Dispatchers.IO) {
        val track = offlineTrackDao.getOfflineTrack(videoId)
        if (track != null) {
            val file = java.io.File(track.localFilePath)
            if (file.exists()) file.delete()
            offlineTrackDao.deleteOfflineTrack(videoId)
        }
    }
}
