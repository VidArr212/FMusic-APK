package com.fmusic.app.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Default fallback base URL. Can be modified via UserPreferences / in-app settings
    const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"
    private const val PREFS_NAME = "fmusic_api_prefs"
    private const val KEY_BASE_URL = "base_url"

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var apiService: FMusicApiService? = null

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, formattedUrl).apply()
        currentBaseUrl = formattedUrl
        apiService = null // Force recreation with new URL
    }

    fun getService(context: Context): FMusicApiService {
        val savedUrl = getBaseUrl(context)
        if (apiService == null || currentBaseUrl != savedUrl) {
            currentBaseUrl = savedUrl
            apiService = createRetrofit(currentBaseUrl).create(FMusicApiService::class.java)
        }
        return apiService!!
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
