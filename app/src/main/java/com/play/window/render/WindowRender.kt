package com.play.window.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.Window
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
    private val surfaceMap = mutableMapOf<Int, DrawSurface?>()
    private var previewSurfaceId = 0


    init {
        mEglCore = EglCore()
        mEglCore?.makeCurrent(null, null)
    }

    fun getPreViewSurfaceTexture(): SurfaceTexture? {
        return surfaceMap[previewSurfaceId]?.getSurfaceTexture()
    }

    /**
     * 添加Surface
     */
    fun addEncoderSurface(surface: Surface){

    }

    /**
     * 添加SurfaceTexture
     */
    fun addDisplaySurface(info: DisplayInfo) {
        val params = createTextureInfo(info)
        surfaceList.add(params)
        Log.i(WindowApp.TAG, "addDisplaySurface: " + params.texture)
        VideoPlayer(info.url, Surface(params.surfaceTexture))
        val drawSurface = DrawSurface(mEglCore?.sharedContext)
        drawSurface.addDisplaySurface(info)
        previewSurfaceId = info.surfaceId
        surfaceMap[info.surfaceId] = drawSurface
    }

    fun setEncoderSurface(surface: Surface){
        val preInfo = surfaceMap[previewSurfaceId]?.getInfo()
        Log.i(WindowApp.TAG, "preInfo: "+preInfo.toString())
        if (preInfo != null) {
            val data = preInfo.copyInfo(null)
            data.surface = surface
            Log.i(WindowApp.TAG, "data: "+data.toString())
            val drawSurface = DrawSurface(mEglCore?.sharedContext)
            drawSurface.addDisplaySurface(data)
        }
    }


    fun copyPreViewSurface(surfaceTexture: SurfaceTexture) {

        val preInfo = surfaceMap[previewSurfaceId]?.getInfo()

        Log.i(WindowApp.TAG, "preInfo: "+preInfo.toString())
        if (preInfo != null) {
            val data = preInfo.copyInfo(surfaceTexture)
            Log.i(WindowApp.TAG, "data: "+data.toString())
            val drawSurface = DrawSurface(mEglCore?.sharedContext)
            drawSurface.addDisplaySurface(data)
        }
    }

    fun addBitmap(scene: BitmapScene) {
        val texture = GlUtils.getTexture(scene.bitmap)
        surfaceMap[scene.surfaceId]?.addBitmap(texture, scene.rect)
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