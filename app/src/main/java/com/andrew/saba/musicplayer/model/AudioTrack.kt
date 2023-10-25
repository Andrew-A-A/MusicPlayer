package com.andrew.saba.musicplayer.model

data class AudioTrack(
    val path: String,
    val name: String,
    val artist: String,
    val image: Long,
    val duration: Int
)