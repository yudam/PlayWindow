package com.play.window.codec

import android.media.MediaCodecList
import android.media.MediaFormat
import com.play.window.utils.MediaConfig

/**
 * User: maodayu
 * Date: 2023/11/11
 * Time: 15:35
 */
interface IEncoder {

    fun startEncoder()
    fun stopEncoder()
    fun getInputSurface(): Any?
    fun getInputRect(): Any?
    fun addListener(listener: EncodeListener)

    interface EncodeListener {
        fun notifyAvailableData(packet: MediaPacket)

        fun notifyMediaFormat(format: MediaFormat, isVideo:Boolean)

        fun notifyHeaderData(packet: MediaPacket){}

        fun notifyEnd(){}
    }

    interface Factory {
        fun createEncoder(config:MediaConfig): IEncoder
    }
}