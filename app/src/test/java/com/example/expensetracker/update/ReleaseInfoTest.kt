package com.example.expensetracker.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReleaseInfoTest {

    @Test
    fun `reads the version code and name from the release's version file`() {
        val release = ReleaseInfo.parse("versionCode=7\nversionName=1.3.0\n")

        assertEquals(ReleaseInfo(versionCode = 7, versionName = "1.3.0"), release)
    }

    @Test
    fun `tolerates Windows line endings and stray spaces`() {
        val release = ReleaseInfo.parse(" versionName = 1.3.0 \r\nversionCode = 7\r\n")

        assertEquals(ReleaseInfo(versionCode = 7, versionName = "1.3.0"), release)
    }

    @Test
    fun `a file without a version code is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.parse("versionName=1.3.0") }
    }

    @Test
    fun `a version code that isn't a number is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseInfo.parse("versionCode=seven\nversionName=1.3.0")
        }
    }

    @Test
    fun `a file without a version name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseInfo.parse("versionCode=7") }
    }
}
