package com.play.window.capture

import android.graphics.SurfaceTexture
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLES32
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.Surface
import com.play.window.WindowApp
import com.play.window.model.DisplayInfo
import com.play.window.render.SurfaceParam
import com.play.window.render.WindowRender
import com.play.window.render.gles.EglCore
import com.play.window.temp.GlUtils
import javax.microedition.khronos.egl.EGL10


/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 11:55
 */
class IWindowImpl() : HandlerThread("IWindowImpl"), IWindow {

    private var windowHandler: Handler? = null
    private var mEglCore: EglCore? = null
    private val lock = Object()


    private val surfaceList = mutableListOf<SurfaceParam>()


    private var windowRender:WindowRender? = null

    init {
        start()
    }


    override fun onLooperPrepared() {
        mEglCore = EglCore()
        mEglCore?.makeCurrent(null, null)
        windowRender = WindowRender(mEglCore?.sharedContext)
        synchronized(lock) {
            windowHandler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        PLAYVIDEO -> {
                            val info = msg.obj as DisplayInfo
                            val params = createTextureInfo(info)
                            surfaceList.add(params)
                            windowRender?.addDisplaySurface(info)
                            VideoPlayer(info.url, Surface(params.surfaceTexture))
                        }
                    }
                }
            }
            lock.notifyAll()
        }
    }

    override fun playVideo(info: DisplayInfo) {
        synchronized(lock) {
            if (mEglCore == null) {
                lock.wait()
            }
            windowHandler?.let {
                it.sendMessage(it.obtainMessage(PLAYVIDEO, info))
            }
        }
    }

    override fun playVideo(url: String, surface: Surface) {

    }

    override fun release() {

    }


    fun createTextureInfo(info: DisplayInfo): SurfaceParam {
        val textureId = GlUtils.getTexture(true)
        val surface = SurfaceTexture(textureId)
        surface.setOnFrameAvailableListener {
            it.updateTexImage()
        }
        info.mTetxureId = textureId
        return SurfaceParam(textureId, surface)
    }

    companion object {
        private const val PLAYVIDEO = 0x11
    }
}