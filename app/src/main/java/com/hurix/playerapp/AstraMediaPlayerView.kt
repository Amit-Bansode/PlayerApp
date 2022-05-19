package com.hurix.playerapp

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.BaseDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource


class AstraMediaPlayerView : StyledPlayerView, Player.Listener {


    private lateinit var defaultSourceFactory: DefaultDataSource.Factory
    private lateinit var dataSourceFactory: DataSource.Factory
    private lateinit var trackSelector: DefaultTrackSelector
    val url = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"
    val key = ""
    private var mContext: Context = context
    var isPlaying: Boolean = true
    private lateinit var player: ExoPlayer

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )


    fun preparePlayer() {

        minimumWidth = 500
        minimumHeight = 500
        try {
            defaultSourceFactory = DefaultDataSource.Factory(mContext)
            dataSourceFactory =
                EncryptedFileDataSource.Factory(key.toByteArray(), "AES/CBC/NoPadding")
            trackSelector = DefaultTrackSelector(mContext)
            player = ExoPlayer.Builder(mContext).setTrackSelector(trackSelector!!).build()

            player.playWhenReady = true
            controllerAutoShow = true
            controllerShowTimeoutMs = 0
            controllerHideOnTouch = false

            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Log.e("EXO onPlayerError", "onPlayerError : $error")
                }

                override fun onPlayerErrorChanged(error: PlaybackException?) {
                    super.onPlayerErrorChanged(error)
                    Log.e("EXO ErrorChanged", "onPlayerErrorChanged : $error")
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)
                    Log.e("EXO ErrorChanged", "onPlayWhenReadyChanged :reason$reason")
                }
            })

            useController = true

            setPlayer(player)

            isPlaying = true
        } catch (e: Exception) {
            Log.e("EXO", "Error : $e")
        }
    }

    fun play() {
        /* val uri: Uri = Uri.parse(url)
         val mediaItem: MediaItem = MediaItem.fromUri(uri)
         Log.e("Mark up uri", uri.toString())
         Log.e("Mark up url", url)

         player.setMediaItem(mediaItem)*/

        val hlsMediaSource = HlsMediaSource.Factory(defaultSourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(MediaItem.fromUri(url))

        player.setMediaSource(hlsMediaSource);
        player.prepare()
        player.play()
    }

    fun stopPlayer() {
        if (player != null && player?.isPlaying!!) {
            player!!.stop()
        }
    }


}
