package com.example.android.sampleplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModels<MainViewModel> {
        MainViewModel.Factory(MusicServiceConnection(application))
    }

    private lateinit var textSongTitle: TextView
    private lateinit var buttonPlayOrPause: ImageButton
    private lateinit var buttonShuffle: ImageButton
    private lateinit var queueList: RecyclerView

    private val queueListAdapter: Adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textSongTitle = findViewById(R.id.text_song_name)
        buttonPlayOrPause = findViewById(R.id.button_play_or_pause)
        buttonPlayOrPause.setOnClickListener {
            mainViewModel.playOrPause()
        }
        buttonShuffle = findViewById(R.id.button_shuffle)
        buttonShuffle.setOnClickListener {
            mainViewModel.toggleShuffleMode()
        }
        queueList = findViewById(R.id.list_song_queue)
        queueList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = queueListAdapter
        }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                mainViewModel.move(viewHolder.absoluteAdapterPosition, target.absoluteAdapterPosition)
                return true
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                mainViewModel.finishMove()
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // not support
            }

            override fun isLongPressDragEnabled() = true

        }).attachToRecyclerView(queueList)

        mainViewModel.isReady.observe(this) {
            buttonPlayOrPause.isEnabled = it
        }
        mainViewModel.playingMediaTitle.observe(this) {
            textSongTitle.text = it
        }
        mainViewModel.isPlaying.observe(this) {
            buttonPlayOrPause.setImageResource(
                if(it) {
                    R.drawable.ic_baseline_pause_24
                } else {
                    R.drawable.ic_baseline_play_arrow_24
                }
            )
        }
        mainViewModel.shuffleModeEnabled.observe(this) {
            buttonShuffle.setImageResource(
                if(it) {
                    R.drawable.ic_baseline_shuffle_on_24
                } else {
                    R.drawable.ic_baseline_shuffle_24
                }
            )
        }
        mainViewModel.currentQueue.observe(this) {
            queueListAdapter.update(it)
        }

        mainViewModel.initialize()
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        private val songList = mutableListOf<Song>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = songList[position].name
        }

        override fun getItemCount() = songList.size

        fun update(songList: List<Song>) {
            val diffCallback = DiffCallback(this.songList, songList)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            this.songList.apply {
                clear()
                addAll(songList)
            }
            diffResult.dispatchUpdatesTo(this)
        }
    }

    private inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.text_song_name)
    }

    private inner class DiffCallback(
        private val oldSongList: List<Song>,
        private val newSongList: List<Song>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldSongList.size

        override fun getNewListSize() = newSongList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldSongList[oldItemPosition].id == newSongList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldSongList[oldItemPosition] == newSongList[newItemPosition]

    }
}