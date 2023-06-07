package com.play.window.render.model

/**
 * User: maodayu
 * Date: 2023/5/26
 * Time: 15:41
 */
data class Transtion(
    val preInfo:TextureInfo,
    val nextInfo: TextureInfo,
    var isFinish: Boolean,
)