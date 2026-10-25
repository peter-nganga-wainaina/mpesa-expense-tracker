package com.example.expensetracker.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.expensetracker.update.ReleaseInfo
import com.example.expensetracker.update.UpdateInstaller
import kotlinx.coroutines.launch
import java.io.IOException

private enum class DownloadState { Idle, Downloading, Failed }

/** Offers a newer release. The download starts only from this tap, then Android asks to confirm the install. */
@Composable
fun UpdateBanner(release: ReleaseInfo, installer: UpdateInstaller, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(release) { mutableStateOf(DownloadState.Idle) }

    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when (state) {
                DownloadState.Idle -> "Update available: version ${release.versionName}"
                DownloadState.Downloading -> "Downloading the update…"
                DownloadState.Failed -> "The update didn't download. Check the connection and try again."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Button(
            enabled = state != DownloadState.Downloading,
            onClick = {
                scope.launch {
                    state = DownloadState.Downloading
                    state = try {
                        installer.downloadAndInstall(context)
                        DownloadState.Idle
                    } catch (e: IOException) {
                        Log.w("UpdateBanner", "Update download failed", e)
                        DownloadState.Failed
                    }
                }
            },
        ) { Text(if (state == DownloadState.Failed) "Try again" else "Update") }
    }
}
