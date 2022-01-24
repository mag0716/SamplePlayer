package com.example.android.sampleplayer

import android.content.ContentResolver

class Playlist(
    val id: String,
    val title: String,
    val songList: List<Song>
) {
    companion object {

        const val DUMMY_PLAYLIST_ID = "dummyPlaylist"

        fun createDummyPlaylist(packageName: String): Playlist {
            val songTitleList = listOf(
                "Intro (.udonthear)",
                "Leaving",
                "Irsen's Tale",
                "Moonlight Reprise",
                "Nothing Lasts Forever",
                "The Moments of Our Mornings",
                "Laceration",
                "Memories",
                "Outro"
            )
            return Playlist(
                DUMMY_PLAYLIST_ID,
                "Irsen's Tale",
                songTitleList.mapIndexed { index, title ->
                    val mediaId = "media$index"
                    Song(
                        mediaId,
                        title,
                        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/raw/$mediaId"
                    )
                }
            )
        }
    }
}