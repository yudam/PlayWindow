package com.play.window

import com.play.window.codec.MediaPacket

/**
 * User: maodayu
 * Date: 2023/4/25
 * Time: 10:18
 */

object BaseNative {

    fun loadLibrary() {
        System.loadLibrary("window")
    }


    external fun stringFromJNI(): String

    /**
     * 本地视频RTMP推流
     */
    external fun app_Open(infilename: String, outfilename: String)
    external fun app_Push()
    external fun app_close()


    /**
     * 实时RTMP推流
     */

    external fun initPublish(url: String, videoBitRate: Int, framerate: Int, width: Int, height: Int)

    external fun connect()

    external fun sendPacket(packet: MediaPacket)

    external fun release()

    /**
     * FFMPEG保存AudioRecord录制的PCM音频为AAC
     */
    external fun startAudioRecord(path: String)
    external fun setFrameData(data: ByteArray,len:Int)
    external fun stopAudioRecord()

    /**
     * FFMPEG推流视频到RTMP
     */
    external fun startPublic(rtmpUrl:String,width: Int,height: Int)
    external fun setVideoData(byteArray: ByteArray,len: Int)
    external fun stopPublish()
}