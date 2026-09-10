package com.lcgg.ohmygoad
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun UpdateCheckHandler() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableUpdate = UpdateChecker.checkForUpdate(context)
    }

    fun startDownloadAndInstall(update: UpdateInfo) {
        isDownloading = true
        scope.launch {
            val downloadId = UpdateDownloader.enqueueDownload(context, update)
            val success = UpdateDownloader.awaitDownload(context, downloadId)
            isDownloading = false

            if (!success) {
                availableUpdate = null
                return@launch
            }

            val file = UpdateDownloader.downloadedFile(context, update)
            val isValid = file.exists() && UpdateDownloader.verifySha256(file, update.sha256)
            availableUpdate = null

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
                    TextButton(onClick = { startDownloadAndInstall(update) }) {
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