package com.fmusic.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fmusic.app.data.local.dao.*
import com.fmusic.app.data.local.entity.*

@Database(
    entities = [
        SearchHistoryEntity::class,
        RecentlyPlayedEntity::class,
        FavoriteTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        OfflineTrackEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FMusicDatabase : RoomDatabase() {

    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun favoriteTrackDao(): FavoriteTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun offlineTrackDao(): OfflineTrackDao

    companion object {
        @Volatile
        private var INSTANCE: FMusicDatabase? = null

        fun getDatabase(context: Context): FMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FMusicDatabase::class.java,
                    "fmusic_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
