package com.example.expensetracker.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * The app's GitHub Releases, the only place it ever connects to. GitHub redirects
 * `releases/latest/download/<file>` to that file in the newest release, so no API call or JSON is needed.
 * The repo must be public, or every request here fails (which the checker reports as a failed check).
 */
class GitHubReleases : ReleaseSource {

    override suspend fun latestVersionFile(): String = withContext(Dispatchers.IO) {
        open(VERSION_FILE).use { it.readBytes().decodeToString() }
    }

    /** Streams the latest release's APK into [target], replacing it only once the download is complete. */
    suspend fun downloadApk(target: File) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val partial = File(target.path + ".part")
        open(APK_FILE).use { input -> partial.outputStream().use { input.copyTo(it) } }
        if (!partial.renameTo(target)) throw IOException("couldn't move the downloaded APK into place")
    }

    private fun open(fileName: String): InputStream {
        val connection = URL(LATEST_DOWNLOAD + fileName).openConnection() as HttpURLConnection
        // HttpURLConnection's default timeouts are 0, meaning wait forever; a stalled network must not
        // hang the daily worker or leave the banner downloading indefinitely.
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        // Already the default; stated because every file sits behind a redirect to GitHub's file host.
        connection.instanceFollowRedirects = true
        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            throw IOException("GET $fileName from GitHub Releases: HTTP $status")
        }
        return connection.inputStream
    }

    private companion object {
        const val LATEST_DOWNLOAD =
            "https://github.com/peter-nganga-wainaina/mpesa-expense-tracker/releases/latest/download/"
        // Both names are fixed by .github/workflows/release.yml, which attaches them to each release.
        const val VERSION_FILE = "version.txt"
        const val APK_FILE = "expense-tracker.apk"
    }
}
