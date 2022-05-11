package com.example.android.sampleplayer.extension

import android.support.v4.media.MediaDescriptionCompat
import com.google.android.exoplayer2.MediaItem

fun MediaItem.toMediaDescriptionCompat(): MediaDescriptionCompat = MediaDescriptionCompat.Builder()
    .setMediaId(mediaId)
    .setMediaUri(localConfiguration?.uri)
    .setTitle(mediaMetadata.title)
    .build()