package com.play.window.render

import android.graphics.SurfaceTexture

/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 12:01
 */
class SurfaceParam(val texture:Int,val surfaceTexture: SurfaceTexture?) {

    var url:String? = null
    override fun toString(): String {
        return "SurfaceParam(texture=$texture, surfaceTexture=$surfaceTexture, url=$url)"
    }


}