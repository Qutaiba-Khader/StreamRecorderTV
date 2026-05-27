package com.streamrecorder.tvapp

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewChannelHelper
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat

object ChannelHelper {
    private const val TAG = "ChannelHelper"
    private const val CHANNEL_KEY = "live_now_channel_id"

    fun updateLiveChannel(context: Context, targets: List<Target>) {
        try {
            val helper = PreviewChannelHelper(context)
            val liveTargets = targets.filter { it.isLive }
            val channelId = getOrCreateChannel(context, helper)
            if (channelId < 0) return

            deleteAllPrograms(context, channelId)

            if (liveTargets.isEmpty()) return

            for (target in liveTargets) {
                val logoUrl = target.logo?.replace("250x250", "56x56") ?: ""

                val program = PreviewProgram.Builder()
                    .setChannelId(channelId)
                    .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                    .setTitle("${target.name} LIVE")
                    .setDescription("${target.platform} · ${target.countTotal} recordings")
                    .setPosterArtUri(Uri.parse(logoUrl))
                    .setIntentUri(Uri.parse("streamrecorder://live/${target.id}"))
                    .setInternalProviderId(target.id.toString())
                    .setLive(true)
                    .build()

                context.contentResolver.insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    program.toContentValues()
                )
            }
            Log.d(TAG, "Updated Live Now channel: ${liveTargets.size} programs")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update channel", e)
        }
    }

    private fun deleteAllPrograms(context: Context, channelId: Long) {
        try {
            val cursor = context.contentResolver.query(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                arrayOf(TvContractCompat.PreviewPrograms._ID, TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID),
                null, null, null
            )
            cursor?.use {
                val idIndex = it.getColumnIndex(TvContractCompat.PreviewPrograms._ID)
                val chIndex = it.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID)
                while (it.moveToNext()) {
                    if (it.getLong(chIndex) == channelId) {
                        val programId = it.getLong(idIndex)
                        context.contentResolver.delete(
                            TvContractCompat.buildPreviewProgramUri(programId),
                            null, null
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete programs", e)
        }
    }

    private fun getOrCreateChannel(context: Context, helper: PreviewChannelHelper): Long {
        val prefs = context.getSharedPreferences("sr_settings", Context.MODE_PRIVATE)
        val savedId = prefs.getLong(CHANNEL_KEY, -1)

        if (savedId >= 0) {
            try {
                val existing = helper.allChannels
                if (existing.any { it.id == savedId }) return savedId
            } catch (_: Exception) {}
        }

        val channel = PreviewChannel.Builder()
            .setDisplayName("StreamRecorder — Live Now")
            .setAppLinkIntentUri(Uri.parse("streamrecorder://home"))
            .setInternalProviderId("live_now")
            .build()

        val channelUri = context.contentResolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            channel.toContentValues()
        ) ?: return -1

        val newId = ContentUris.parseId(channelUri)
        prefs.edit().putLong(CHANNEL_KEY, newId).apply()

        TvContractCompat.requestChannelBrowsable(context, newId)
        Log.d(TAG, "Created Live Now channel id=$newId")
        return newId
    }
}
