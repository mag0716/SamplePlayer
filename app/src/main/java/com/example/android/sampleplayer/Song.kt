package com.example.android.sampleplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory

data class Song(
    val id: String,
    val name: String,
    val contentUrl: String
) {
    fun toMediaBrowserCompatMediaItem(): MediaBrowserCompat.MediaItem =
        MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId(id)
                .setTitle(name)
                .setMediaUri(contentUrl.toUri())
                .build(),
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )

    fun toMediaSource(mediaSourceFactory: MediaSourceFactory) : MediaSource =
        mediaSourceFactory.createMediaSource(toExoPlayerMediaItem())

    private fun toExoPlayerMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(contentUrl.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .build()
            ).build()
    }
}