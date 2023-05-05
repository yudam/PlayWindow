package com.play.window.utils


/**
 *  AAC工具类
 */
object AacUtils {


    /**
     * 添加ADTS头部信息 ,采用7个字节表示
     * https://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
     *
     * profile 表示使用哪个级别的AAC
     * 1: AAC Main
     * 2: AAC LC (Low Complexity)
     * 3: AAC SSR (Scalable Sample Rate)
     * 4: AAC LTP (Long Term Prediction)
     *
     * freqIdx表示选择的采样率下标，对应着不同的采样率
     * 0: 96000 Hz
     * 1: 88200 Hz
     * 2: 64000 Hz
     * 3: 48000 Hz
     * 4: 44100 Hz
     *
     * chanCfg 表示声道
     * 1: 1 channel: front-center
     * 2: 2 channels: front-left, front-right
     */

    fun addADTStoPacket(
        packet: ByteArray,
        profile: Int = 2,
        freqIdx: Int = 4,
        chanCfg: Int = 2,
    ) {

        val packetLen = packet.size
        /**
         * 填充header头部数据
         */
        packet[0] = 0xFF.toByte()  //ADTS Header的头部开始
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

}