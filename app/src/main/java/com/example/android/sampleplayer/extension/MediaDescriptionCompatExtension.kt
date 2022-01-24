package com.example.android.sampleplayer.extension

import android.support.v4.media.MediaDescriptionCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata

fun MediaDescriptionCompat.toMediaBrowserCompatMediaItem() = MediaItem.Builder()
    .setMediaId("$mediaId")
    .setUri(mediaUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .build()
    ).build()