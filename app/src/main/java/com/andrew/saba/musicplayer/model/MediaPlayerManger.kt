package com.andrew.saba.musicplayer.model

import android.media.MediaPlayer
import java.util.Timer
import java.util.TimerTask


class MediaPlayerManager {
    private var playbackCallback: PlaybackCallback? = null
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var currentTrack: AudioTrack? = null
    private var currentPlaylist=ArrayList<AudioTrack>()
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
        mediaPlayer.reset()
        currentTrack=null
    }


    fun nextTrack() {
        // Implement logic to play the next track in the playlist
        val currentTrackIndex=currentPlaylist.indexOf(currentTrack)
        if (currentTrackIndex != -1 && currentTrackIndex < currentPlaylist.size - 1) {
            val nextTrack = currentPlaylist[currentTrackIndex + 1]
            playTrack(nextTrack)
        }
    }

    fun previousTrack() {
        // Implement logic to play the previous track in the playlist
        val currentTrackIndex=currentPlaylist.indexOf(currentTrack)
        if (currentTrackIndex != -1 && currentTrackIndex < currentPlaylist.size - 2) {
            val previousTrack = currentPlaylist[currentTrackIndex - 1]
            playTrack(previousTrack)
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun setPlaybackCallback(callback: PlaybackCallback) {
        playbackCallback = callback
    }

    // Timer to update current position
    private var positionUpdateTimer = Timer()

    // Function to start updating current position
    private fun startPositionUpdate() {
        positionUpdateTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentPosition = mediaPlayer.currentPosition
                playbackCallback?.onPositionChanged(currentPosition)
            }
        }, 0, 100) // Update every 1 second
    }
    fun isMediaPlaying():Boolean{
       return mediaPlayer.isPlaying
    }

    fun setCurrentPlaylist(audioTracks: ArrayList<AudioTrack>) {
        currentPlaylist=audioTracks
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
