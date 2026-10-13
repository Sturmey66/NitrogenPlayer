package com.example.nitrogenplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class SettingsActivity : ComponentActivity() {

    private val prefsName = "NitrogenPlayerPrefs"
    private val playlistKey = "playlistUrl"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(
            prefsName,
            MODE_PRIVATE
        )

        val savedPlaylistUrl =
            preferences.getString(playlistKey, "") ?: ""

        setContent {

            var playlistUrl by remember {
                mutableStateOf(savedPlaylistUrl)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Text(
                    text = "Settings"
                )

                Text(
                    text = "Playlist URL"
                )

                TextField(
                    value = playlistUrl,
                    onValueChange = {
                        playlistUrl = it
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = {

                            lifecycleScope.launch {

                                Toast.makeText(
                                    this@SettingsActivity,
                                    "Scanning for Beryllium...",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val foundUrl =
                                    BerylliumScanner.findBerylliumUrl()

                                if (foundUrl != null) {

                                    playlistUrl = foundUrl

                                    Toast.makeText(
                                        this@SettingsActivity,
                                        "Beryllium found",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {

                                    Toast.makeText(
                                        this@SettingsActivity,
                                        "Beryllium not found",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.20f)
                    ) {
                        Text("SCAN FOR BERYLLIUM")
                    }

                    Button(
                        onClick = {

                            if (playlistUrl.isBlank()) {

                                Toast.makeText(
                                    this@SettingsActivity,
                                    "Please enter an M3U URL",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@Button
                            }

                            lifecycleScope.launch {

                                try {

                                    val playlist =
                                        withContext(Dispatchers.IO) {
                                            URL(playlistUrl).readText()
                                        }

                                    val parsedChannels =
                                        parseM3U(playlist)

                                    if (parsedChannels.isEmpty()) {

                                        Toast.makeText(
                                            this@SettingsActivity,
                                            "No channels found in playlist",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } else {

                                        preferences.edit()
                                            .putString(
                                                playlistKey,
                                                playlistUrl
                                            )
                                            .apply()

                                        val resultIntent = Intent().apply {
                                            putExtra(
                                                "PLAYLIST_URL",
                                                playlistUrl
                                            )
                                        }

                                        setResult(
                                            RESULT_OK,
                                            resultIntent
                                        )

                                        finish()
                                    }

                                } catch (e: Exception) {

                                    Toast.makeText(
                                        this@SettingsActivity,
                                        "Failed to load playlist: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.20f)
                    ) {
                        Text("SAVE PLAYLIST")
                    }

                }
            }
        }
    }
}