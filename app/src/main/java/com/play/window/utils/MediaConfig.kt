package com.play.window.utils

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface


/**
 * User: maodayu
 * Date: 2023/5/11
 * Time: 16:21
 */
class MediaConfig {

    //192.168.20.20
    var publishUrl: String = "rtmp://192.168.30.120:1935/live/room"

    var videoBitRate = 1920 * 1080 * 3

    var videoWidth = 3840

    var videoHeight = 2160

    var videoFrameRate = 20

    var videoBitRateModel = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR

    var i_frame_interval = 2

    var mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC


    var openQP: Boolean = false
    var minIQP = 1
    var maxIQP = 51
}