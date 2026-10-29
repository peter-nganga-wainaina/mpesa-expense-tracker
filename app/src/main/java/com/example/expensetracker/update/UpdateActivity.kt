package com.example.expensetracker.update

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * The safety net: a bare update screen, reached from the update notification and from the "Check for
 * updates" shortcut on the launcher icon. It runs in its own process (see the manifest) and uses plain
 * views, with no database, Compose theme or app screens, so a release that crashes while starting can
 * still install the release that fixes it.
 */
class UpdateActivity : ComponentActivity() {

    private val releases = GitHubReleases()
    private val installer = UpdateInstaller(releases)
    private lateinit var message: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        message = TextView(this).apply { textSize = 18f }
        button = Button(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            addView(message)
            addView(button)
        })
        check()
    }

    private fun check() {
        message.text = "Checking for updates…"
        button.visibility = View.GONE
        lifecycleScope.launch {
            val check = UpdateChecker(releases, installedVersionCode(this@UpdateActivity)).check()
            if (check is UpdateCheck.Failed) Log.w(TAG, "Update check failed", check.cause)
            show(updateScreenState(check))
        }
    }

    private fun show(state: UpdateScreenState) {
        message.text = state.message
        button.visibility = View.VISIBLE
        button.isEnabled = true
        when (state.action) {
            UpdateAction.INSTALL -> button.label("Update") { install() }
            UpdateAction.RETRY -> button.label("Try again") { check() }
            UpdateAction.NONE -> button.label("Close") { finish() }
        }
    }

    private fun install() {
        message.text = "Downloading the update…"
        button.isEnabled = false
        lifecycleScope.launch {
            try {
                installer.downloadAndInstall(this@UpdateActivity)
                // Android's installer is on screen now; back here, the user can tap Update again.
                button.isEnabled = true
                message.text = "Tap Update if the install prompt didn't appear."
            } catch (e: IOException) {
                Log.w(TAG, "Update download failed", e)
                show(UpdateScreenState("The update didn't download. Check the connection and try again.", UpdateAction.RETRY))
            }
        }
    }

    private fun Button.label(text: String, onClick: () -> Unit) {
        this.text = text
        setOnClickListener { onClick() }
    }

    private companion object {
        const val TAG = "UpdateActivity"
    }
}
