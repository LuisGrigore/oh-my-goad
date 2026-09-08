package com.lcgg.ohmygoad

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private fun latestJsonUrl(): String =
        "https://github.com/${UpdateConfig.GITHUB_REPO}/releases/latest/download/latest.json"

    private fun currentVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.longVersionCode
    }

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(latestJsonUrl()).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val remote = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                downloadUrl = json.getString("downloadUrl"),
                sha256 = json.getString("sha256"),
            )

            if (remote.versionCode > currentVersionCode(context)) remote else null
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}