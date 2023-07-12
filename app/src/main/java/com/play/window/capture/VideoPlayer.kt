package com.play.window.capture

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.Window
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.video.VideoSize
import com.play.window.WindowApp
import org.greenrobot.eventbus.EventBus


/**
 * User: maodayu
 * Date: 2023/4/12
 * Time: 16:22
 */
class VideoPlayer(uri: String, surface: Surface) {

    private var exoPlayer: ExoPlayer? = null

    private var callback: VideoDataCallback? = null

    init {
        exoPlayer = initPlayer(WindowApp.instance(), uri, surface)

    }

    private fun initPlayer(context: Context, uri: String, surface: Surface): ExoPlayer {

        // 1.创建ExoPlayer实例
        val exoPlayer = ExoPlayer.Builder(context)
            .build()
        exoPlayer.trackSelectionParameters = TrackSelectionParameters.Builder(context)
            .build()
        // 2.设置Player状态为STATE_READY时开始播放
        exoPlayer.playWhenReady = true
        // exoPlayer.seekTo(-1,0)
        // 3.设置媒体信息
        exoPlayer.setMediaItem(MediaItem.Builder()
            .setUri(uri)
            .build(), false)

        Log.i(WindowApp.TAG, "isValid: " + surface.isValid)
        //4.设置预览
        exoPlayer.setVideoSurface(surface)

        exoPlayer.setVideoFrameMetadataListener { presentationTimeUs, releaseTimeNs, format, mediaFormat ->


        }
        // 5.监听播放过程中的变化
        exoPlayer.addListener(object : Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                Log.i("onVideoSizeChanged", "width: " + videoSize.width + "   height:" + videoSize.height)
                callback?.onVideoDataChange(videoSize.width, videoSize.height)
            }
        })

        // 6.开始加载媒体资源
        exoPlayer.prepare()
        return exoPlayer
    }


    fun addPrepareVideoCallback(callback: VideoDataCallback){
        this.callback = callback
    }


    private fun getPlayerStateString(): String? {
        val playbackStateString = when (exoPlayer?.playbackState) {
            Player.STATE_BUFFERING -> "buffering"
            Player.STATE_ENDED -> "ended"
            Player.STATE_IDLE -> "idle"
            Player.STATE_READY -> "ready"
            else -> "unknown"
        }
        return java.lang.String.format(
            "playWhenReady:%s playbackState:%s item:%s",
            exoPlayer?.playWhenReady, playbackStateString, exoPlayer?.currentMediaItemIndex)
    }


    fun interface VideoDataCallback {

        fun onVideoDataChange(width: Int, height: Int)
    }
}