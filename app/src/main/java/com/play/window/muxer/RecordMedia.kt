package com.play.window.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.play.window.codec.MediaPacket
import com.play.window.utils.MuxerUtil
import java.util.concurrent.LinkedBlockingQueue

/**
 * User: maodayu
 * Date: 2023/6/8
 * Time: 17:21
 *
 * 1.  MediaMuxer
 *
 * 2.
 *
 * 3.
 */
class RecordMedia(val path: String) : Runnable {

    private val audioQueue = LinkedBlockingQueue<MediaPacket>()
    private val videoQueue = LinkedBlockingQueue<MediaPacket>()

    private var videoCsd: MediaPacket? = null
    private var audioCsd: MediaPacket? = null

    private var isWriteIFrame: Boolean = false
    private var isMuxer: Boolean = false
    private var isAddTrack:Boolean = false
    private val muxerUtil = MuxerUtil(path)

    private var videoTime = 0L
    private var videoFormat:MediaFormat? = null
    private var audioFormat:MediaFormat? = null

    init {
        isMuxer = true
        Thread(this).start()
    }


    @Synchronized
    fun addPacket(packet: MediaPacket) {
        if (packet.isVideo) {
            if (packet.isCsd) {
                videoCsd = packet
            } else {
                videoQueue.offer(packet)

            }
        } else {
            if (packet.isCsd) {
                audioCsd = packet
            } else {
                audioQueue.offer(packet)
            }
        }
    }

    fun addTrack(format: MediaFormat, isAudio: Boolean) {
        Log.i("RecordMedia", "addTrack: $isAudio")
        if(isAudio){
            audioFormat = format
        } else {
            videoFormat = format
        }
    }

    fun release() {
        Log.i("RecordMedia", "release: ")
        muxerUtil.release()
    }

    override fun run() {
        while (isMuxer) {

            if(!isAddTrack && videoFormat != null && audioFormat != null) {
                muxerUtil.addTrack(videoFormat!!, false)
                muxerUtil.addTrack(audioFormat!!, true)
                isAddTrack = true
            }


            if(isAddTrack){
                if (videoQueue.isNotEmpty()) {
                    val packet = videoQueue.poll()
                    videoTime = packet.pts
                    writeData(packet)
                }

                if (audioQueue.isNotEmpty()) {
                    while (audioQueue.isNotEmpty()) {
                        val packet = audioQueue.element()
                        if (packet != null && packet.pts <= videoTime) {
                            writeData(packet)
                            audioQueue.poll()
                        } else {
                            break
                        }
                    }
                }
            }
        }
    }

    private fun writeData(packet: MediaPacket) {
        if (packet.isVideo) {
            if (!isWriteIFrame) {
                if (packet.info?.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                    isWriteIFrame = true
                    muxerUtil.writeSampleData(packet.data, packet.info, packet.isAudio)
                }
            } else {
                muxerUtil.writeSampleData(packet.data, packet.info, packet.isAudio)
            }
        }

        if (packet.isAudio && isWriteIFrame) {
            muxerUtil.writeSampleData(packet.data, packet.info, packet.isAudio)
        }
    }
}