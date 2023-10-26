package com.andrew.saba.musicplayer.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.andrew.saba.musicplayer.R
import com.andrew.saba.musicplayer.adapter.RvAdapter
import com.andrew.saba.musicplayer.databinding.ActivityPlayerBinding
import com.andrew.saba.musicplayer.model.AudioTrack
import com.andrew.saba.musicplayer.model.MediaPlayerManager
import com.andrew.saba.musicplayer.viewmodel.PlayerViewModel


class PlayerActivity : AppCompatActivity(), RvAdapter.OnItemClickListener {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var tracksViewAdapter: RvAdapter
    private lateinit var mediaPlayerManager: MediaPlayerManager
    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize the MediaPlayerManager
        mediaPlayerManager = MediaPlayerManager()

        // Initialize the PlayerViewModel
        playerViewModel = PlayerViewModel(mediaPlayerManager)

        // Set up search text box
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission, if needed
                filterAudioTracks(query)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query text changes
                filterAudioTracks(newText)
                return true
            }
        })


        // Set up the Play/Pause button click listener
        binding.playButton.setOnClickListener {
            if (mediaPlayerManager.isMediaPlaying()) {
                // If the media is playing, pause it
                playerViewModel.pause()
            } else {
                // If the media is paused, resume playback
                if ( playerViewModel.playbackState.value==PlayerViewModel.PlaybackState.PAUSED)
                playerViewModel.resume()
            }
        }



        // Set up the Stop button click listener
        binding.stopButton.setOnClickListener{
            playerViewModel.stop()
        }

        //Set up next/previous buttons
        binding.nextButton.setOnClickListener{
            mediaPlayerManager.nextTrack()
        }
        binding.previousButton.setOnClickListener{
            mediaPlayerManager.previousTrack()
        }

        // Observe the playback state and update UI accordingly
        playerViewModel.playbackState.observe(this) { state ->
            when (state) {
                PlayerViewModel.PlaybackState.PLAYING -> {
                    // Change the Play/Pause button icon to pause when playing
                    binding.playButton.setImageResource(androidx.media3.ui.R.drawable.exo_icon_pause)
                }
                else -> {
                    // Change the Play/Pause button icon to play when paused
                    binding.playButton.setImageResource(androidx.media3.ui.R.drawable.exo_icon_play)
                }
            }
        }

        // Set up the RecyclerView and load audio tracks
        setRecyclerAdapter()

        // Set the playback callback for the MediaPlayerManager
        mediaPlayerManager.setPlaybackCallback(playerViewModel)

        // Observe the current position of the track and update the SeekBar
        playerViewModel.currentPosition.observe(this) { position ->
            binding.seekBar.progress = position
        }


        playerViewModel.playbackState.observe(this) { state ->
            when (state) {
                PlayerViewModel.PlaybackState.PLAYING -> {
                    binding.playButton.setImageResource(R.drawable.ic_pause)
                }
                else -> {
                    binding.playButton.setImageResource(R.drawable.ic_play)
                }
            }
        }

        playerViewModel.currentPosition.observe(this) { position ->
            // Update your SeekBar or UI element with the current position
            binding.seekBar.progress = position
        }

    }

    // Function to set up the RecyclerView with audio tracks
    private fun setRecyclerAdapter() {
        val audioTracks = ArrayList<AudioTrack>()

        // Set up the RecyclerView
        binding.recyclerView.layoutManager = GridLayoutManager(this, 1)

        // Fill tracks list
        getTracksList(audioTracks)

        mediaPlayerManager.setCurrentPlaylist(audioTracks)

        // Create and set the RecyclerView adapter
        tracksViewAdapter = RvAdapter(audioTracks)
        tracksViewAdapter.setOnItemClickListener(this)
        binding.recyclerView.adapter = tracksViewAdapter
    }

    //Initialize audio tracks list
    private fun getTracksList(audioTracks: ArrayList<AudioTrack>) {
        // Define the columns you want to retrieve from the MediaStore
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        // Query the MediaStore for audio files
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        if (cursor != null) {
            val titleColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val filePathColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val artistColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumIdColumnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val length = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val trackName = cursor.getString(titleColumnIndex)
                val filePath = cursor.getString(filePathColumnIndex)
                val artistName = cursor.getString(artistColumnIndex)
                val albumId = cursor.getLong(albumIdColumnIndex)
                val duration = cursor.getInt(length)

                // Check for valid data
                if (trackName == null || filePath == null) {
                    continue  // Skip invalid entries
                }

                // Create an audio track object and retrieve album artwork URI
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                val track = AudioTrack(filePath, trackName, artistName, albumId, duration)
                audioTracks.add(track)
            }
            cursor.close()

            // Now you have a list of AudioTrack objects containing the required information.
        }
    }

    // Handle item click in the RecyclerView
    override fun onItemClicked(audioTrack: AudioTrack) {
        // Play the selected track, update the UI, and set the SeekBar max value
        playerViewModel.play(audioTrack)
        binding.seekBar.max = audioTrack.duration
    }

    // Function to filter audio tracks based on the search query
    private fun filterAudioTracks(query: String?) {
        val tracks=ArrayList<AudioTrack>()
        getTracksList(tracks)
        val filteredTracks:ArrayList<AudioTrack> = if (query.isNullOrBlank()) {
            // If the query is blank, show all tracks
            tracks
        } else {
            // Filter tracks based on the query
            tracks.filter { track ->
                track.name.contains(query, ignoreCase = true) ||
                        track.artist.contains(query, ignoreCase = true)
            }.toCollection(ArrayList())
        }

        // Update the RecyclerView with the filtered tracks
      tracksViewAdapter.updateData(filteredTracks)
    }


}
