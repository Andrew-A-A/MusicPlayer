package com.andrew.saba.musicplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.andrew.saba.musicplayer.model.MediaPlayerManager

class PlayerViewModel : ViewModel(),MediaPlayerManager.PlaybackCallback {
    // LiveData to observe the current playback state
    private val _playbackState = MutableLiveData<PlaybackState>()
    val playbackState: LiveData<PlaybackState>
        get() = _playbackState

    // LiveData to observe the current track position
    private val _currentPosition = MutableLiveData<Int>()
    val currentPosition: LiveData<Int>
        get() = _currentPosition
    init {
        // Initialize the ViewModel with an initial playback state
        _playbackState.value = PlaybackState.STOPPED
    }

    fun updateCurrentPosition(position: Int) {
        _currentPosition.postValue(position)
    }

    // Function to start or resume playback
    fun play() {
        // Logic to start or resume playback
        _playbackState.value = PlaybackState.PLAYING
    }

    // Function to pause playback
    fun pause() {
        // Logic to pause playback
        _playbackState.value = PlaybackState.PAUSED
    }

    // Function to stop playback
    fun stop() {
        // Logic to stop playback
        _playbackState.value = PlaybackState.STOPPED
    }

    // Enum to represent playback states
    enum class PlaybackState {
        PLAYING,
        PAUSED,
        STOPPED
    }

    override fun onPositionChanged(position: Int) {
        _currentPosition.postValue(position)
    }
}
