package com.example.android.sampleplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.*
import com.example.android.sampleplayer.extension.isPlaying
import com.example.android.sampleplayer.extension.toSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainViewModel(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    val isReady: LiveData<Boolean> = musicServiceConnection.isConnected.asLiveData(viewModelScope.coroutineContext)
    val isPlaying: LiveData<Boolean> = musicServiceConnection.playbackState.map {
        it.isPlaying
    }.asLiveData(viewModelScope.coroutineContext)
    val shuffleModeEnabled: LiveData<Boolean> = musicServiceConnection.shuffleModeEnabled.asLiveData(viewModelScope.coroutineContext)

    val playingMediaTitle: LiveData<String> = musicServiceConnection.playingMedia.map {
        it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
    }.asLiveData(viewModelScope.coroutineContext)
    private val _currentQueue: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val currentQueue: LiveData<List<Song>> = _currentQueue.asLiveData(viewModelScope.coroutineContext)

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
    }

    private var _moveFrom = ITEM_INDEX_NOT_MOVING
    private var _moveTo = ITEM_INDEX_NOT_MOVING

    init {
        viewModelScope.launch {
            musicServiceConnection.currentQueue.collect { it ->
                _currentQueue.value = it.map { queueItem ->
                        queueItem.toSong()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(Playlist.DUMMY_PLAYLIST_ID ,subscriptionCallback)
    }

    fun initialize() {
        musicServiceConnection.subscribe(Playlist.DUMMY_PLAYLIST_ID, subscriptionCallback)
    }

    fun playOrPause() {
        viewModelScope.launch {
            musicServiceConnection.playOrPause()
        }
    }

    fun toggleShuffleMode() {
        viewModelScope.launch {
            musicServiceConnection.toggleShuffleMode()
        }
    }

    fun move(from: Int, to: Int) {
        val tmp = currentQueue.value
        if(tmp != null) {
            _currentQueue.value = _currentQueue.value.toMutableList().apply {
                val backup = this[from]
                this[from] = this[to]
                this[to] = backup
            }
            if (_moveFrom == ITEM_INDEX_NOT_MOVING) {
                _moveFrom = from
            }
            _moveTo = to
        }
    }

    fun finishMove() {
        viewModelScope.launch {
            musicServiceConnection.move(_moveFrom, _moveTo)
            _moveFrom = ITEM_INDEX_NOT_MOVING
            _moveTo = ITEM_INDEX_NOT_MOVING
        }
    }

    class Factory(
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(musicServiceConnection) as T
        }
    }

    companion object {
        private const val ITEM_INDEX_NOT_MOVING = -1
    }
}