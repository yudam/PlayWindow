package com.play.window.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.play.window.model.GLRect
import com.play.window.utils.MediaConfig
import com.play.window.utils.Utils
import java.nio.ByteBuffer

/**
 * 在创建MediaFormat时不需要配置sps和pps，在编码接收到第一帧也就是INFO_OUTPUT_FORMAT_CHANGED时，
 * 可以获取outputFormat来获取sps和pps，sps和pps一般位于码流的起始位置，也就是编码器的第一帧。
 *
 * sps ：序列参数集，保存了和编码序列相关的参数，如编码的profile、level、图像宽高等
 * pps ：图像参数集，保存了图像的相关参数
 *
 * 如果通过MediaCodec创建InputSurface来当作输入，必须在MediaCodec调用config之后才可以创建，
 * 且MediaFormat中color format必须设置为COLOR_FormatSurface
 *
 * MediaCodec默认采用的是AnnexB格式的码流，又连续的NALU单元组成，通过startcode来分隔NALU,
 * MediaCodec从输出队列中读取的ByteBuffer以00000001开始。
 *
 * H264编码，
 */
class H264Encoder(val config: MediaConfig) : Thread("H264Encoder-Thread") {

    /**
     * H264压缩格式的写法
     */
    private val mMimeType = "video/avc"

    private lateinit var mMediaCodec: MediaCodec

    private val timeoutUs = 10000L

    private var dataListener: IEncoderDataListener? = null

    private var surface: Surface? = null

    @Volatile
    private var isEncoder = true

    private val lock = Object()

    private var lastV1Time = 0L
    private var lastV2Time = 0L

    init {
        start()
    }

    override fun run() {
        initConfig()
    }


    /**
     * 对于视频编码来说设置MediaFormat时主要有以下参数：颜色格式、码率、码率控制模式、帧率、I帧间隔
     */
    private fun initConfig() {
        val mediaFormat = MediaFormat.createVideoFormat(mMimeType, config.videoWidth, config.videoHeight)
        // 选择对应的YUV4颜色格式
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitRate)
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, config.videoBitRateModel)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, config.videoFrameRate)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.i_frame_interval)

        synchronized(lock) {
            mMediaCodec = MediaCodec.createEncoderByType(mMimeType)
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            // createInputSurface函数必须在configure之后和start方法之前调用
            surface = mMediaCodec.createInputSurface()
            mMediaCodec.start()
            lock.notifyAll()
        }
        onFrame()
    }

    /**
     * 这里的Surface做为MediaCodec的输入源，我们需要在调用WindowSurface的swapBuffers之前通过
     * setPresentationTime函数来设置inputSurface的时间戳
     */
    fun getEncoderSurface(): Surface {
        if (surface == null) {
            synchronized(lock) {
                if (surface == null) {
                    lock.wait()
                }
            }
        }
        return surface!!
    }

    fun getRect(): GLRect {
        val width = config.videoWidth.toFloat()
        val height = config.videoHeight.toFloat()
        val cx = width / 2
        val cy = height / 2
        return GLRect(cx, cy, width, height, width, height)
    }


    fun setListener(listener: IEncoderDataListener?) {
        dataListener = listener
    }

    fun stopEncoder() {
        isEncoder = false
    }

    private fun onFrame() {
        val mBufferInfo = MediaCodec.BufferInfo()
        while (isEncoder) {
            val outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, timeoutUs)
            if (outputBufferIndex >= 0) {
                val byteBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex)

                //Log.i(TAG, "onFrame: "+byteBuffer?.position()+"    limit : "+byteBuffer?.limit())

                if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM: ")
                } else if ((mBufferInfo.flags and MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) != 0) {
                    Log.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED: ")
                    dataListener?.notifyMediaFormat(mMediaCodec.outputFormat, true)
                    // 获取sps和pps
                    val sps = mMediaCodec.outputFormat.getByteBuffer("csd-0")
                    val pps = mMediaCodec.outputFormat.getByteBuffer("csd-1")
                    Log.i(TAG, "sps-----------: " + Utils.bytesToHex(sps?.array()))
                    sps?.array()?.forEachIndexed { index, byte ->
                        Log.i(TAG, "index: " + index + "   byte: " + byte)
                    }
                    Log.i(TAG, "pps-----------: " + Utils.bytesToHex(pps?.array()))
                    pps?.array()?.forEachIndexed { index, byte ->
                        Log.i(TAG, "index: " + index + "   byte: " + byte)
                    }


                    val csd_0 = ByteBuffer.allocateDirect(sps?.capacity()?:1)
                    csd_0.put(sps)
                    csd_0.rewind()

                    val csd_1 = ByteBuffer.allocateDirect(pps?.capacity()?:1)
                    csd_1.put(pps)
                    csd_1.rewind()


                    val packet = MediaPacket().apply {
                        isCsd = true
                        isVideo = true
                        csd0 = csd_0
                        csd1 = csd_1
                        pts = System.currentTimeMillis() * 1000
                        csd0Size = csd_0.remaining()
                        csd1Size = csd_1.remaining()
                        data = ByteBuffer.allocateDirect(1)
                    }

                    dataListener?.notifyHeaderData(packet)

                } else {
                    // 当前缓冲区是关键帧信息
                    if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        Log.i(TAG, "BUFFER_FLAG_KEY_FRAME: ")
                    }
                    val videoArray = ByteArray(mBufferInfo.size)
                    byteBuffer?.let {
                        it.position(mBufferInfo.offset)
                        it.limit(mBufferInfo.offset + mBufferInfo.size)
                        it.get(videoArray, mBufferInfo.offset, mBufferInfo.size)
                    }

                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false)

                    val videoPTS = getPts()

                    Log.i(TAG, "pts: "+mBufferInfo.presentationTimeUs+"   time: "+videoPTS)
                    val buffer = ByteBuffer.allocateDirect(videoArray.size)
                    buffer.put(videoArray)
                    buffer.clear()
                    val copy = MediaCodec.BufferInfo()
                    copy.set(mBufferInfo.offset, mBufferInfo.size,
                        videoPTS, mBufferInfo.flags)
                    val pkt = MediaPacket().apply {
                        info = copy
                        data = buffer
                        pts = videoPTS
                        isVideo = true
                        bufferSize = data?.remaining() ?: 0

                        csd0 = ByteBuffer.allocateDirect(1)
                        csd1 = ByteBuffer.allocateDirect(1)
                    }
                    dataListener?.notifyAvailableData(pkt)
                }
            }
        }
        mMediaCodec.stop()
        mMediaCodec.release()
        dataListener?.notifyEnd()
    }

    private var lastPresentationTimeUs = 0L

    //private var expectNextTimeUs = 0L


    private val duration = 1000 / 30f

    /**
     * 这里视频的pts如果是ms的话后续合成MP4时会变成慢放的形式
     *
     * 因为这里是合成MP4文件，所以pts值按照帧数的间隔来递增就可以保证视频的流畅
     *
     * 如果是实时播放的话，就需要计算时间之间的误差，来减少卡顿
     */
    private fun getPts(): Long {
        if (lastPresentationTimeUs == 0L) {
            lastPresentationTimeUs = System.currentTimeMillis()
        } else {
            lastPresentationTimeUs += duration.toInt()
        }
        //expectNextTimeUs = lastPresentationTimeUs + duration.toInt()
        return lastPresentationTimeUs * 1000
    }

    companion object {
        private const val TAG = "H264Encoder"
    }
}