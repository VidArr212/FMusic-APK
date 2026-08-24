package com.fmusic.app.ui.screens.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fmusic.app.data.api.ApiClient
import com.fmusic.app.data.local.FMusicDatabase
import com.fmusic.app.data.local.entity.PlaylistEntity
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.data.repository.MusicRepository
import com.fmusic.app.data.repository.OfflineMusicRepository
import com.fmusic.app.player.PlayerManager
import com.fmusic.app.ui.components.*
import com.fmusic.app.ui.navigation.Screen
import com.fmusic.app.ui.screens.browse.BrowseDetailScreen
import com.fmusic.app.ui.screens.charts.ChartsScreen
import com.fmusic.app.ui.screens.home.HomeScreen
import com.fmusic.app.ui.screens.library.LibraryScreen
import com.fmusic.app.ui.screens.lyrics.LyricsScreen
import com.fmusic.app.ui.screens.search.SearchScreen
import com.fmusic.app.ui.theme.DarkBackground
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerManager = remember { PlayerManager.getInstance(context) }
    val repository = remember { MusicRepository(context) }
    val offlineRepo = remember { OfflineMusicRepository(context) }

    val playerState by playerManager.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    // Dialog & Modal states
    var isFullPlayerVisible by remember { mutableStateOf(false) }
    var isLyricsVisible by remember { mutableStateOf(false) }
    var isSleepTimerVisible by remember { mutableStateOf(false) }
    var isServerConfigVisible by remember { mutableStateOf(false) }
    var isAddToPlaylistVisible by remember { mutableStateOf(false) }
    var isCreatePlaylistVisible by remember { mutableStateOf(false) }
    var selectedTrackForPlaylist by remember { mutableStateOf<TrackItem?>(null) }

    // Favorites & Offline status
    val currentVideoId = playerState.currentTrack?.videoId ?: ""
    val isFavorite by repository.isFavorite(currentVideoId).collectAsState(initial = false)
    val isDownloaded by repository.isDownloaded(currentVideoId).collectAsState(initial = false)
    val allPlaylists by repository.getAllPlaylists().collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.background(DarkBackground)) {
                // Mini Player (Only visible when a track is active)
                if (playerState.currentTrack != null && !isFullPlayerVisible && !isLyricsVisible) {
                    MiniPlayer(
                        state = playerState,
                        isFavorite = isFavorite,
                        onPlayPauseClick = { playerManager.togglePlayPause() },
                        onNextClick = { playerManager.skipNext() },
                        onFavoriteClick = {
                            playerState.currentTrack?.let { track ->
                                scope.launch { repository.toggleFavorite(track) }
                            }
                        },
                        onAddToPlaylistClick = {
                            selectedTrackForPlaylist = playerState.currentTrack
                            isAddToPlaylistVisible = true
                        },
                        onClick = { isFullPlayerVisible = true }
                    )
                }

                // Bottom Navigation Bar
                if (!isFullPlayerVisible && !isLyricsVisible && !currentRoute.startsWith("browse_detail")) {
                    FMusicBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onBrowseClick = { browseId, title, type ->
                        navController.navigate(Screen.BrowseDetail.createRoute(browseId, title, type))
                    },
                    onOpenSettings = { isServerConfigVisible = true }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onBrowseClick = { browseId, title, type ->
                        navController.navigate(Screen.BrowseDetail.createRoute(browseId, title, type))
                    }
                )
            }

            composable(Screen.Charts.route) {
                ChartsScreen(
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onBrowseClick = { browseId, title, type ->
                        navController.navigate(Screen.BrowseDetail.createRoute(browseId, title, type))
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    },
                    onPlaylistClick = { playlistId, name ->
                        navController.navigate(Screen.BrowseDetail.createRoute("VL$playlistId", name, "playlist"))
                    }
                )
            }

            composable(Screen.BrowseDetail.route) { backStackEntry ->
                val browseId = backStackEntry.arguments?.getString("browseId") ?: ""
                val title = backStackEntry.arguments?.getString("title")
                val type = backStackEntry.arguments?.getString("type")

                BrowseDetailScreen(
                    browseId = browseId,
                    titleHint = title,
                    typeHint = type,
                    onBack = { navController.popBackStack() },
                    onTrackClick = { track, queue ->
                        playerManager.playTrack(track, queue)
                    }
                )
            }
        }
    }

    // Full Player Modal
    if (isFullPlayerVisible && playerState.currentTrack != null) {
        FullPlayerModal(
            state = playerState,
            isFavorite = isFavorite,
            isDownloaded = isDownloaded,
            onDismiss = { isFullPlayerVisible = false },
            onPlayPause = { playerManager.togglePlayPause() },
            onNext = { playerManager.skipNext() },
            onPrevious = { playerManager.skipPrevious() },
            onSeek = { progress -> playerManager.seekToProgress(progress) },
            onToggleShuffle = { playerManager.toggleShuffle() },
            onToggleRepeat = { playerManager.toggleRepeat() },
            onToggleFavorite = {
                playerState.currentTrack?.let { track ->
                    scope.launch { repository.toggleFavorite(track) }
                }
            },
            onAddToPlaylist = {
                selectedTrackForPlaylist = playerState.currentTrack
                isAddToPlaylistVisible = true
            },
            onOpenSleepTimer = { isSleepTimerVisible = true },
            onOpenLyrics = { isLyricsVisible = true },
            onDownloadTrack = {
                playerState.currentTrack?.let { track ->
                    scope.launch {
                        Toast.makeText(context, "Memulai unduhan offline...", Toast.LENGTH_SHORT).show()
                        offlineRepo.downloadTrack(track) { state ->
                            when (state) {
                                is com.fmusic.app.data.repository.DownloadState.Success -> {
                                    Toast.makeText(context, "Lagu berhasil disimpan offline! 🎵", Toast.LENGTH_SHORT).show()
                                }
                                is com.fmusic.app.data.repository.DownloadState.Error -> {
                                    Toast.makeText(context, "Unduhan gagal: ${state.error}", Toast.LENGTH_SHORT).show()
                                }
                                else -> {}
                            }
                        }
                    }
                }
            },
            onOpenArtist = { artistBrowseId ->
                isFullPlayerVisible = false
                navController.navigate(Screen.BrowseDetail.createRoute(artistBrowseId, playerState.currentTrack?.getDisplayArtist(), "artist"))
            }
        )
    }

    // Lyrics Full Screen Dialog
    if (isLyricsVisible && playerState.currentTrack != null) {
        LyricsScreen(
            track = playerState.currentTrack!!,
            currentPositionMs = playerState.currentPositionMs,
            onSeek = { targetMs -> playerManager.seekTo(targetMs) },
            onClose = { isLyricsVisible = false }
        )
    }

    // Sleep Timer Dialog
    if (isSleepTimerVisible) {
        SleepTimerDialog(
            isActive = playerState.isSleepTimerActive,
            remainingSeconds = playerState.sleepTimerRemainingSeconds,
            onSetTimer = { minutes ->
                playerManager.setSleepTimer(minutes)
                Toast.makeText(context, "Timer tidur disetel ke $minutes menit", Toast.LENGTH_SHORT).show()
            },
            onCancelTimer = {
                playerManager.cancelSleepTimer()
                Toast.makeText(context, "Timer tidur dimatikan", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { isSleepTimerVisible = false }
        )
    }

    // Server Config Dialog
    if (isServerConfigVisible) {
        ServerConfigDialog(
            currentUrl = ApiClient.getBaseUrl(context),
            onSaveUrl = { newUrl ->
                ApiClient.setBaseUrl(context, newUrl)
                Toast.makeText(context, "Server API diperbarui: $newUrl", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { isServerConfigVisible = false }
        )
    }

    // Add to Playlist Dialog
    if (isAddToPlaylistVisible && selectedTrackForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = allPlaylists,
            onDismiss = {
                isAddToPlaylistVisible = false
                selectedTrackForPlaylist = null
            },
            onSelectPlaylist = { playlist ->
                scope.launch {
                    repository.addTrackToPlaylist(playlist.id, selectedTrackForPlaylist!!)
                    Toast.makeText(context, "Ditambahkan ke '${playlist.name}'", Toast.LENGTH_SHORT).show()
                }
            },
            onCreateNewClick = {
                isCreatePlaylistVisible = true
            }
        )
    }

    // Create Playlist Dialog
    if (isCreatePlaylistVisible) {
        CreatePlaylistDialog(
            onDismiss = { isCreatePlaylistVisible = false },
            onCreate = { name, desc ->
                scope.launch {
                    val id = repository.createPlaylist(name, desc)
                    if (selectedTrackForPlaylist != null) {
                        repository.addTrackToPlaylist(id, selectedTrackForPlaylist!!)
                    }
                    Toast.makeText(context, "Playlist '$name' dibuat!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
