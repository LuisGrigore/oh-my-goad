package com.lcgg.ohmygoad
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import java.security.MessageDigest

object UpdateDownloader {

    private fun apkFileName(update: UpdateInfo): String =
        "update-${update.versionName}.apk"

    fun enqueueDownload(context: Context, update: UpdateInfo): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
            .setTitle("OhMyGOAD update ${update.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                apkFileName(update),
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        return downloadManager.enqueue(request)
    }

    /**
     * Polls DownloadManager until the given download finishes (successfully or not).
     * Avoids relying on ACTION_DOWNLOAD_COMPLETE, which can be missed if the download
     * finishes before a BroadcastReceiver has had a chance to register.
     */
    suspend fun awaitDownload(context: Context, downloadId: Long): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    when (cursor.getInt(statusIndex)) {
                        DownloadManager.STATUS_SUCCESSFUL -> return true
                        DownloadManager.STATUS_FAILED -> return false
                    }
                } else {
                    // Entry gone (e.g. user cancelled it from the system download UI)
                    return false
                }
            }
            delay(500)
        }
    }

    fun downloadedFile(context: Context, update: UpdateInfo): File =
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            apkFileName(update),
        )

    fun verifySha256(file: File, expectedSha256: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expectedSha256, ignoreCase = true)
    }

    fun installApk(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}