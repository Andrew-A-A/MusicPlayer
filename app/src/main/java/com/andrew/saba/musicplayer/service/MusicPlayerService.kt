package com.andrew.saba.musicplayer.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import com.andrew.saba.musicplayer.R
import com.andrew.saba.musicplayer.view.CHANNEL_ID
import androidx.media.app.NotificationCompat as MediaNotificationCompat


class MusicService : Service() {
    companion object {
        var artwork: Long =0
        var mediaPlayer: MediaPlayer? = null
         var title=""
         var artist=""
    }
    private val binder = MusicBinder()
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaSession: MediaSessionCompat

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val audioAttributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> pauseMusic()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer?.pause()
                        }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> playMusic()
                }
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer?.setAudioAttributes(audioAttributes)
        notificationManager = NotificationManagerCompat.from(this)

        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .build()
        )
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                playMusic()
            }

            override fun onPause() {
                super.onPause()
                pauseMusic()
            }

            override fun onStop() {
                super.onStop()
                stopMusic()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                // Implement skipping to the next track
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                // Implement skipping to the previous track
            }
        })

        mediaSession.isActive = true
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun playMusic() {
        if (requestAudioFocus()) {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
            }
            showNotification()
        }
    }

    fun pauseMusic() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer?.pause()
        }
        showNotification()
    }

    fun stopMusic() {
        abandonAudioFocus()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
    }

    private fun requestAudioFocus(): Boolean {
        val result =
            audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }



    private fun showNotification() {
        val playPauseAction = PendingIntent.getService(this, 0, Intent("PlayPauseAction"), PendingIntent.FLAG_IMMUTABLE)
        val stopAction = PendingIntent.getService(this, 0, Intent("StopAction"), PendingIntent.FLAG_IMMUTABLE)
        val nextAction = PendingIntent.getService(this, 0, Intent("NextAction"), PendingIntent.FLAG_IMMUTABLE)
        val previousAction = PendingIntent.getService(this, 0, Intent("PreviousAction"), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setLargeIcon(AppCompatResources.getDrawable(this,R.drawable.default_track_ic)?.toBitmap())
            .setContentText(artist)
            .addAction(NotificationCompat.Action(R.drawable.ic_prev, "Previous", previousAction))
            .addAction(
                NotificationCompat.Action(R.drawable.ic_play, "Play/Pause", playPauseAction)
            )
            .addAction(NotificationCompat.Action(R.drawable.ic_next, "Next", nextAction))
            .addAction(NotificationCompat.Action(R.drawable.baseline_stop_24, "Stop", stopAction))
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2, 3)
                    .setMediaSession(mediaSession.sessionToken)  // Set the media session here
            )
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_MIN
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        startForeground(1, notification)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PlayPauseAction" -> {
                if (mediaPlayer?.isPlaying == true) {
                    pauseMusic()
                } else {
                    playMusic()
                }
            }
            "StopAction" -> {
                stopMusic()
            }
            "NextAction" -> {
                // Handle the action to play the next track
            }
            "PreviousAction" -> {
                // Handle the action to play the previous track
            }
        }
        return START_STICKY
    }
}
