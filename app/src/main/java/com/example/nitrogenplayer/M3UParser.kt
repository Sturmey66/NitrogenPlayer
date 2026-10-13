package com.example.nitrogenplayer

fun parseM3U(content: String): List<Channel> {

    val channels = mutableListOf<Channel>()

    var pendingName: String? = null
    var pendingLogo: String? = null

    val lines = content.lines()

    for (line in lines) {

        val trimmed = line.trim()

        if (trimmed.startsWith("#EXTINF:")) {

            // Everything after the final comma is the channel name.
            pendingName = trimmed
                .substringAfterLast(",")
                .trim()

            // Extract tvg-logo="..."
            val logoMatch = Regex(
                """tvg-logo=["']([^"']*)["']"""
            ).find(trimmed)

            pendingLogo = logoMatch?.groupValues?.get(1)

        } else if (
            trimmed.isNotEmpty() &&
            !trimmed.startsWith("#") &&
            pendingName != null
        ) {

            channels.add(
                Channel(
                    name = pendingName,
                    url = trimmed,
                    logoUrl = pendingLogo
                )
            )

            pendingName = null
            pendingLogo = null
        }
    }

    return channels
}