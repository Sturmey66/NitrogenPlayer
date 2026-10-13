package com.example.nitrogenplayer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

object BerylliumScanner {

    suspend fun findBerylliumUrl(): String? {

        return withContext(Dispatchers.IO) {

            try {

                Log.d(
                    "BERYLLIUM_SCAN",
                    "===== Starting Beryllium scan ====="
                )

                val interfaces =
                    NetworkInterface.getNetworkInterfaces()

                for (networkInterface in interfaces) {

                    Log.d(
                        "BERYLLIUM_SCAN",
                        "Interface: ${networkInterface.name} " +
                                "up=${networkInterface.isUp} " +
                                "loopback=${networkInterface.isLoopback}"
                    )

                    if (!networkInterface.isUp ||
                        networkInterface.isLoopback
                    ) {
                        continue
                    }

                    for (address in networkInterface.inetAddresses) {

                        if (address !is Inet4Address ||
                            address.isLoopbackAddress
                        ) {
                            continue
                        }

                        val hostAddress =
                            address.hostAddress ?: continue

                        Log.d(
                            "BERYLLIUM_SCAN",
                            "IPv4 address found: $hostAddress"
                        )

                        val parts =
                            hostAddress.split(".")

                        if (parts.size != 4) {
                            continue
                        }

                        val base =
                            "${parts[0]}.${parts[1]}.${parts[2]}."

                        Log.d(
                            "BERYLLIUM_SCAN",
                            "Scanning subnet: ${base}1 - ${base}254"
                        )

                        for (i in 1..254) {

                            val ip = "$base$i"

                            try {

                                val url =
                                    URL(
                                        "http://$ip:8080/service.m3u"
                                    )

                                val connection =
                                    url.openConnection()
                                            as HttpURLConnection

                                connection.connectTimeout = 500
                                connection.readTimeout = 500
                                connection.requestMethod = "GET"

                                Log.d(
                                    "BERYLLIUM_SCAN",
                                    "Testing $ip:8080"
                                )

                                val responseCode =
                                    connection.responseCode

                                Log.d(
                                    "BERYLLIUM_SCAN",
                                    "$ip returned HTTP $responseCode"
                                )

                                if (responseCode == 200) {

                                    val content =
                                        connection.inputStream
                                            .bufferedReader()
                                            .use { it.readText() }

                                    Log.d(
                                        "BERYLLIUM_SCAN",
                                        "$ip returned ${content.length} bytes"
                                    )

                                    if (
                                        content.trimStart()
                                            .startsWith("#EXTM3U")
                                    ) {

                                        Log.d(
                                            "BERYLLIUM_SCAN",
                                            "***** BERYLLIUM FOUND: $url *****"
                                        )

                                        connection.disconnect()

                                        return@withContext url.toString()
                                    }

                                    Log.d(
                                        "BERYLLIUM_SCAN",
                                        "$ip responded, but was not an M3U playlist"
                                    )
                                }

                                connection.disconnect()

                            } catch (e: Exception) {

                                Log.d(
                                    "BERYLLIUM_SCAN",
                                    "$ip failed: ${e.javaClass.simpleName}"
                                )
                            }
                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "BERYLLIUM_SCAN",
                    "Scanner error",
                    e
                )
            }

            Log.d(
                "BERYLLIUM_SCAN",
                "===== Beryllium scan finished: NOT FOUND ====="
            )

            null
        }
    }
}