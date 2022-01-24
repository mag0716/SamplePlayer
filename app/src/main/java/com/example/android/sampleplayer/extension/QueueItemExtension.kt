package com.example.android.sampleplayer.extension

import android.support.v4.media.session.MediaSessionCompat
import com.example.android.sampleplayer.Song

fun MediaSessionCompat.QueueItem.toSong() = Song(
    description.mediaId!!,
    description.title!!.toString(),
    description.mediaUri!!.toString()
)