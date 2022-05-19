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
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource


class AstraMediaPlayerView : StyledPlayerView, Player.Listener {


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
            val defaultSourceFactory: DataSource.Factory =
                DefaultDataSource.Factory(mContext)

            val dataSourceFactory: DataSource.Factory =
                EncryptedFileDataSource.Factory(key.toByteArray(), "AES/CBC/NoPadding")
            trackSelector = DefaultTrackSelector(mContext)

            val hlsMediaSource = HlsMediaSource.Factory(defaultSourceFactory)
                .setAllowChunklessPreparation(false)
                .createMediaSource(MediaItem.fromUri(url))
            player = ExoPlayer.Builder(mContext).setTrackSelector(trackSelector!!).build()

            player.playWhenReady = true



            controllerAutoShow = true

            controllerShowTimeoutMs = 0

            controllerHideOnTouch = false

//      val hlsuri = "https://astracore-dev-static.s3.amazonaws.com/879c11b4-8e70-423b-9e8c-2d0a3d52c8df/843af611-3c57-43cf-81d9-32f328619775/playlist.m3u8"
//      val hlsuri = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8"




            player.setMediaSource(hlsMediaSource);
            player.prepare()
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Log.e("EXO onPlayerError", "Error : $error")

                }

                override fun onPlayerErrorChanged(error: PlaybackException?) {
                    super.onPlayerErrorChanged(error)
                    Log.e("EXO ErrorChanged", "Error : $error")
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    super.onPlayWhenReadyChanged(playWhenReady, reason)

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
        player.play()
    }

    fun stopPlayer() {
        if (player != null && player?.isPlaying!!) {
            player!!.stop()
        }
    }


}
