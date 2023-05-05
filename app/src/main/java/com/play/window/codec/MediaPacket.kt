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
}