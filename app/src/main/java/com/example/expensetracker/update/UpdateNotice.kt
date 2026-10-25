package com.example.expensetracker.update

/**
 * Whether the daily check should notify about [release]. Once per version: the in-app banner keeps
 * offering it after that, so the notification shade isn't nagged every day.
 */
fun shouldAnnounce(release: ReleaseInfo, lastAnnouncedVersionCode: Long?): Boolean =
    lastAnnouncedVersionCode == null || release.versionCode > lastAnnouncedVersionCode
