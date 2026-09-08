package com.lcgg.ohmygoad

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun UpdateCheckHandler() {
    val context = LocalContext.current

    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var pendingDownloadId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        availableUpdate = UpdateChecker.checkForUpdate(context)
    }

    DisposableEffect(pendingDownloadId) {
        val downloadId = pendingDownloadId
        if (downloadId == null) {
            onDispose { }
        } else {
            val update = availableUpdate
            val receiver = UpdateDownloader.registerDownloadCompleteReceiver(
                context = context,
                downloadId = downloadId,
            ) {
                isDownloading = false
                pendingDownloadId = null

                if (update != null) {
                    val file = UpdateDownloader.downloadedFile(context, update)
                    val isValid = file.exists() &&
                            UpdateDownloader.verifySha256(file, update.sha256)

                    if (isValid) {
                        if (context.packageManager.canRequestPackageInstalls()) {
                            UpdateDownloader.installApk(context, file)
                        } else {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    }
                }
            }

            onDispose { context.unregisterReceiver(receiver) }
        }
    }

    availableUpdate?.let { update ->
        if (!isDownloading) {
            AlertDialog(
                onDismissRequest = { availableUpdate = null },
                title = { Text("Update available") },
                text = {
                    Text(
                        "A new version (${update.versionName}) is available. " +
                                "Download and install it now?",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        isDownloading = true
                        pendingDownloadId = UpdateDownloader.enqueueDownload(context, update)
                    }) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { availableUpdate = null }) {
                        Text("Later")
                    }
                },
            )
        }
    }
}