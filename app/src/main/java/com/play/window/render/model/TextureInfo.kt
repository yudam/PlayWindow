package com.play.window.render.model

import com.play.window.model.GLRect
import com.play.window.render.TextureProgram

/**
 * User: maodayu
 * Date: 2023/4/25
 * Time: 11:58
 */
data class TextureInfo(
    val stream: Stream,
    var texture: Int,
    var rect: GLRect,
    var isOES: Boolean = true,
) {

    var surfaceId: Int = 0

    var mProgram: TextureProgram? = null

    fun clone(rect: GLRect): TextureInfo {
        return TextureInfo(stream, texture, rect, isOES)
    }
}