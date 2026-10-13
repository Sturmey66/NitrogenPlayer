package com.example.nitrogenplayer

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import androidx.lifecycle.lifecycleScope
import androidx.core.net.toUri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import android.view.WindowManager

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private lateinit var player: ExoPlayer

    private var channelUrls = ArrayList<String>()
    private var channelNames = ArrayList<String>()
    private var currentChannelIndex = 0

    private var showChannelName by mutableStateOf(false)
    private var displayedChannelName by mutableStateOf("")

    private var channelNameJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val channelUrl = intent.getStringExtra("CHANNEL_URL")

        if (channelUrl.isNullOrBlank()) {
            finish()
            return
        }

        currentChannelIndex =
            intent.getIntExtra("CHANNEL_INDEX", 0)

        channelUrls =
            intent.getStringArrayListExtra("CHANNEL_URLS")
                ?: arrayListOf(channelUrl)

        channelNames =
            intent.getStringArrayListExtra("CHANNEL_NAMES")
                ?: arrayListOf("")

        // Show channel name on initial playback
        showChannelNameForCurrentChannel()

        android.util.Log.d(
            "NITROGEN_URL",
            "Player URL: $channelUrl"
        )

        player = ExoPlayer.Builder(this).build()

        player.addAnalyticsListener(
            EventLogger("NITROGEN_EVENT")
        )

        player.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {

                android.util.Log.e(
                    "NITROGEN_HLS",
                    "Playback error: ${error.errorCodeName}",
                    error
                )

                Toast.makeText(
                    this@PlayerActivity,
                    "Playback error: ${error.errorCodeName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        player.setMediaItem(
            MediaItem.fromUri(channelUrl.toUri())
        )

        player.prepare()
        player.playWhenReady = true

        setContent {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                AndroidView(
                    factory = { context ->

                        PlayerView(context).apply {

                            player = this@PlayerActivity.player

                            useController = true

                            // Hide player controls almost immediately.
                            // 5 = 5 milliseconds.
                            setControllerShowTimeoutMs(5)

                            visibility = View.VISIBLE

                            setBackgroundColor(Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (showChannelName) {

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                bottom = 150.dp
                            )
                    ) {

                        // Black outline
                        Text(
                            text = displayedChannelName,
                            color = ComposeColor.Black,
                            fontSize = 40.sp,
                            style = TextStyle(
                                fontSize = 40.sp
                            ),
                            modifier = Modifier
                                .padding(4.dp)
                        )

                        // White text
                        Text(
                            text = displayedChannelName,
                            color = ComposeColor.White,
                            fontSize = 40.sp,
                            style = TextStyle(
                                fontSize = 40.sp
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent
    ): Boolean {

        when (keyCode) {

            // UP or CH- = previous channel
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {

                changeChannel(-1)

                return true
            }

            // DOWN or CH+ = next channel
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_CHANNEL_UP -> {

                changeChannel(1)

                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun changeChannel(direction: Int) {

        if (channelUrls.isEmpty()) {
            return
        }

        currentChannelIndex += direction

        if (currentChannelIndex < 0) {
            currentChannelIndex = channelUrls.lastIndex
        }

        if (currentChannelIndex > channelUrls.lastIndex) {
            currentChannelIndex = 0
        }

        val newUrl = channelUrls[currentChannelIndex]

        android.util.Log.d(
            "NITROGEN_CHANNEL",
            "Changing to channel ${currentChannelIndex + 1}: $newUrl"
        )

        player.stop()

        player.setMediaItem(
            MediaItem.fromUri(newUrl.toUri())
        )

        player.prepare()
        player.playWhenReady = true

        // Show the new channel name and restart its timer
        showChannelNameForCurrentChannel()
    }

    private fun showChannelNameForCurrentChannel() {

        displayedChannelName =
            channelNames.getOrNull(currentChannelIndex)
                ?: ""

        showChannelName = true

        // Cancel any existing timer
        channelNameJob?.cancel()

        // Keep the channel name visible for 2 seconds
        channelNameJob = lifecycleScope.launch {

            delay(2.seconds)

            showChannelName = false
        }
    }

    override fun onStop() {
        super.onStop()

        if (::player.isInitialized) {
            player.pause()
        }
    }

    override fun onDestroy() {

        channelNameJob?.cancel()

        if (::player.isInitialized) {
            player.release()
        }

        super.onDestroy()
    }
}