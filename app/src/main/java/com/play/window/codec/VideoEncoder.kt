package com.play.window.codec

import android.media.MediaCodec
import android.media.MediaFormat
import com.play.window.utils.MediaConfig
/**
 * 在你低比特率的情况下，降低分辨率同行会产生更好的画质
 * 码率越高，画质越好。但是一味的提高码率画质的提升会越来越不明显
 * 且会增加延迟
 */
class VideoEncoder(val config: MediaConfig) : IEncoder {

    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mMediaFormat: MediaFormat

    private val mCallback = object :MediaCodec.Callback(){
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        }
    }

    private fun initConfig(config: MediaConfig) {
        mMediaCodec = MediaCodec.createEncoderByType(config.mimeType)
        mMediaFormat = MediaFormat.createVideoFormat(config.mimeType, config.videoWidth, config.videoHeight)
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,config.videoBitRate)
        /**
         * VBR、CBR、CQ
         */
        mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,config.videoBitRateModel)
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,config.videoFrameRate)
        mMediaFormat.setInteger(MediaFormat.KEY_MAX_FPS_TO_ENCODER,120)
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2)

        /**
         * 控制视频编码的压缩质量和比特率，达到调节画面质量的作用，范围0到51，
         * 值越小画质越高,可以分别对I、P、B帧设置
         *
         * 注意：Android底层不一定支持，且高通865芯片的设置也不一样
         */
        if(config.openQP){
            mMediaFormat.setInteger(MediaFormat.KEY_VIDEO_QP_I_MIN,config.minIQP)
            mMediaFormat.setInteger(MediaFormat.KEY_VIDEO_QP_I_MAX,config.maxIQP)
        }
        mMediaCodec.setCallback(mCallback)
        mMediaCodec.configure(mMediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    override fun startEncoder() {
        mMediaCodec.start()
    }

    override fun stopEncoder() {
        mMediaCodec.stop()
    }

    override fun getInputSurface(): Any? {
        return null
    }

    override fun getInputRect(): Any? {
        return null
    }

    override fun addListener(listener: IEncoder.EncodeListener) {
    }
}