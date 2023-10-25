package com.andrew.saba.musicplayer.model

import android.media.MediaPlayer
import java.util.Timer
import java.util.TimerTask


class MediaPlayerManager {
    private var playbackCallback: PlaybackCallback? = null
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var currentTrack: AudioTrack? = null

    init {
        mediaPlayer.setOnCompletionListener {
            // Handle track completion event here, e.g., play the next track
            // Implement your logic to manage playlists
        }
    }

    fun initialize() {
        // Initialize the MediaPlayer

    }

    fun playTrack(audioTrack: AudioTrack) {
        currentTrack = audioTrack
        mediaPlayer.apply {
            reset()
            setDataSource(audioTrack.path)
            prepare()
            start()
        }
        startPositionUpdate()
    }

    fun resume() {
        if (currentTrack != null && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
        startPositionUpdate()
    }


    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            currentTrack=null
        }
    }


    fun nextTrack() {
        // Implement logic to play the next track in the playlist

    }

    fun previousTrack() {
        // Implement logic to play the previous track in the playlist
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun setPlaybackCallback(callback: PlaybackCallback) {
        playbackCallback = callback
    }

    // Timer to update current position
    private val positionUpdateTimer = Timer()

    // Function to start updating current position
    private fun startPositionUpdate() {
        positionUpdateTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentPosition = mediaPlayer.currentPosition
                playbackCallback?.onPositionChanged(currentPosition)
            }
        }, 0, 1000) // Update every 1 second
    }
    fun isMediaPlaying():Boolean{
       return mediaPlayer.isPlaying
    }

    enum class MediaPlayerState {
        PLAYING,
        PAUSED,
        STOPPED
    }
    interface PlaybackCallback {
        fun onPositionChanged(position: Int)
    }

}
