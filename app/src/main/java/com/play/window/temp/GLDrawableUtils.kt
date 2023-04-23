package com.play.window.temp

import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * User: maodayu
 * Date: 2023/1/6
 * Time: 15:41
 */
object GLDrawableUtils {
    var common_vertext_coord_full = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    var common_vertext_coord = floatArrayOf(
        -0.5f, -0.5f,
        0.5f, -0.5f,
        -0.5f, 0.5f,
        0.5f, 0.5f
    )

    var common_fragment_coord = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    var common_fbo_fragment_coord = floatArrayOf(
        0f,0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )


    fun getByteBuffer(array: FloatArray): Buffer {
        return ByteBuffer.allocateDirect(array.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .position(0)
    }
}