package com.play.window.capture

/**
 * User: maodayu
 * Date: 2023/5/12
 * Time: 11:18
 */
interface AudioEncoderListener {

    fun startAudioRecord(path:String)

    fun stopAudioRecord()
}