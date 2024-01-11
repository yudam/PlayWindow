package com.play.window.utils

import java.nio.ByteBuffer

/**
 * User: maodayu
 * Date: 2023/11/10
 * Time: 13:25
 */
object ByteBufferUtils {

    /**
     * flip 在重置limit和position，方便外部重新读写缓存
     */
    fun cloneBuffer(buffer: ByteBuffer): ByteBuffer {
        val sizeInByte = buffer.remaining()
        val byteArray = ByteArray(sizeInByte)
        buffer.get(byteArray, 0, sizeInByte)
        buffer.flip()
        val byteBuffer = ByteBuffer.allocateDirect(sizeInByte)
        byteBuffer.put(byteArray)
        byteBuffer.flip()
        return byteBuffer
    }

}