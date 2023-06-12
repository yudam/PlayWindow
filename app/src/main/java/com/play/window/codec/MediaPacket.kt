package com.play.window.codec

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * User: maodayu
 * Date: 2023/4/23
 * Time: 19:16
 */
class MediaPacket {
    var data: ByteBuffer? = null
    var info: MediaCodec.BufferInfo? = null
    var pts: Long = -1
    var isVideo = false
    var isAudio = false
    var meidaFormat: MediaFormat? = null

    var isCsd = false
    var csd0: ByteBuffer? = null
    var csd1: ByteBuffer? = null


    var csd0Size: Int = -1
    var csd1Size: Int = -1
    var bufferSize: Int = -1
    override fun toString(): String {
        return "MediaPacket(pts=$pts, isVideo=$isVideo, isAudio=$isAudio, isCsd=$isCsd, bufferSize=$bufferSize)"
    }


}