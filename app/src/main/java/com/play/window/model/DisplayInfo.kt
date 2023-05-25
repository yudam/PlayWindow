package com.play.window.model

import android.graphics.SurfaceTexture
import android.view.Surface

/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 13:57
 */
class DisplayInfo(
    val url: String,
    val rect: GLRect,
    val surfaceTexture: SurfaceTexture?,
    val fps: Int,
) {

    var mTetxureId: Int? = null

    var mvpMatrix: FloatArray? = null

    val  surfaceId: Int = surfaceTexture.hashCode()

    var surface: Surface? = null

    override fun toString(): String {
        return "surfaceTexture :$surfaceTexture mTetxureId:  $mTetxureId"
    }



    fun copyInfo(surface: SurfaceTexture?):DisplayInfo{
        return DisplayInfo(url,rect, surface, fps).also {
            it.mTetxureId = mTetxureId
            it.mvpMatrix = mvpMatrix
        }
    }
}