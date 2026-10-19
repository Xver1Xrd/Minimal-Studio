package com.minimalstudio.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minimalstudio.app.ui.StudioApp
import com.minimalstudio.app.ui.StudioTheme

/** An incoming Android share can open a new card with its text and attachments prefilled. */
data class SharedContent(val id: Long = System.nanoTime(), val text: String = "", val uris: List<Uri> = emptyList())

class MainActivity : ComponentActivity() {
    private var share by mutableStateOf<SharedContent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        receive(intent)
        setContent {
            StudioTheme {
                val model: StudioViewModel = viewModel()
                StudioApp(model, share, onShareHandled = { share = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receive(intent)
    }

    private fun receive(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND && incoming?.action != Intent.ACTION_SEND_MULTIPLE) return
        val text = incoming.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        @Suppress("DEPRECATION")
        val uris: List<Uri> = when (incoming.action) {
            Intent.ACTION_SEND_MULTIPLE -> incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> listOfNotNull(if (Build.VERSION.SDK_INT >= 33)
                incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        }
        if (text.isNotBlank() || uris.isNotEmpty()) share = SharedContent(text = text, uris = uris)
    }
}
