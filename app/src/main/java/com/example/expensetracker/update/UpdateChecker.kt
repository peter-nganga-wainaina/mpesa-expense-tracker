package com.example.expensetracker.update

import java.io.IOException

/** Where the latest release's version file comes from. Implemented over HTTPS by [GitHubReleases]. */
fun interface ReleaseSource {
    /** @throws IOException when it can't be fetched (no network, or the repo isn't public yet). */
    suspend fun latestVersionFile(): String
}

sealed interface UpdateCheck {
    data object UpToDate : UpdateCheck
    data class Available(val release: ReleaseInfo) : UpdateCheck
    /** Kept apart from [UpToDate], so a check that never reached GitHub isn't mistaken for "nothing new". */
    data class Failed(val cause: Exception) : UpdateCheck
}

/** Answers whether the latest release is newer than the installed app. Only reads; never downloads. */
class UpdateChecker(private val releases: ReleaseSource, private val installedVersionCode: Long) {

    suspend fun check(): UpdateCheck = try {
        val latest = ReleaseInfo.parse(releases.latestVersionFile())
        // Strictly newer: Android refuses to install a lower version code over a higher one.
        if (latest.versionCode > installedVersionCode) UpdateCheck.Available(latest) else UpdateCheck.UpToDate
    } catch (e: IOException) {
        UpdateCheck.Failed(e)
    } catch (e: IllegalArgumentException) {
        UpdateCheck.Failed(e)
    }
}
