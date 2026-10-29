package com.example.expensetracker.update

/** The one button the standalone update screen offers, if any. */
enum class UpdateAction { INSTALL, RETRY, NONE }

/** What the standalone update screen says and offers for one check's outcome. */
data class UpdateScreenState(val message: String, val action: UpdateAction)

fun updateScreenState(check: UpdateCheck): UpdateScreenState = when (check) {
    is UpdateCheck.Available ->
        UpdateScreenState("Version ${check.release.versionName} is ready to install.", UpdateAction.INSTALL)
    UpdateCheck.UpToDate -> UpdateScreenState("You have the latest version.", UpdateAction.NONE)
    // Never "up to date": a check that didn't reach GitHub knows nothing about newer versions.
    is UpdateCheck.Failed ->
        UpdateScreenState("Couldn't check for updates. Check the connection and try again.", UpdateAction.RETRY)
}
