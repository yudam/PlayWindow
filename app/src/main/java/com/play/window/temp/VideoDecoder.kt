package com.play.window.temp

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface

/**
 * 视频解码
 * 为了防止视频播放过快导致的音画不同步问题，我们需要根据每一帧的pts来计算渲染的时间，通过线程休眠的方式
 * 来达到指定的渲染时间点在继续执行
 */
class VideoDecoder(val filePath: String, val surface: Surface) : Runnable {

    companion object {
        private const val TIMEOUTUS = 1000 * 10L
        private const val TAG = "VideoDecoder"
    }


    private lateinit var mMediaExtractor: MediaExtractor
    private lateinit var mMediaCodec: MediaCodec
    private var mMediaFormat: MediaFormat? = null
    private var isUnDecoder: Boolean = false

    private var mVideoWidth: Int = 0
    private var mVideoHeight: Int = 0


    init {
        initConfig()
    }

    private fun initConfig() {
        mMediaFormat = getMediaFormat() ?: return
        mVideoWidth = mMediaFormat?.getInteger(MediaFormat.KEY_WIDTH) ?: 0
        mVideoHeight = mMediaFormat?.getInteger(MediaFormat.KEY_HEIGHT) ?: 0
        val typeMime = mMediaFormat?.getString(MediaFormat.KEY_MIME) ?: ""
        mMediaCodec = MediaCodec.createDecoderByType(typeMime)

        Log.i(TAG, "mMediaFormat:" + mMediaFormat?.toString())
        mMediaCodec.configure(mMediaFormat, surface, null, 0)
        mMediaCodec.start()
    }


    private fun getMediaFormat(): MediaFormat? {
        mMediaExtractor = MediaExtractor()
        mMediaExtractor.setDataSource(filePath)
        for (index in 0 until mMediaExtractor.trackCount) {
            val format = mMediaExtractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/")) {
                mMediaExtractor.selectTrack(index)
                return format
            }
        }
        return null
    }

    fun getVideoWidth(): Int {
        return mVideoWidth
    }

    fun getVideoHeight(): Int {
        return mVideoHeight
    }

    override fun run() {
        doDecoderWork()
    }


    private var mPreFrameTime = 0L
    private var mPreSysTime = 0L

    private fun doDecoderWork() {

        val mBufferInfo = MediaCodec.BufferInfo()
        while (!isUnDecoder) {
            val inputIndex = mMediaCodec.dequeueInputBuffer(TIMEOUTUS)
            if (inputIndex >= 0) {
                val inputBuffer = mMediaCodec.getInputBuffer(inputIndex) ?: continue
                val ret = mMediaExtractor.readSampleData(inputBuffer, 0)
                if (ret >= 0) {
                    val len = inputBuffer.remaining()
                    val pts = mMediaExtractor.sampleTime
                    mMediaCodec.queueInputBuffer(inputIndex, 0, len, pts, 0)
                    mMediaExtractor.advance()
                }
            }

            val outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUTUS)
            if (outputIndex >= 0) {
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "doDecoderWork: BUFFER_FLAG_END_OF_STREAM")
                } else {

                    /**
                     * 如果将渲染速度加快，画面就有了快进的效果，
                     * 如果线程休眠时间加长，画面就有了慢放的效果
                     */

                    // 根据pts计算帧刷新时间，通过线程休眠来防止渲染太快问题
                    if (mPreFrameTime == 0L) {
                        mPreSysTime = System.nanoTime() / 1000
                        mPreFrameTime = mBufferInfo.presentationTimeUs
                    } else {
                        var framePer = mBufferInfo.presentationTimeUs - mPreFrameTime
                        if (framePer < 0) framePer = 0
                        val desircTime = mPreSysTime + framePer
                        val nowTime = System.nanoTime() / 1000
                        if (desircTime > nowTime) {
                            val sleepTime = desircTime - nowTime
                            Log.i(TAG, "sleepTime: " + sleepTime)
                            Thread.sleep(sleepTime / 1000, (sleepTime % 1000 * 1000).toInt())
                        }

                        mPreSysTime += framePer
                        mPreFrameTime += framePer
                    }

                }
                mMediaCodec.releaseOutputBuffer(outputIndex, true)
            }
        }
        release()
    }


    private fun release() {
        mMediaExtractor.release()
        mMediaCodec.stop()
        mMediaCodec.release()
    }
}