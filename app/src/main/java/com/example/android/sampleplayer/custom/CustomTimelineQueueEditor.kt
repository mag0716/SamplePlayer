package com.example.android.sampleplayer.custom

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor.*
import com.google.android.exoplayer2.util.Util

class CustomTimelineQueueEditor @JvmOverloads constructor(
    private val mediaController: MediaControllerCompat,
    private val queueDataAdapter: QueueDataAdapter,
    private val mediaDescriptionConverter: MediaDescriptionConverter,
    private val equalityChecker: MediaDescriptionEqualityChecker = MediaIdEqualityChecker()
) : MediaSessionConnector.QueueEditor, MediaSessionConnector.CommandReceiver {

    interface MediaDescriptionConverter {
        fun convert(description: MediaDescriptionCompat?): MediaItem?
    }

    interface QueueDataAdapter {
        fun add(position: Int, description: MediaDescriptionCompat)

        fun remove(position: Int)

        fun move(
            from: Int,
            to: Int,
            handleOnPlayer: Boolean,
            currentQueue: List<MediaSessionCompat.QueueItem>
        )
    }

    interface MediaDescriptionEqualityChecker {
        fun equals(d1: MediaDescriptionCompat, d2: MediaDescriptionCompat): Boolean
    }

    class MediaIdEqualityChecker : MediaDescriptionEqualityChecker {
        override fun equals(d1: MediaDescriptionCompat, d2: MediaDescriptionCompat): Boolean {
            return Util.areEqual(d1.mediaId, d2.mediaId)
        }
    }

    override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat) {
        onAddQueueItem(player, description, player.currentTimeline.windowCount)
    }

    override fun onAddQueueItem(player: Player, description: MediaDescriptionCompat, index: Int) {
        val mediaItem = mediaDescriptionConverter.convert(description)
        if (mediaItem != null) {
            queueDataAdapter.add(index, description)
            player.addMediaItem(index, mediaItem)
        }
    }

    override fun onRemoveQueueItem(player: Player, description: MediaDescriptionCompat) {
        val queue = mediaController.queue
        for (i in queue.indices) {
            if (equalityChecker.equals(queue[i].description, description)) {
                queueDataAdapter.remove(i)
                player.removeMediaItem(i)
                return
            }
        }
    }

    override fun onCommand(
        player: Player,
        command: String,
        extras: Bundle?,
        cb: ResultReceiver?
    ): Boolean {
        if (COMMAND_MOVE_QUEUE_ITEM != command || extras == null) {
            return false
        }
        val from = extras.getInt(EXTRA_FROM_INDEX, C.INDEX_UNSET)
        val to = extras.getInt(EXTRA_TO_INDEX, C.INDEX_UNSET)
        if (from != C.INDEX_UNSET && to != C.INDEX_UNSET) {
            // !!!customize for support reordering during the shuffle.!!!
            val handleOnPlayer = player.shuffleModeEnabled.not()
            queueDataAdapter.move(from, to, handleOnPlayer, mediaController.queue)
            if (handleOnPlayer) {
                player.moveMediaItem(from, to)
            }
            // !!!customize for support reordering during the shuffle.!!!
        }
        return true
    }
}