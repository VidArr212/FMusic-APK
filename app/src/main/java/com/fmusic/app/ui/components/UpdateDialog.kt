package com.fmusic.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fmusic.app.ui.theme.*
import com.fmusic.app.updater.UpdateInfo
import com.fmusic.app.updater.UpdateProgress

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    progress: UpdateProgress,
    onStartUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (progress !is UpdateProgress.Downloading) onDismiss()
        },
        containerColor = DarkSurfaceVariant,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Update FMusic v${updateInfo.latestVersion}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (progress) {
                    is UpdateProgress.Downloading -> {
                        Text(
                            text = "Mengunduh pembaruan APK... (${progress.percent}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = progress.percent / 100f,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = NeonCyan,
                            trackColor = DarkSurfaceElevated
                        )
                    }

                    is UpdateProgress.ReadyToInstall -> {
                        Text(
                            text = "Unduhan selesai! Membuka installer pembaruan...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan
                        )
                    }

                    is UpdateProgress.Error -> {
                        Text(
                            text = "Gagal: ${progress.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HeartRed
                        )
                    }

                    else -> {
                        Text(
                            text = "Versi baru telah dirilis dengan pembaruan dan perbaikan terbaru!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (updateInfo.changelog.isNotBlank()) {
                            Text(
                                text = "Catatan Pembaruan:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = updateInfo.changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (progress !is UpdateProgress.Downloading && progress !is UpdateProgress.ReadyToInstall) {
                Button(
                    onClick = onStartUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Update Sekarang", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (progress !is UpdateProgress.Downloading && progress !is UpdateProgress.ReadyToInstall) {
                TextButton(onClick = onDismiss) {
                    Text("Nanti", color = TextGray)
                }
            }
        }
    )
}
