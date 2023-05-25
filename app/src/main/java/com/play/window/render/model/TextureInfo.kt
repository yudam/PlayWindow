package com.play.window.render.model

import com.play.window.model.GLRect
import com.play.window.render.TextureProgram

/**
 * User: maodayu
 * Date: 2023/4/25
 * Time: 11:58
 */
data class TextureInfo(val texture: Int, val rect: GLRect, var isOES: Boolean = true) {


    var mProgram: TextureProgram? = null

    var surfaceId: Int? = null

    var sceneId: Int? = null

    /**
     * 当前TextureInfo之后绘制的纹理
     */
    val nextTextureInfoList = mutableListOf<TextureInfo>()
}