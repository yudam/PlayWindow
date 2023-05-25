package com.play.window.capture

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.android.exoplayer2.audio.AudioGet
import com.play.window.WindowApp
import java.nio.ByteBuffer

/**
 * User: maodayu
 * Date: 2023/4/24
 * Time: 10:34
 */
class AudioProcess : HandlerThread("AudioProcess") {

    /**
     * 采样率
     */
    private val sampleRate = 48000

    /**
     * 通道数
     */
    private val channelConfig = 2

    /**
     * 采样深度
     */
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioTrack: AudioTrack? = null

    private var audioHandler: Handler? = null

    override fun onLooperPrepared() {
        super.onLooperPrepared()
        audioHandler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val audioData = msg.obj as ByteBuffer
                write(audioData)
            }
        }
    }

    init {
        val mMinBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        Log.i(WindowApp.TAG, "mMinBufferSize:$mMinBufferSize")
        audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
            getChannelConfig(), audioFormat, mMinBufferSize, AudioTrack.MODE_STREAM
        )
        audioTrack?.play()
        AudioGet.registerListener { buffer, size ->
            audioHandler?.let {
                it.sendMessage(it.obtainMessage(0x11, buffer))
            }
        }

        start()
    }

    private fun getChannelConfig(): Int {
        return if (channelConfig == 1) {
            AudioFormat.CHANNEL_OUT_MONO //单声道
        } else {
            AudioFormat.CHANNEL_OUT_STEREO //双声道
        }
    }


    /**
     *  写入的字节大小默认为一个音频帧的大小，也就是4096
     *  建议为音频帧大小的倍数
     */
    private fun write(audioData: ByteBuffer) {
        val buffSize = if (audioData.remaining() > 4096) {
            audioData.remaining()
        } else {
            4096
        }
        audioData.clear()
        Log.i(WindowApp.TAG, "write: " + audioData.remaining())
       val ret =  audioTrack?.write(audioData, buffSize, AudioTrack.WRITE_BLOCKING)
        Log.i(WindowApp.TAG, "ret: $ret")
    }

    companion object {
        private const val TAG = "AudioProcess"
    }
}

