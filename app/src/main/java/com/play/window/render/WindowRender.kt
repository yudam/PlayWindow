package com.play.window.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.util.Log
import android.util.Size
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
import com.play.window.render.model.TextureInfo
import com.play.window.render.model.Transtion
import com.play.window.render.process.OpenglUtil
import com.play.window.temp.GlUtils
import java.util.concurrent.locks.ReentrantLock

/**
 * User: maodayu
 * Date: 2023/4/13
 * Time: 19:16
 *
 */
class WindowRender() {

    companion object{
        val centreSurfaceId = -6
    }

    private var mEglCore: EglCore? = null
    private var windowHandler: Handler? = null
    private val surfaceList = mutableListOf<SurfaceParam>()
    private val surfaceMap = mutableMapOf<Int, DrawSurface?>()
    private var previewSurfaceId = -1

    private var encoderSurfaceId = -11

    private var directorSurfaceId = -12

    private var outPutSurfaceId = -13


    private val backSurfaceId = -101

    private val shareLock = ReentrantLock()

    private val sizeMap = mutableMapOf<Int, Size>()

    private val mSize = Size(1920, 1080)

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
     * 添加默认的渲染源
     */
    fun addOriginalSurface(){
        val width = mSize.width.toFloat()
        val height = mSize.height.toFloat()
        val glRect = GLRect(width/2,height/2,width,height,width,height)
        addDisplaySurface(DisplayInfo(glRect,30).apply { surfaceId =  centreSurfaceId})
    }


    /**
     * 添加从编码器中获取的Surface，接收预览的画面
     */
    fun setEncoderSurface(encoderSurface: Any?, rect: GLRect) {
        val info = DisplayInfo(rect,30).apply {
            surface = encoderSurface
            isOutPut = true
        }
        addDisplaySurface(info)
    }

    /**
     * 添加SurfaceTexture
     */
    fun addDisplaySurface(info: DisplayInfo) {
        if(info.isOutPut){
            val backDraw = surfaceMap[centreSurfaceId]
            info.surfaceTexture?.let {
                backDraw?.addOutPutSurface(it)
            }

            info.surface?.let {
                backDraw?.addOutPutSurface(it)
            }
        } else {
            val surfaceName = if(info.isPreView())"Preview-SUrface" else ""
            val drawSurface = DrawSurface(mEglCore?.sharedContext, shareLock, name = surfaceName).apply {
                setRenderSize(info.rect.pw.toInt(), info.rect.ph.toInt())
                setFps(info.fps)
            }
            drawSurface.scheduleEvent(mEglCore!!)
            drawSurface.addDisplaySurface(info.surfaceTexture)
            drawSurface.setOutPut(info.surfaceId == centreSurfaceId)
            drawSurface.sendDraw()
            surfaceMap[info.surfaceId] = drawSurface
            Log.i("SMP", "info.surfaceId: "+info.surfaceId)
            if (info.url != null) {
                val param = createTextureInfo(info)
                VideoParse.addSurfaceParam(info.url!!, param)
                openVideo(info, param.surfaceTexture!!)
            }
        }
    }

    /**
     * 添加纹理
     */
    fun addScene(surfaceId: Int, targetId: Int) {
        val currentDraw = surfaceMap[surfaceId]
        val targetDraw = surfaceMap[targetId]
        val size = sizeMap[targetId] ?: mSize
        val currentInfo = currentDraw?.getTextureInfo()
        val targetInfo = targetDraw?.getTextureInfo() ?: return
        val rect = OpenglUtil.createDefaultRect(mSize)
        OpenglUtil.scaleVideoRect(rect, size.width, size.height)
        currentDraw?.addScene(targetInfo.texture, rect, targetInfo.isOES)
//        if (currentInfo == null) {
//            currentDraw?.updateScene(targetInfo.texture, rect, targetInfo.isOES)
//        } else {
//            currentDraw.setTranstion(Transtion(currentInfo.clone(rect), targetInfo.clone(rect), false))
//            currentDraw.updateScene(targetInfo.texture, rect, targetInfo.isOES)
//        }
    }



    /**
     * 设置、更新预览界面
     */
    fun updateSurface(surfaceTexture: Any, surfaceId: Int, rect: GLRect) {
        val preInfo = surfaceMap[previewSurfaceId]?.getTextureInfo()
        previewSurfaceId = surfaceId
        val drawSurface = surfaceMap[previewSurfaceId] ?: return
        val renderInfo = drawSurface.getTextureInfo() ?: return
        val encoderView = surfaceMap[encoderSurfaceId]
        if (encoderView == drawSurface) return
        if (encoderView != null) {
            val size = sizeMap[previewSurfaceId] ?: Size(1920, 1080)
            OpenglUtil.scaleVideoRect(rect, size.width, size.height)
            if (preInfo == null) {
                encoderView.updateScene(renderInfo.texture, rect, renderInfo.isOES)
            } else {
                encoderView.setTranstion(Transtion(preInfo.clone(rect), renderInfo.clone(rect), false))
                encoderView.updateScene(renderInfo.texture, rect, renderInfo.isOES)
            }
        } else {
            val encoderSurface = DrawSurface(mEglCore?.sharedContext, shareLock).apply {
                setRenderSize(rect.pw.toInt(), rect.ph.toInt())
                setFps(drawSurface.getFps())
                setEncode(true)
            }
            encoderSurface.scheduleEvent(mEglCore!!)
            encoderSurface.addDisplaySurface(surfaceTexture)
            val size = sizeMap[previewSurfaceId] ?: Size(1920, 1080)
            OpenglUtil.scaleVideoRect(rect, size.width, size.height)
            encoderSurface.addScene(renderInfo.texture, rect, renderInfo.isOES)
            surfaceMap[encoderSurfaceId] = encoderSurface
        }
    }


    fun copyPreViewSurface(surfaceTexture: SurfaceTexture) {

    }

    fun addBitmap(scene: BitmapScene) {
        val texture = GlUtils.getTexture(scene.bitmap)
        surfaceMap[scene.surfaceId]?.addOverlay(texture, scene.rect)
    }


    /**
     * 打开本地视频
     */
    private fun openVideo(info: DisplayInfo, surfaceTexture: SurfaceTexture) {
        val videoPlayer = VideoPlayer(info.url!!, Surface(surfaceTexture))
        videoPlayer.addPrepareVideoCallback { width, height ->
            sizeMap[info.surfaceId] = Size(width, height)
            val drawSurface = surfaceMap[info.surfaceId]
            val newRect = OpenglUtil.scaleVideoRect(info.rect, width, height)
            info.rect = newRect
            drawSurface?.addScene(info.mTetxureId!!, newRect)
        }
    }

    /**
     * 当同步Surfacetexture中画面到绑定的纹理时，有可能其他的Surface正在使用当前纹理进行绘制，
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