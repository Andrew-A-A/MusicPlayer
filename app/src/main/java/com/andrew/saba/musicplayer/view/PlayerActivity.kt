package com.andrew.saba.musicplayer.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.R.drawable.exo_icon_pause
import androidx.media3.ui.R.drawable.exo_icon_play
import androidx.recyclerview.widget.GridLayoutManager
import com.andrew.saba.musicplayer.R
import com.andrew.saba.musicplayer.adapter.RvAdapter
import com.andrew.saba.musicplayer.database.SearchHistoryDAO
import com.andrew.saba.musicplayer.databinding.ActivityPlayerBinding
import com.andrew.saba.musicplayer.model.AudioTrack
import com.andrew.saba.musicplayer.model.MediaPlayerManager
import com.andrew.saba.musicplayer.service.MusicService
import com.andrew.saba.musicplayer.viewmodel.PlayerViewModel


const val CHANNEL_ID="player"
class PlayerActivity : AppCompatActivity(), RvAdapter.OnItemClickListener,SeekBar.OnSeekBarChangeListener {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var tracksViewAdapter: RvAdapter
    private lateinit var mediaPlayerManager: MediaPlayerManager
    private lateinit var playerViewModel: PlayerViewModel
    val searchHistoryDAO = SearchHistoryDAO(this)
    private var permissionGranted=true
    private var musicService: MusicService? = null
    private var isMusicServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isMusicServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isMusicServiceBound = false
        }
    }

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createAndRegisterNotificationChannel()

        requestPermissions()


        // Initialize the MediaPlayerManager
        mediaPlayerManager = MediaPlayerManager()

        // Initialize the PlayerViewModel
        playerViewModel = PlayerViewModel(mediaPlayerManager)

        //Bind service intent with App activity
        val serviceIntent = Intent(this, MusicService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)


        // Set up search text box
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission, if needed
                filterAudioTracks(query)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
                // Store the search query in the database
                if (!query.isNullOrBlank()) {
                    // Check if the query already exists in the database
                    if (!searchHistoryDAO.isSearchQueryExists(query)) {
                        searchHistoryDAO.insertSearchQuery(query)
                    }
                }
                binding.searchDropdown.visibility = View.GONE
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query text changes
                filterAudioTracks(newText)

                // Retrieve search history and display it in the dropdown
                val searchQueries = searchHistoryDAO.getAllSearchQueries()
                val adapter = ArrayAdapter(baseContext, android.R.layout.simple_dropdown_item_1line, searchQueries)
                binding.searchDropdown.adapter = adapter

                // Show the dropdown when the user starts typing
                if (newText?.isNotEmpty() == true) {
                    binding.searchDropdown.visibility = View.VISIBLE
                } else {
                    binding.searchDropdown.visibility = View.GONE
                }
                return true
            }
        })

        binding.searchDropdown.setOnItemClickListener { parent, _, position, _ ->
            val selectedQuery = parent.adapter.getItem(position) as String
            // Handle the selected item here
            // For example, you can populate the search view with the selected query
            binding.searchView.setQuery(selectedQuery, true)
            binding.searchDropdown.visibility = View.GONE
        }

        // Set up a click listener for the SearchView to show search history
        binding.searchView.setOnClickListener {
            searchHistoryDAO.getAllSearchQueries()
            // Display the search history to the user
            // You can use a dialog, a dropdown, or any UI component to show the history
        }


        // Set up the Play/Pause button click listener
        binding.playButton.setOnClickListener {
            if (mediaPlayerManager.isMediaPlaying()) {
                // If the media is playing, pause it
                playerViewModel.pause()
                  if (isMusicServiceBound) musicService!!.pauseMusic()
            } else if (playerViewModel.playbackState.value == PlayerViewModel.PlaybackState.PAUSED) {
                // If the media is paused, resume playback
                playerViewModel.resume()
                    if (isMusicServiceBound) musicService!!.playMusic()
            }

        }


        //Set up seek bar change listener
        binding.seekBar.setOnSeekBarChangeListener(this)

        // Set up the Stop button click listener
        binding.stopButton.setOnClickListener{
            playerViewModel.stop()
        }

        //Set up next/previous buttons
        binding.nextButton.setOnClickListener{
            mediaPlayerManager.nextTrack()
            binding.seekBar.max=mediaPlayerManager.getCurrentTrackDuration()
        }
        binding.previousButton.setOnClickListener{
            mediaPlayerManager.previousTrack()
            binding.seekBar.max=mediaPlayerManager.getCurrentTrackDuration()
        }

        // Observe the playback state and update UI accordingly
        playerViewModel.playbackState.observe(this) { state ->
            when (state) {
                PlayerViewModel.PlaybackState.PLAYING -> {
                    // Change the Play/Pause button icon to pause when playing
                    binding.playButton.setImageResource(exo_icon_pause)
                }
                else -> {
                    // Change the Play/Pause button icon to play when paused
                    binding.playButton.setImageResource(exo_icon_play)
                }
            }
        }

        // Set the playback callback for the MediaPlayerManager
        mediaPlayerManager.setPlaybackCallback(playerViewModel)

        // Observe the current position of the track and update the SeekBar
        playerViewModel.currentPosition.observe(this) { position ->
            binding.seekBar.progress = position
        }


        //Set play back state observer to change play/pause button icon
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


    }
        private val activityResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                // Handle Permission granted/rejected
                permissions.entries.forEach {
                    if (it.key == Manifest.permission.READ_EXTERNAL_STORAGE && !it.value)
                        permissionGranted = false
                }
                if (!permissionGranted) {
                    Toast.makeText(baseContext,
                        "Permission request denied",
                        Toast.LENGTH_SHORT).show()
                    this.finish()
                } else {
                    // Set up the RecyclerView and load audio tracks
                    setRecyclerAdapter()
                }
            }

    private fun requestPermissions() {
        activityResultLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }


    //Unbound service on activity destroy
    override fun onDestroy() {
        super.onDestroy()
        if (isMusicServiceBound) {
            musicService?.stopMusic()
            // Unbind from the MusicService
           unbindService(serviceConnection)
            isMusicServiceBound = false
        }
    }

    private fun createAndRegisterNotificationChannel() {
        // Create the NotificationChannel.
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_MIN
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
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
        if (isMusicServiceBound) musicService!!.playMusic()
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
    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if (p2) {
            // Set new position based on the progress value
            mediaPlayerManager.seekTo(p1)
        }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    override fun onStopTrackingTouch(p0: SeekBar?) {

    }
}
