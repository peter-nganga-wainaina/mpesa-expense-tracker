package com.example.expensetracker.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads the latest release's APK and hands it to Android's installer, which always asks the user
 * to confirm. Android itself refuses the APK unless it carries this app's ID and signing key, so a
 * tampered download can't replace the app.
 */
class UpdateInstaller(private val releases: GitHubReleases) {

    /** @throws java.io.IOException when the download fails; nothing is handed to the installer then. */
    suspend fun downloadAndInstall(context: Context) {
        val apk = File(context.cacheDir, "updates/update.apk")
        releases.downloadApk(apk)
        // The installer is another app, so it reads the file through a content URI rather than a path.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val install = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(install)
    }
}
