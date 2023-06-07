package com.play.window.utils

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

/**
 * 音频混合器
 *
 * MediaMuxer当前只支持AAC压缩格式的音频
 *
 * MediaMuxer生成AAC音频文件时，不需要添加AAC头信息，直接写入即可。
 * MediaCodec.BufferInfo中包含了每一帧数据的偏移、大小和时间戳（微秒 = ms * 1000）等信息。
 *
 * 当编码器没输出一次数据，即可认为输出一帧AAC数据，一帧AAC数据包括1024个采样点
 */
class MuxerUtil(val videoPath: String) {

    private var mMuxer: MediaMuxer = MediaMuxer(videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    @Volatile
    private var isStart: Boolean = false

    @Volatile
    private var isStop: Boolean = false
    private var mAudioTrackIndex: Int = -1
    private var mVideoTrackIndex: Int = -1


    fun addTrack(mediaFormat: MediaFormat, isAudio: Boolean = false) {
        Log.i("MuxerUtil", "isAudio: " + isAudio)
        if (isAudio) {
            mAudioTrackIndex = mMuxer.addTrack(mediaFormat)
        } else {
            mVideoTrackIndex = mMuxer.addTrack(mediaFormat)
        }


        mMuxer.start()
//        if (mAudioTrackIndex != -1 && mVideoTrackIndex != -1) {
//            mMuxer.start()
//            isStart = true
//        }
    }


    fun writeSampleData(dataBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?, isAudio: Boolean = false) {
        //  if (!isStart) return
        if (isStop || dataBuffer == null || bufferInfo == null) return
        if (isAudio) {
            Log.i("MuxerUtil", "writeSampleData: audio")
            mMuxer.writeSampleData(mAudioTrackIndex, dataBuffer, bufferInfo)
        } else {
            Log.i("MuxerUtil", "writeSampleData: video ")
            mMuxer.writeSampleData(mVideoTrackIndex, dataBuffer, bufferInfo)
        }
    }


    @Synchronized
    fun release() {
        Log.i("MuxerUtil", "release: ")
        //if (isStop) return
        isStop = true
        mMuxer.stop()
        mMuxer.release()
    }
}