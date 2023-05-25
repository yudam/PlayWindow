package com.play.window.utils

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import com.play.window.codec.MediaPacket
import java.nio.ByteBuffer

/**
 * AudioRecord录音
 * 注意权限问题
 */
@SuppressLint("MissingPermission")
class RecordUtil( val audioPath: String? = null) : Thread("Audio-Record-1") {

    private val TAG = "RecordUtil"
    private var isRecord = false
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private var bufferSize: Int = 0
    private var isFirstFrame = true


    /**
     * 回音消除
     * audioRecord可以获取一个SessionId，在创建AudioTrack的时候可以传入
     */
    private fun createAEC() {
        val audioSessionId = audioRecord?.audioSessionId
        if (AcousticEchoCanceler.isAvailable() && audioSessionId != null) {
            val acousticEchoCanceler = AcousticEchoCanceler.create(audioSessionId)
            acousticEchoCanceler.enabled = true
        }
    }


    override fun run() {
        Log.i(TAG, "开始录音    startRecording: ")
        initAudioRecord()
        isRecord = true
        audioRecord?.startRecording()
        executeWriteAudio()
    }

    private fun initAudioRecord() {


        bufferSize = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT,
            sampleRate, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize)
    }

    /**
     *  allocateDirect分配一个直接字节缓冲区，用于频繁的操作字节
     *
     *  这里缓冲区的大小推荐是帧大小的倍数，太小会导致播放无声音
     *
     *  ffmpeg的frame_size默认是1024，一次采样至少1024个字节，两个声道就是4096个字节
     *  所以这里要设置为4096，否则编码的时候声音可能不对
     */
    private fun executeWriteAudio() {
        while (isRecord) {
            // 这里设置为4096和字节
            val buf = ByteBuffer.allocateDirect(1024 * 4)
            val len = audioRecord?.read(buf, buf.capacity()) ?: 0
            Log.i(TAG, "录音数据写入:  $len")
            if (len > 0 && len <= buf.capacity()) {
                //重置ByteBuffer的position和limit
                buf.position(len)
                buf.flip()

                val mediaData = MediaPacket().apply {
                    data = buf
                }
            }
        }
    }


    fun stopRecording() {
        Log.i(TAG, "结束录音    stopRecording")
        isRecord = false
        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord?.stop()
        }
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
        }
    }
}