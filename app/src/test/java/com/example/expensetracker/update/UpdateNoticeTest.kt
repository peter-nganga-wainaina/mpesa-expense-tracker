package com.example.expensetracker.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNoticeTest {

    private val release = ReleaseInfo(versionCode = 5, versionName = "1.4.0")

    @Test
    fun `the first time a release is found, it is announced`() {
        assertTrue(shouldAnnounce(release, lastAnnouncedVersionCode = null))
    }

    @Test
    fun `a release already announced is not announced again the next day`() {
        assertFalse(shouldAnnounce(release, lastAnnouncedVersionCode = 5))
    }

    @Test
    fun `a newer release than the one last announced is announced`() {
        assertTrue(shouldAnnounce(release, lastAnnouncedVersionCode = 4))
    }
}
