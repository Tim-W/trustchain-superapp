package com.example.musicdao

import android.R
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.music_app_main.*
import kotlinx.android.synthetic.main.music_app_main.view.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

lateinit var instance: AudioPlayer

/**
 * Implements an Android MediaPlayer. Is a singleton.
 */
class AudioPlayer(context: Context, private val musicService: MusicService) : LinearLayout(context),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener, SeekBar.OnSeekBarChangeListener {
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var interestedFraction: Float = 0F
    private val bufferInfo = musicService.bufferInfo
    private val progressBar = musicService.progressBar
    private val seekBar = musicService.seekBar
    private val playButton = musicService.playButtonAudioPlayer

    init {
        progressBar.max = 100
        progressBar.progress = 0
        bufferInfo.text = "No track currently playing"
        seekBar.setOnSeekBarChangeListener(this)

        // Handle playing and pausing tracks
        this.playButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                this.playButton.setImageResource(R.drawable.ic_media_play)
                mediaPlayer.pause()
            } else {
                this.playButton.setImageResource(R.drawable.ic_media_pause)
                mediaPlayer.start()
            }
        }

        followSeekBarWithTrack()
    }

    /**
     * This function updates the seek bar location every second with the playing position
     */
    private fun followSeekBarWithTrack() {
        val mHandler = Handler()
        musicService.runOnUiThread(object : Runnable {
            override fun run() {
                val mCurrentPosition: Int = mediaPlayer.currentPosition / 1000
                seekBar.progress = mCurrentPosition
                mHandler.postDelayed(this, 1000)
            }
        })
    }

    companion object {
        fun getInstance(context: Context, musicService: MusicService): AudioPlayer {
            if (!::instance.isInitialized) {
                createInstance(
                    context,
                    musicService
                )
            }
            return instance
        }

        @Synchronized
        private fun createInstance(context: Context, musicService: MusicService) {
            instance =
                AudioPlayer(context, musicService)
        }
    }

    /**
     * Reset internal state to prepare for playing a track
     */
    fun prepareNextTrack() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        seekBar.progress = 0
        playButton.setImageResource(R.drawable.ic_media_play)
        playButton.isClickable = false
        playButton.isActivated = false
        playButton.isEnabled = false
    }

    fun setAudioResource(url: String) {
        prepareNextTrack()
        mediaPlayer.reset()
        mediaPlayer.apply {
            setOnPreparedListener(this@AudioPlayer)
            setOnErrorListener(this@AudioPlayer)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(url)
            prepareAsync()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        var message = ""
        when (what) {
            MediaPlayer.MEDIA_ERROR_IO -> {
                message = "Media error: IO"
            }
            MediaPlayer.MEDIA_ERROR_MALFORMED -> {
                message = "Media error: malformed"
            }
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
                message = "Media error: invalid for progressive playback"
            }
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                message = "Media error: server died"
            }
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                message = "Media error: timed out"
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                message = "Media error: unknown"
            }
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                message = "Media error: unsupported"
            }
        }
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.show()
        return true
    }

    override fun onPrepared(mp: MediaPlayer) {
        this.playButton.isClickable = true
        this.playButton.isActivated = true
        this.playButton.isEnabled = true
        // Directly play the track when it is prepared
        this.playButton.callOnClick()
    }

    /**
     * This enables seeking through the track
     */
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!fromUser) return
        interestedFraction = (progress.toFloat() / 100.toFloat())
        val duration = mediaPlayer.duration
        val seekMs: Int = (duration * interestedFraction).toInt()
        mediaPlayer.seekTo(seekMs)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
}
