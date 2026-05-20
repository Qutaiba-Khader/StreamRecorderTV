package com.streamrecorder.core.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.streamrecorder.core.model.Recording
import com.streamrecorder.core.model.Source

class PlayerLauncher(private val context: Context) {

    enum class Player { MPV, MX, GENERIC }

    companion object {
        const val MX_PRO_PACKAGE = "com.mxtech.videoplayer.pro"
        const val MX_FREE_PACKAGE = "com.mxtech.videoplayer.ad"
        const val MPV_PACKAGE = "is.xyz.mpv"
    }

    fun playRecording(
        recording: Recording,
        source: Source,
        player: Player,
        baseUrl: String,
        useMxPro: Boolean = true,
        useStableUrl: Boolean = true,
    ): Intent {
        val url = if (useStableUrl) {
            "${baseUrl}play/${recording.id}?res=${source.resolution}"
        } else {
            source.downloadlink
        }
        val title = recording.dateTime
        return buildIntent(url, title, player, useMxPro)
    }

    fun playLiveStream(
        streamUrl: String,
        title: String,
        player: Player,
        useMxPro: Boolean = true,
    ): Intent = buildIntent(streamUrl, title, player, useMxPro)

    private fun buildIntent(
        url: String,
        title: String,
        player: Player,
        useMxPro: Boolean,
    ): Intent {
        val uri = Uri.parse(url)
        return when (player) {
            Player.MPV -> Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                setPackage(MPV_PACKAGE)
                putExtra("title", title)
            }
            Player.MX -> Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                setPackage(if (useMxPro) MX_PRO_PACKAGE else MX_FREE_PACKAGE)
                putExtra("title", title)
            }
            Player.GENERIC -> Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                putExtra("title", title)
            }
        }
    }

    fun playPlaylist(playlistUrl: String, player: Player, useMxPro: Boolean = true): Intent {
        val uri = Uri.parse(playlistUrl)
        val mimeType = "audio/x-mpegurl"
        return when (player) {
            Player.MPV -> Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                setPackage(MPV_PACKAGE)
            }
            Player.MX -> Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                setPackage(if (useMxPro) MX_PRO_PACKAGE else MX_FREE_PACKAGE)
            }
            Player.GENERIC -> Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
            }
        }
    }
}
