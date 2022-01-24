package com.example.android.sampleplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import com.example.android.sampleplayer.extension.EMPTY_PLAYBACK_STATE
import com.example.android.sampleplayer.extension.isPlaying
import com.example.android.sampleplayer.extension.isPrepared
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber

class MusicServiceConnection(
    context: Context
) {
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }
    private lateinit var mediaController: MediaControllerCompat
    private val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected

    private val _playbackState = MutableStateFlow(EMPTY_PLAYBACK_STATE)
    val playbackState: Flow<PlaybackStateCompat> = _playbackState

    private val _playingMedia: MutableStateFlow<MediaMetadataCompat> = MutableStateFlow(NOTHING_PLAYING)
    val playingMedia: Flow<MediaMetadataCompat> = _playingMedia

    private val _currentQueue: MutableStateFlow<List<MediaSessionCompat.QueueItem>> = MutableStateFlow(emptyList())
    val currentQueue: Flow<List<MediaSessionCompat.QueueItem>> = _currentQueue

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: Flow<Boolean> = _shuffleModeEnabled

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    suspend fun playOrPause() {
        val playbackState = _playbackState.first()
        if(playbackState.isPrepared) {
            if(playbackState.isPlaying) {
                transportControls.pause()
            } else {
                transportControls.play()
            }
        } else {
            transportControls.playFromMediaId(
                Playlist.DUMMY_PLAYLIST_ID,
                bundleOf()
            )
        }
    }

    suspend fun toggleShuffleMode() {
        val shuffleEnabled = _shuffleModeEnabled.first()
        transportControls.setShuffleMode(
            if(shuffleEnabled) {
                PlaybackStateCompat.SHUFFLE_MODE_NONE
            } else {
                PlaybackStateCompat.SHUFFLE_MODE_ALL
            }
        )
    }

    suspend fun move(from: Int, to: Int) {
        if (_isConnected.first()) {
            val params = bundleOf(TimelineQueueEditor.EXTRA_FROM_INDEX to from, TimelineQueueEditor.EXTRA_TO_INDEX to to)
            mediaController.sendCommand(
                TimelineQueueEditor.COMMAND_MOVE_QUEUE_ITEM, params,
                object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        super.onReceiveResult(resultCode, resultData)
                    }
                }
            )
        }
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.value = true
        }

        override fun onConnectionSuspended() {
            _isConnected.value = false
        }

        override fun onConnectionFailed() {
            _isConnected.value = false
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let {
                _playbackState.value = it
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.let {
                _playingMedia.value = metadata
            }
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            Timber.d("onQueueChanged : ${queue?.map { it.description.mediaId }}")
            queue?.let {
                _currentQueue.value = queue
            }
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            _shuffleModeEnabled.value = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    companion object {
        val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
            .build()
    }
}