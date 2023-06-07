package com.play.window.utils

import android.media.MediaCodecInfo


/**
 * User: maodayu
 * Date: 2023/5/11
 * Time: 16:21
 */
class MediaConfig {

    var publishUrl:String = "rtmp://172.16.0.97:1935/live/room"

    var videoBitRate = 1920 * 1080 * 5

    var videoWidth = 1920

    var videoHeight = 1080

    var videoFrameRate = 20

    var videoBitRateModel = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR

    var i_frame_interval = 2
}