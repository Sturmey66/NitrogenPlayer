package com.example.nitrogenplayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : ComponentActivity() {

    private val prefsName = "NitrogenPlayerPrefs"
    private val playlistKey = "playlistUrl"

    private var backPressCount = 0
    private var lastBackPressTime = 0L

    private val backPressResetTime = 2000L

    private var playlistUrl by mutableStateOf("")
    private var channels by mutableStateOf<List<Channel>>(emptyList())

    private val settingsLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                val newPlaylistUrl =
                    result.data?.getStringExtra("PLAYLIST_URL")

                if (!newPlaylistUrl.isNullOrBlank()) {
                    playlistUrl = newPlaylistUrl
                    loadPlaylist(newPlaylistUrl)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentTime = System.currentTimeMillis()

                    if (currentTime - lastBackPressTime > backPressResetTime) {
                        backPressCount = 0
                    }

                    lastBackPressTime = currentTime
                    backPressCount++

                    if (backPressCount >= 3) {
                        backPressCount = 0
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        return
                    }

                    val remaining = 3 - backPressCount

                    Toast.makeText(
                        this@MainActivity,
                        "Press BACK $remaining more time${if (remaining == 1) "" else "s"} to exit",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val preferences = getSharedPreferences(
            prefsName,
            MODE_PRIVATE
        )

        playlistUrl = preferences.getString(playlistKey, "") ?: ""

        if (playlistUrl.isBlank()) {
            val intent = Intent(this, SettingsActivity::class.java)
            settingsLauncher.launch(intent)
        } else {
            loadPlaylist(playlistUrl)
        }

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NitrogenPlayer",
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val intent = Intent(
                                this@MainActivity,
                                SettingsActivity::class.java
                            )
                            settingsLauncher.launch(intent)
                        }
                    ) {
                        Text("SETTINGS")
                    }
                }

                if (channels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(channels) { index, channel ->
                            var isFocused by remember {
                                mutableStateOf(false)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .onFocusChanged {
                                        isFocused = it.isFocused
                                    }
                                    .background(
                                        color = if (isFocused) {
                                            Color.White
                                        } else {
                                            Color(0xFFCCFFCC)
                                        }
                                    )
                                    .border(
                                        width = 4.dp,
                                        color = Color.Black
                                    )
                                    .clickable {
                                        val intent = Intent(
                                            this@MainActivity,
                                            PlayerActivity::class.java
                                        )

                                        intent.putExtra(
                                            "CHANNEL_URL",
                                            channel.url
                                        )

                                        intent.putExtra(
                                            "CHANNEL_INDEX",
                                            index
                                        )

                                        intent.putStringArrayListExtra(
                                            "CHANNEL_URLS",
                                            ArrayList(channels.map { it.url })
                                        )

                                        intent.putStringArrayListExtra(
                                            "CHANNEL_NAMES",
                                            ArrayList(channels.map { it.name })
                                        )

                                        startActivity(intent)
                                    }
                                    .focusable()
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (channel.logoUrl != null) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(
                                                androidx.compose.foundation
                                                    .shape
                                                    .RoundedCornerShape(6.dp)
                                            ),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("NO LOGO")
                                    }
                                }

                                Text(
                                    text = channel.name,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nitrogen Player 2026 v1.0",
                        color = Color.White
                    )
                }
            }
        }
    }

    private fun loadPlaylist(url: String) {
        lifecycleScope.launch {
            try {
                val playlist = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }

                val parsedChannels = parseM3U(playlist)

                if (parsedChannels.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No channels found in playlist",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val preferences = getSharedPreferences(
                        prefsName,
                        MODE_PRIVATE
                    )

                    preferences.edit()
                        .putString(playlistKey, url)
                        .apply()

                    channels = parsedChannels

                    Toast.makeText(
                        this@MainActivity,
                        "Playlist loaded: ${parsedChannels.size} channels",
                        Toast.LENGTH_LONG
                    ).show()

                    // Automatically start the first channel.
                    val intent = Intent(
                        this@MainActivity,
                        PlayerActivity::class.java
                    )

                    intent.putExtra(
                        "CHANNEL_URL",
                        parsedChannels[0].url
                    )

                    intent.putExtra("CHANNEL_INDEX", 0)

                    intent.putStringArrayListExtra(
                        "CHANNEL_URLS",
                        ArrayList(parsedChannels.map { it.url })
                    )

                    intent.putStringArrayListExtra(
                        "CHANNEL_NAMES",
                        ArrayList(parsedChannels.map { it.name })
                    )

                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load playlist: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
