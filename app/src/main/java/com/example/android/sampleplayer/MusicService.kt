package com.example.android.sampleplayer

import CustomShuffleOrder
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import androidx.media.MediaBrowserServiceCompat
import com.example.android.sampleplayer.custom.CustomTimelineQueueEditor
import com.example.android.sampleplayer.extension.toMediaBrowserCompatMediaItem
import com.example.android.sampleplayer.extension.toMediaDescriptionCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock
import kotlinx.coroutines.launch

class MusicService : MediaBrowserServiceCompat(), LifecycleOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var defaultTrackSelector: DefaultTrackSelector
    private lateinit var mediaSourceFactory: MediaSource.Factory

    private val musicPlayerListener = MusicPlayerListener()

    override fun onCreate() {
        super.onCreate()
        dispatcher.onServicePreSuperOnCreate()

        defaultTrackSelector = DefaultTrackSelector(this)
        mediaSourceFactory = DefaultMediaSourceFactory(this)
        exoPlayer = ExoPlayer.Builder(
            this,
            DefaultRenderersFactory(this),
            mediaSourceFactory,
            defaultTrackSelector,
            DefaultLoadControl(),
            DefaultBandwidthMeter.getSingletonInstance(this),
            DefaultAnalyticsCollector(Clock.DEFAULT)
        ).setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MUSIC).build(),
            true
        ).setHandleAudioBecomingNoisy(true)
            .build()
        exoPlayer.addListener(musicPlayerListener)

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(MusicPlaybackPrepare())
            setQueueNavigator(object : TimelineQueueNavigator(mediaSession, MAX_QUEUE_SIZE) {
                override fun getMediaDescription(
                    player: Player,
                    windowIndex: Int
                ): MediaDescriptionCompat {
                    return player.getMediaItemAt(windowIndex).toMediaDescriptionCompat()
                }
            })
            // !!!customize for support reordering during the shuffle.!!!
            setQueueEditor(
                CustomTimelineQueueEditor(
                    mediaSession.controller,
                    object : CustomTimelineQueueEditor.QueueDataAdapter {
                        override fun add(position: Int, description: MediaDescriptionCompat) {
                            // not support
                        }

                        override fun remove(position: Int) {
                            // not support
                        }

                        override fun move(
                            from: Int,
                            to: Int,
                            handleOnPlayer: Boolean,
                            currentQueue: List<MediaSessionCompat.QueueItem>
                        ) {
                            if (handleOnPlayer.not()) {
                                exoPlayer.setShuffleOrder(
                                    CustomShuffleOrder.cloneAndMove(
                                        currentQueue.map { it.queueId.toInt() }.toList()
                                            .toIntArray(),
                                        from,
                                        to
                                    )
                                )
                            }
                        }
                    },
                    object : CustomTimelineQueueEditor.MediaDescriptionConverter {
                        override fun convert(description: MediaDescriptionCompat?): MediaItem? {
                            return description?.toMediaBrowserCompatMediaItem()
                        }
                    }
                )
            )
            // !!!customize for support reordering during the shuffle.!!!
            setPlayer(exoPlayer)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession.run {
            isActive = false
            release()
        }
        exoPlayer.removeListener(musicPlayerListener)
        exoPlayer.release()
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    override fun getLifecycle(): Lifecycle = dispatcher.lifecycle

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(
            ROOT_ID,
            bundleOf()
        )
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        lifecycleScope.launch {
            val playlist = Playlist.createDummyPlaylist(packageName)
            result.sendResult(playlist.songList.map { it.toMediaBrowserCompatMediaItem() })
        }
    }

    private inner class MusicPlayerListener : Player.Listener {
    }

    private inner class MusicPlaybackPrepare : MediaSessionConnector.PlaybackPreparer {
        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ) = false

        override fun getSupportedPrepareActions(): Long {
            return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        }

        override fun onPrepare(playWhenReady: Boolean) {
            // noop
        }

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            val playlist = Playlist.createDummyPlaylist(packageName)

            exoPlayer.setShuffleOrder(CustomShuffleOrder(0))
            exoPlayer.setMediaSources(playlist.songList.map { it.toMediaSource(mediaSourceFactory) })
            exoPlayer.prepare()
            exoPlayer.seekTo(0, C.TIME_UNSET)
            exoPlayer.playWhenReady = playWhenReady
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
            // noop
        }

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
            // noop
        }
    }

    companion object {
        private const val TAG = "MusicService"
        private const val ROOT_ID = "RootID"
        private const val MAX_QUEUE_SIZE = 10
    }
}