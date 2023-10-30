package com.andrew.saba.musicplayer.model

import android.media.MediaPlayer
import java.util.Timer
import java.util.TimerTask
import com.andrew.saba.musicplayer.service.MusicService


class MediaPlayerManager {
    private var playbackCallback: PlaybackCallback? = null
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var currentTrack: AudioTrack? = null
    private var currentPlaylist=ArrayList<AudioTrack>()
    init {
        mediaPlayer.setOnCompletionListener {
            // Handle track completion event here, e.g., play the next track
            // Implement your logic to manage playlists
            nextTrack()
        }
    }

    fun playTrack(audioTrack: AudioTrack) {
        currentTrack = audioTrack
        mediaPlayer.apply {
            reset()
            setDataSource(audioTrack.path)
            prepare()
            start()
        }
        MusicService.title=audioTrack.name
        MusicService.artist=audioTrack.artist
        MusicService.artwork= audioTrack.image
        MusicService.mediaPlayer=mediaPlayer
        startPositionUpdate()
    }

    fun resume() {
        if (currentTrack != null && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
            MusicService.mediaPlayer=mediaPlayer
        }
        startPositionUpdate()
    }


    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            MusicService.mediaPlayer=mediaPlayer
        }
    }

    fun stop() {
        mediaPlayer.reset()
        currentTrack=null
        MusicService.mediaPlayer=mediaPlayer
    }


    fun nextTrack() {
        // Implement logic to play the next track in the playlist
        val currentTrackIndex=currentPlaylist.indexOf(currentTrack)
        if (currentTrackIndex != -1 && currentTrackIndex < currentPlaylist.size - 1) {
            val nextTrack = currentPlaylist[currentTrackIndex + 1]
            mediaPlayer.reset()
            playTrack(nextTrack)
        }
    }

    fun previousTrack() {
        // Implement logic to play the previous track in the playlist
        val currentTrackIndex=currentPlaylist.indexOf(currentTrack)
        if (currentTrackIndex > 0 && currentTrackIndex <= currentPlaylist.size - 1) {
            val previousTrack = currentPlaylist[currentTrackIndex - 1]
            mediaPlayer.reset()
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

    fun getCurrentTrackDuration(): Int {
        return currentTrack?.duration ?: 0
    }

    fun setCurrentPlaylist(audioTracks: ArrayList<AudioTrack>) {
        currentPlaylist=audioTracks
    }

    interface PlaybackCallback {
        fun onPositionChanged(position: Int)
    }

}
