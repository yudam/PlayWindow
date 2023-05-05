package com.play.window.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import android.view.Surface
import com.play.window.utils.Utils
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 在创建MediaFormat时不需要配置sps和pps，在编码接收到第一帧也就是INFO_OUTPUT_FORMAT_CHANGED时，
 * 可以获取outputFormat来获取sps和pps，sps和pps一般位于码流的起始位置，也就是编码器的第一帧。
 *
 * sps ：序列参数集，保存了和编码序列相关的参数，如编码的profile、level、图像宽高等
 * pps ：图像参数集，保存了图像的相关参数
 *
 *  H264编码，
 */
class H264Encoder(val inputSurface:Surface? = null) : Thread("H264Encoder-Thread") {

    /**
     * H264压缩格式的写法
     */
    private val mMimeType = "video/avc"

    private lateinit var mMediaCodec: MediaCodec

    private val mediaQueue = LinkedBlockingQueue<ByteArray>()

    private var mediaMuxer: MediaMuxer? = null

    private var mTrackIndex: Int = -1

    private val timeoutUs = 10000L

    private var dataListener:IEncoderDataListener? = null

    @Volatile
    private var isEncoder = true

    override fun run() {
        initConfig()
    }


    /**
     * 对于视频编码来说设置MediaFormat时主要有以下参数：颜色格式、码率、码率控制模式、帧率、I帧间隔
     */
    private fun initConfig(width :Int = 1080,height:Int = 1920) {
        val mediaFormat = MediaFormat.createVideoFormat(mMimeType, width, height)
        // 选择对应的YUV4颜色格式
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,width * height * 5)
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        mMediaCodec = MediaCodec.createEncoderByType(mMimeType)
        mMediaCodec.configure(mediaFormat, inputSurface, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaCodec.start()
        onFrame()
    }


    fun put(packet: ByteArray) {
        mediaQueue.offer(packet)
    }

    fun setListener(listener: IEncoderDataListener?){
        dataListener = listener
    }

    private fun onFrame() {
        val mBufferInfo = MediaCodec.BufferInfo()
        while (isEncoder) {
            val outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, timeoutUs)
            if (outputBufferIndex >= 0) {
                val byteBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex)
                if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM: ")
                } else if ((mBufferInfo.flags and MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) != 0) {
                    Log.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED: ")
                    dataListener?.notifyMediaFormat(mMediaCodec.outputFormat,true)
                    // 获取sps和pps
                    val sps = mMediaCodec.outputFormat.getByteBuffer("csd-0")
                    val pps = mMediaCodec.outputFormat.getByteBuffer("csd-1")
                    Log.i(TAG, "sps: "+ Utils.bytesToHex(sps?.array()))
                    sps?.array()?.forEachIndexed { index, byte ->
                        Log.i(TAG, "index: "+index+"   byte: "+byte)
                    }
                    Log.i(TAG, "pps: "+Utils.bytesToHex(pps?.array()))
                    pps?.array()?.forEachIndexed { index, byte ->
                        Log.i(TAG, "index: "+index+"   byte: "+byte)
                    }

                } else if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // 表示当前缓冲区携带的是编码器的初始化信息，并不是媒体数据
                    Log.i(TAG, "BUFFER_FLAG_CODEC_CONFIG: ")
                } else{
                    // 当前缓冲区是关键帧信息
                    if((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0){
                        Log.i(TAG, "BUFFER_FLAG_KEY_FRAME: ")
                    }
                    val videoArray = ByteArray(mBufferInfo.size)
                    byteBuffer?.let {
                        it.position(mBufferInfo.offset)
                        it.limit(mBufferInfo.offset + mBufferInfo.size)
                        it.get(videoArray,mBufferInfo.offset,mBufferInfo.size)
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                    val copy = MediaCodec.BufferInfo()
                    copy.set(mBufferInfo.offset, mBufferInfo.size,
                        mBufferInfo.presentationTimeUs, mBufferInfo.flags)
                    val pkt = MediaPacket().apply {
                        info = copy
                        data = ByteBuffer.wrap(videoArray)
                        pts = info!!.presentationTimeUs
                        isVideo = true
                    }
                    dataListener?.notifyAvailableData(pkt)
                }
            }
        }

        mMediaCodec.stop()
        mMediaCodec.release()
    }

    companion object {
        private const val TAG = "H264Encoder"
    }
}