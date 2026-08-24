package com.fmusic.app.data.api

import com.fmusic.app.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface FMusicApiService {

    @GET("api/home")
    suspend fun getHome(): Response<HomeResponse>

    @GET("api/charts")
    suspend fun getCharts(): Response<ChartResponse>

    @GET("api/moods")
    suspend fun getMoods(): Response<MoodResponse>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String? = null
    ): Response<SearchResponse>

    @GET("api/suggest")
    suspend fun getSuggestions(
        @Query("q") query: String
    ): Response<SuggestResponse>

    @GET("api/browse")
    suspend fun browse(
        @Query("id") id: String,
        @Query("params") params: String? = null
    ): Response<BrowseResponse>

    @GET("api/next")
    suspend fun getNext(
        @Query("videoId") videoId: String? = null,
        @Query("playlistId") playlistId: String? = null,
        @Query("params") params: String? = null
    ): Response<QueueResponse>

    @GET("api/related")
    suspend fun getRelated(
        @Query("browseId") browseId: String
    ): Response<HomeResponse>

    @GET("api/lyrics")
    suspend fun getLyrics(
        @Query("title") title: String,
        @Query("artist") artist: String,
        @Query("duration") duration: Long? = null,
        @Query("browseId") browseId: String? = null
    ): Response<LyricsResponse>

    @GET("api/download-start")
    suspend fun startDownload(
        @Query("videoId") videoId: String
    ): Response<DownloadStartResponse>

    @GET("api/download-progress")
    suspend fun pollDownloadProgress(
        @Query("progressUrl") progressUrl: String
    ): Response<DownloadProgressResponse>

    @GET("api/sponsorblock")
    suspend fun getSponsorBlock(
        @Query("videoId") videoId: String
    ): Response<SponsorBlockResponse>
}
