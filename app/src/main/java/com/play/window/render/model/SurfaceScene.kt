package com.play.window.render.model

import com.play.window.model.GLRect

/**
 * User: maodayu
 * Date: 2023/9/5
 * Time: 14:17
 */
class SurfaceScene() {

     var stream:Stream = Stream.STREAM_EMPTY

     var glRect: GLRect? = null

     var textureId: Int? = null

     var surface: Any? = null

     var fps: Int = 30

     var surfaceId: Int? = null

     var isOesTexture: Boolean = false


    constructor(rect:GLRect,texture:Int,oes:Boolean):this(){
        glRect = rect
        textureId = texture
        isOesTexture = oes
    }


    fun dataCopy():TextureInfo{
        return TextureInfo(
            stream,textureId!!,glRect!!,isOesTexture
        )
    }
}