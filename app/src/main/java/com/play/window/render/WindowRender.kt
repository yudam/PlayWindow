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
import com.play.window.capture.VideoParse
import com.play.window.capture.VideoPlayer
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.EglCore2
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.BitmapScene
import com.play.window.render.model.Transtion
import com.play.window.temp.GlUtils
import java.util.concurrent.locks.ReentrantLock

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

    private var encoderSurfaceId = -11

    private val shareLock = ReentrantLock()


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
    fun addEncoderSurface(surface: Surface) {

    }

    /**
     * 添加SurfaceTexture
     */
    fun addDisplaySurface(info: DisplayInfo) {

        var param = VideoParse.getSurfaceParam(info.url)
        if (param == null) {
            param = createTextureInfo(info)
            VideoParse.addSurfaceParam(info.url!!, param)
            VideoPlayer(info.url!!, Surface(param.surfaceTexture))
            previewSurfaceId = info.surfaceId
        } else {
            info.mTetxureId = param.texture
            return
        }
        val drawSurface = DrawSurface(mEglCore?.sharedContext,shareLock).apply {
            setSurface(info.surfaceTexture)
            setRenderSize(info.rect.pw.toInt(), info.rect.ph.toInt())
            setFps(info.fps)
        }
        drawSurface.scheduleEvent(mEglCore!!)
        drawSurface.addDisplaySurface(info.mTetxureId!!, info.rect)
        surfaceMap[info.surfaceId] = drawSurface
    }

    /**
     * 添加转场动画
     */
    fun addTransition(nextInfo: DisplayInfo) {
        val preInfo = surfaceMap[previewSurfaceId]?.getTextureInfo()
        val nextInfo = surfaceMap[nextInfo.surfaceId]?.getTextureInfo()
        if(preInfo == null || nextInfo == null) return
        val transtion = Transtion(preInfo, nextInfo, false)
        surfaceMap[previewSurfaceId]?.setTranstion(transtion)
    }


    fun addOffScreen() {

    }

    /**
     * 添加编码的Surface，这里的surface推荐是输出编码的surface，通过获取预览界面的输出纹理
     * 添加到编码对应的DrawSurface中，直接绘制然后编码
     */
    fun setEncoderSurface(surface: Any?, rect: GLRect) {
        val drawSurface = surfaceMap[previewSurfaceId] ?: return
        val renderInfo = drawSurface.getTextureInfo() ?: return
        val encoderSurface = DrawSurface(mEglCore?.sharedContext,shareLock).apply {
            setSurface(surface)
            setRenderSize(rect.pw.toInt(), rect.ph.toInt())
            setFps(drawSurface.getFps())
            setEncode(true)
        }
        encoderSurface.scheduleEvent(mEglCore!!)
        encoderSurface.addDisplaySurface(renderInfo.texture, rect, renderInfo.isOES)
        surfaceMap[encoderSurfaceId] = encoderSurface
    }


    fun copyPreViewSurface(surfaceTexture: SurfaceTexture) {

    }

    fun addBitmap(scene: BitmapScene) {
        val texture = GlUtils.getTexture(scene.bitmap)
        surfaceMap[scene.surfaceId]?.addBitmap(texture, scene.rect)
    }

    /**
     * 当同步Surfacetexture中画面到绑定的纹理时，有可能其他啊的Surface正在使用当前纹理进行绘制，
     * 可能会出现冲突，需要加锁
     */
    private fun createTextureInfo(info: DisplayInfo): SurfaceParam {
        val textureId = GlUtils.getTexture(true)
        val surface = SurfaceTexture(textureId)
        surface.setOnFrameAvailableListener {
            shareLock.lock()
            try {
                it.updateTexImage()
            } finally {
                shareLock.unlock()
            }
        }
        info.mTetxureId = textureId
        return SurfaceParam(textureId, surface)
    }
}