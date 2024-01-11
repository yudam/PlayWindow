package com.play.window.model

import android.graphics.SurfaceTexture
import android.view.Surface

/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 13:57
 */
class DisplayInfo(
    var rect: GLRect,
    var fps: Int,
) {

    var test:Boolean = false

    var url: String? = null

    var surfaceTexture: SurfaceTexture? = null

    var surface: Any? = null

    var mTetxureId: Int? = null

    var mvpMatrix: FloatArray? = null

    var surfaceId: Int = SURFACE_ID++


    var isOutPut:Boolean = false


    fun isPreView() = url == null


    override fun toString(): String {
        return "surfaceTexture :$surfaceTexture mTetxureId:  $mTetxureId"
    }


    fun copyInfo(surface: SurfaceTexture?): DisplayInfo {
        return DisplayInfo(rect, fps).also {
            it.url = url
            it.surfaceTexture = surface
            it.mTetxureId = mTetxureId
            it.mvpMatrix = mvpMatrix
        }
    }

    companion object {

        @Volatile
        var SURFACE_ID: Int = 0
    }
}