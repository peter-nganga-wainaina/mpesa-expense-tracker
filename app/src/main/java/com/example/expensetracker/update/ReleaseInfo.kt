package com.example.expensetracker.update

/**
 * What the latest published release says about itself, read from the `version.txt` file that CI
 * attaches next to the APK (`key=value` lines, written from the APK's own manifest).
 */
data class ReleaseInfo(val versionCode: Long, val versionName: String) {

    companion object {
        /** Fails loudly on a malformed file rather than guessing, so a bad release is never offered. */
        fun parse(versionFile: String): ReleaseInfo {
            val fields = versionFile.lines()
                .filter { '=' in it }
                .associate { line -> line.substringBefore('=').trim() to line.substringAfter('=').trim() }
            val versionCode = requireNotNull(fields["versionCode"]?.toLongOrNull()) {
                "version file has no numeric versionCode: $versionFile"
            }
            val versionName = requireNotNull(fields["versionName"]?.takeIf { it.isNotEmpty() }) {
                "version file has no versionName: $versionFile"
            }
            return ReleaseInfo(versionCode, versionName)
        }
    }
}
