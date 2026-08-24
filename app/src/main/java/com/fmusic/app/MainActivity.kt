package com.fmusic.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.fmusic.app.player.FMusicPlayerService
import com.fmusic.app.ui.screens.main.MainScreen
import com.fmusic.app.ui.screens.splash.SplashScreen
import com.fmusic.app.ui.theme.DarkBackground
import com.fmusic.app.ui.theme.FMusicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Start background media player service
        startPlayerService()

        setContent {
            FMusicTheme {
                var isSplashActive by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                ) {
                    Crossfade(
                        targetState = isSplashActive,
                        label = "SplashToMainTransition"
                    ) { showSplash ->
                        if (showSplash) {
                            SplashScreen(
                                onSplashFinished = { isSplashActive = false }
                            )
                        } else {
                            MainScreen()
                        }
                    }
                }
            }
        }
    }

    private fun startPlayerService() {
        val intent = Intent(this, FMusicPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
