package com.play.window.codec

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.play.window.utils.AacUtils
import com.play.window.utils.Utils
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * AAC编码
 * AudioRecord录制出来的是PCM音频格式，占用空间较大，不适合网络传输，而AAC是比较合适的压缩格式
 * AAC要添加ADTS Header
 *
 * 音频编码时比特率设置高一些，太低的话会导致杂音严重
 */
class AACEncoder(val audioPath:String) : Thread("AACEncoder") {

    private lateinit var aacEncoder: MediaCodec
    private var isEncoder = true
    private val mBufferPool = LinkedBlockingQueue<ByteBuffer>()
    private var dataListener: IEncoderDataListener? = null
    private var outFile = BufferedOutputStream(FileOutputStream(audioPath))


    override fun run() {
        initConfig()
    }

    private fun initConfig() {
        val audioFormat = MediaFormat.createAudioFormat(AAC_MIME_TYPE, 44100, 2)
        // 录制音频时，比特率太低会导致杂音严重
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 160000)
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO)
        audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        aacEncoder = MediaCodec.createEncoderByType(AAC_MIME_TYPE)
        aacEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        aacEncoder.start()
        onDrain()
    }


    private fun onDrain() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isEncoder) {
            val inputerBufferIndex = aacEncoder.dequeueInputBuffer(TIMEOUT)
            if (inputerBufferIndex >= 0) {
                val dataBuffer = mBufferPool.poll(1000, TimeUnit.MILLISECONDS)
                val inputBuffer = aacEncoder.getInputBuffer(inputerBufferIndex)
                dataBuffer?.let {
                    inputBuffer?.clear()
                    //Log.i(TAG, "size: "+it.remaining()+"    input:"+inputBuffer?.remaining())
                    inputBuffer?.put(it)
                    aacEncoder.queueInputBuffer(inputerBufferIndex, 0, dataBuffer.capacity(), 0, 0)
                }
            }

            /**
             * 获取一个输出缓存句柄，-1表示没有可用的
             * bufferInfo参数包含被编码好的数据，包括pts，size，flags
             */
            val outputBufferIndex = aacEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT)
            if (outputBufferIndex >= 0) {
                if ((bufferInfo.flags and MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) != 0) {
                    //MediaFormat发生改变，通常只会在开始调用一次，可以设置混合器的轨道
                    Log.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED: " + dataListener)
                    val audioForamt = aacEncoder.outputFormat
                    val adts = audioForamt.getByteBuffer("csd-0")
                    Log.i(TAG, "adts: " + Utils.bytesToHex(adts?.array()))
                    adts?.array()?.forEachIndexed { index, byte ->
                        Log.i(TAG, "index: " + index + "   byte: " + byte)
                    }

                    Log.i("MDY", "onDrain: " + adts?.array().toString())
                    dataListener?.notifyMediaFormat(audioForamt, false)
                } else if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(TAG, "BUFFER_FLAG_END_OF_STREAM: ")
                    isEncoder = false
                } else {
                    val outputBuffer = aacEncoder.getOutputBuffer(outputBufferIndex)
                    outputBuffer?.let {
// 将音频写入文件，主要是为了验证编码后的音频是否正确
//                        val aacData = ByteArray(bufferInfo.size + 7)
//                        // 将数据复制到aacData数组中
//                        //获取编码后的AAC数据，并添加头部参数
//                        AacUtils.addADTStoPacket(aacData)
//
//                        it.position(bufferInfo.offset)
//                        it.limit(bufferInfo.offset + bufferInfo.size)
//                        it.get(aacData, 7, bufferInfo.size)
//                        //将编码后的音频写入文件
//                        writeAudio2File(aacData, outFile)

                        val aacData = ByteArray(bufferInfo.size)
                        it.get(aacData, bufferInfo.offset, bufferInfo.size)
                        // 音频的pts总是偶尔出现降序的问题，导致合并失败，所以这里重新设置pts
                        val audioPTS = getPts()
                        val copy = MediaCodec.BufferInfo()
                        copy.set(bufferInfo.offset, bufferInfo.size,
                            audioPTS, bufferInfo.flags)
                        val copyBuffer = ByteBuffer.allocateDirect(bufferInfo.size)
                        copyBuffer.put(aacData, 0, bufferInfo.size)
                        copyBuffer.clear()

                        val pkt = MediaPacket().apply {
                            info = copy
                            data = copyBuffer
                            pts = audioPTS
                            isAudio = true
                        }
                        dataListener?.notifyAvailableData(pkt)
                    }
                    aacEncoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
        }
        aacEncoder.stop()
        aacEncoder.release()
    }

    private var lastPresentationTimeUs = 0L

    private val duration = 1024f / 44100 * 1000

    private fun getPts(): Long {
        if (lastPresentationTimeUs == 0L) {
            lastPresentationTimeUs = System.currentTimeMillis()
        } else {
            lastPresentationTimeUs += duration.toInt()
        }
        return lastPresentationTimeUs * 1000
    }

    fun frameBuffer(bytedata: ByteBuffer) {
        // put 将元素插入到队列尾部，空间不足时可等待插入
        if (isEncoder) {
            mBufferPool.put(bytedata)
        }
/*      add 在由于容量限制导无法插入时，会抛出 IllegalStateException 异常
        mBufferPool.add(byteBuffer)
        offer 插入元素到队列的尾部，返回插入结果，可以设置超时时间
        mBufferPool.offer(byteBuffer)
        mBufferPool.offer(byteBuffer,1000,TimeUnit.MILLISECONDS)*/
    }


    fun setListener(listener: IEncoderDataListener?) {
        dataListener = listener
    }

    fun stopEncoder() {
        isEncoder = false
    }



    fun writeAudio2File(byteArray: ByteArray, outPutStream: OutputStream) {
        outPutStream.write(byteArray)
        outPutStream.flush()
        Log.d("audio_remain", "write success")
    }

    companion object {
        private const val TAG = "AACEncoder"
        private const val TIMEOUT = 10000L
        private const val AAC_MIME_TYPE = "audio/mp4a-latm"
        private const val CSD_INDEX = "csd-0"
        private const val AAC_HEADER_SIZE = 7
    }
}