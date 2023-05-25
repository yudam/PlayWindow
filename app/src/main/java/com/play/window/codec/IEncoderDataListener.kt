package com.play.window.codec

import android.media.MediaFormat

/**
 * User: maodayu
 * Date: 2023/2/20
 * Time: 17:47
 */
interface IEncoderDataListener {

    fun notifyAvailableData(packet: MediaPacket)

    fun notifyMediaFormat(format: MediaFormat,isVideo:Boolean)

    fun notifyHeaderData(packet: MediaPacket){}

    fun notifyEnd(){}
}