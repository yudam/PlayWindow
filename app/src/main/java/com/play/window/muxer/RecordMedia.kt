package com.play.window.muxer

import com.play.window.codec.MediaPacket
import com.play.window.utils.MuxerUtil

/**
 * User: maodayu
 * Date: 2023/6/8
 * Time: 17:21
 */
class RecordMedia(val path:String):Runnable {

    private val audioQueue = ArrayDeque<MediaPacket>()
    private val videoQueue = ArrayDeque<MediaPacket>()

    private var isWriteIFrame:Boolean = false
    private var isMuxer:Boolean = false
    private val muxerUtil = MuxerUtil(path)

    init {
        isMuxer = true
        Thread(this).start()
    }

    fun addVideo(packet: MediaPacket){
        videoQueue.add(packet)
    }

    fun addAudio(packet: MediaPacket){
        audioQueue.add(packet)
    }

    override fun run() {

    }
}