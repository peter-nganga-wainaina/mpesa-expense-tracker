package com.example.expensetracker.update

import com.example.expensetracker.testing.FakeReleaseSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val releases = FakeReleaseSource()

    private fun checkWith(installedVersionCode: Long) =
        runBlocking { UpdateChecker(releases, installedVersionCode).check() }

    @Test
    fun `a release newer than the installed app is offered`() {
        releases.publish(versionCode = 3, versionName = "1.2.0")

        assertEquals(UpdateCheck.Available(ReleaseInfo(3, "1.2.0")), checkWith(installedVersionCode = 2))
    }

    @Test
    fun `the release already installed is not offered`() {
        releases.publish(versionCode = 2)

        assertEquals(UpdateCheck.UpToDate, checkWith(installedVersionCode = 2))
    }

    @Test
    fun `an older release is not offered, since Android won't install a downgrade`() {
        releases.publish(versionCode = 1)

        assertEquals(UpdateCheck.UpToDate, checkWith(installedVersionCode = 2))
    }

    @Test
    fun `no network is reported as a failed check, not as up to date`() {
        releases.versionFile = null

        assertTrue(checkWith(installedVersionCode = 2) is UpdateCheck.Failed)
    }

    @Test
    fun `a malformed version file is reported as a failed check`() {
        releases.versionFile = "<html>Not Found</html>"

        assertTrue(checkWith(installedVersionCode = 2) is UpdateCheck.Failed)
    }
}
