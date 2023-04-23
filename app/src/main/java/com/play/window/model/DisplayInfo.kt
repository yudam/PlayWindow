package com.play.window.model

import android.graphics.SurfaceTexture

/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 13:57
 */
class DisplayInfo(
    val url:String,
    val rect: GLRect,
    val surfaceTexture: SurfaceTexture,
    val fps: Int,
) {

    var mTetxureId:Int? = null

    var mvpMatrix:FloatArray? = null

    override fun toString(): String {
        return super.toString()
    }

}