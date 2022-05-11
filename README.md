# README

## Things I want to do

* The app shows currently playing items
* The app allows reordering during the shuffle
* The result of reordering during the shuffle does not affect the song order before the shuffle

## Current approach

* Get currently playing items from [MediaControllerCompat.Callback#onQueueChanged](https://developer.android.com/reference/kotlin/android/support/v4/media/session/MediaControllerCompat.Callback#onqueuechanged)
* Customize [TimelineQueueEditor](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/ext/mediasession/TimelineQueueEditor.html), [ShuffleOrder](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/source/ShuffleOrder.html)
  * doesn't call [Player#moveMediaItem](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/Player.html#moveMediaItem(int,int)) when reorder during the shuffle
  * generate a new `ShuffleOrder` from the sorted result
  * call [ExoPlayer#setShuffleOrder](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/ExoPlayer.html#setShuffleOrder(com.google.android.exoplayer2.source.ShuffleOrder)) to update the sort order

## Result

* When reordering several times while shuffling, the song order may not be reflected in MediaSession
  * doesn't call [MediaControllerCompat.Callback#onQueueChanged](https://developer.android.com/reference/kotlin/android/support/v4/media/session/MediaControllerCompat.Callback#onqueuechanged)

## Reproduction Steps

1. Play music
1. Enable shuffling
1. Reorder playlist items several times
1. Wait to seek to next media item

## Environment

* ExoPlayer:2.17.1
* Test Device:
  * Pixel 3(OS 12)
  * Pixel 4(OS 11)
  * Emulator(OS 10)

## Music

Music provided by the [Free Music Archive](http://freemusicarchive.org/).

* [Irsen's Tale](http://freemusicarchive.org/music/Kai_Engel/Irsens_Tale/) by
[Kai Engel](http://freemusicarchive.org/music/Kai_Engel/) is licensed under [CC BY-NC 3.0](https://creativecommons.org/licenses/by-nc/3.0/).
