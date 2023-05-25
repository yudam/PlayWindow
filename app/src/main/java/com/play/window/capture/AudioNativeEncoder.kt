package com.play.window.capture

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.play.window.BaseNative
import java.nio.ByteBuffer

/**
 * User: maodayu
 * Date: 2023/5/12
 * Time: 11:16
 */
class AudioNativeEncoder() : Thread("AudioNativeEncoder"), AudioEncoderListener {

    private var isRecord = true
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private var bufferSize: Int = 0

    override fun startAudioRecord(path: String) {
        BaseNative.startAudioRecord(path)
        start()
    }

    override fun stopAudioRecord() {
        isRecord = false
        BaseNative.stopAudioRecord()
        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord?.stop()
        }
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
        }
    }


    override fun run() {
        initAudioRecord()
        executeWriteAudio()
    }

    @SuppressLint("MissingPermission")
    private fun initAudioRecord() {
        bufferSize = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.DEFAULT,
            sampleRate, AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize)
        audioRecord?.startRecording()
    }

    /**
     *  allocateDirect分配一个直接字节缓冲区，用于频繁的操作字节
     *
     *  这里缓冲区的大小推荐是帧大小的倍数，太小会导致播放无声音
     */
    private fun executeWriteAudio() {
        while (isRecord) {
            // 这里设置为4096和字节
            val buf = ByteBuffer.allocateDirect(1024 * 4)
            val len = audioRecord?.read(buf, buf.capacity()) ?: 0
            Log.i("MDY", "buf: "+buf.capacity()+"    len:"+len)
            if (len > 0 && len <= buf.capacity()) {
                onFrameData(buf.array(),buf.capacity())
            }
        }
    }

    private fun onFrameData(audioData: ByteArray,len:Int) {
        BaseNative.setFrameData(audioData,len)
    }
}