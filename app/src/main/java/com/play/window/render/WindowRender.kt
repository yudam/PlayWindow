package com.play.window.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.util.Log
import android.view.Surface
import com.play.window.R
import com.play.window.WindowApp
import com.play.window.capture.VideoPlayer
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.EglCore2
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.BitmapScene
import com.play.window.temp.GlUtils

/**
 * User: maodayu
 * Date: 2023/4/13
 * Time: 19:16
 *
 */
class WindowRender() {

    private var mEglCore: EglCore? = null
    private var windowHandler: Handler? = null
    private val surfaceList = mutableListOf<SurfaceParam>()
    private var drawSurface:DrawSurface? = null

    init {
        mEglCore = EglCore()
        mEglCore?.makeCurrent(null, null)
    }

    /**
     * 添加Surface
     */
    fun addDisplaySurface(info: DisplayInfo) {
        val params = createTextureInfo(info)
        surfaceList.add(params)
        Log.i(WindowApp.TAG, "addDisplaySurface: "+params.texture)
        VideoPlayer(info.url, Surface(params.surfaceTexture))
        drawSurface = DrawSurface(mEglCore?.sharedContext)
        drawSurface?.addDisplaySurface(info)
    }

    fun addBitmap(scene: BitmapScene){
        val texture = GlUtils.getTexture(scene.bitmap)
        drawSurface?.addBitmap(texture,scene.rect)
    }


    private fun createTextureInfo(info: DisplayInfo): SurfaceParam {
        val textureId = GlUtils.getTexture(true)
        val surface = SurfaceTexture(textureId)
        surface.setOnFrameAvailableListener {
            it.updateTexImage()
        }
        info.mTetxureId = textureId
        return SurfaceParam(textureId, surface)
    }
}