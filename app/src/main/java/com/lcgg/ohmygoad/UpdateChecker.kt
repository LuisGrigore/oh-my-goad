package com.lcgg.ohmygoad

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val TAG = "UpdateChecker"

    private fun latestJsonUrl(): String =
        "https://github.com/${UpdateConfig.GITHUB_REPO}/releases/latest/download/latest.json"

    private fun currentVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.longVersionCode
    }

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking for update at ${latestJsonUrl()}")

        try {
            val connection = URL(latestJsonUrl()).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            Log.d(TAG, "Response code: ${connection.responseCode}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Non-200 response, aborting update check")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Response body: $body")

            val json = JSONObject(body)

            val remote = UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                downloadUrl = json.getString("downloadUrl"),
                sha256 = json.getString("sha256"),
            )

            val current = currentVersionCode(context)
            Log.d(TAG, "Installed versionCode=$current, remote versionCode=${remote.versionCode}")

            if (remote.versionCode > current) {
                Log.i(TAG, "Update available: ${remote.versionName}")
                remote
            } else {
                Log.i(TAG, "No update needed")
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Could not read installed package info", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }
}