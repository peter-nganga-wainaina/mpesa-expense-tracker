package com.example.expensetracker.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateScreenStateTest {

    @Test
    fun `a newer release is offered for install, naming its version`() {
        val state = updateScreenState(UpdateCheck.Available(ReleaseInfo(versionCode = 4, versionName = "1.3")))

        assertEquals(UpdateAction.INSTALL, state.action)
        assertTrue(state.message.contains("1.3"))
    }

    @Test
    fun `when already up to date there is nothing to do`() {
        assertEquals(UpdateAction.NONE, updateScreenState(UpdateCheck.UpToDate).action)
    }

    @Test
    fun `a failed check offers to try again rather than claiming to be up to date`() {
        assertEquals(UpdateAction.RETRY, updateScreenState(UpdateCheck.Failed(java.io.IOException("offline"))).action)
    }
}
