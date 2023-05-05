package com.play.window.capture

import android.graphics.Bitmap
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
import com.play.window.model.GLRect
import com.play.window.render.SurfaceParam
import com.play.window.render.WindowRender
import com.play.window.render.gles.EglCore
import com.play.window.render.model.BitmapScene
import com.play.window.render.model.TextureInfo
import com.play.window.temp.GlUtils
import javax.microedition.khronos.egl.EGL10


/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 11:55
 */
class IWindowImpl() : HandlerThread("IWindowImpl"), IWindow {

    private var windowHandler: Handler? = null
    private var windowRender:WindowRender? = null
    private val lock = Object()

    init {
        start()
    }


    override fun onLooperPrepared() {
        synchronized(lock) {
            windowRender = WindowRender()
            windowHandler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        PLAYVIDEO -> {
                            val info = msg.obj as DisplayInfo
                            windowRender?.addDisplaySurface(info)
                        }

                        PLAYAUDIO -> {
                            val process = AudioProcess()
                        }

                        ADDBITMAP -> {
                            val scene = msg.obj as BitmapScene
                            windowRender?.addBitmap(scene)
                        }
                    }
                }
            }
            lock.notifyAll()
        }
    }

    override fun playVideo(info: DisplayInfo){
        synchronized(lock) {
            if(windowHandler == null){
                lock.wait()
            }
            windowHandler?.let {
                it.sendMessage(it.obtainMessage(PLAYVIDEO, info))
                it.sendMessage(it.obtainMessage(PLAYAUDIO))
            }
        }
    }

    override fun addBitmap(bitmap: Bitmap, rect: GLRect): Int {
        windowHandler?.let {
            it.sendMessage(it.obtainMessage(ADDBITMAP, BitmapScene(bitmap,rect)))
        }
        return 1
    }

    override fun updateBitmap(sceneId: Int, bitmap: Bitmap, rect: GLRect): Int {
        TODO("Not yet implemented")
    }

    override fun removeBitmap(sceneId: Int) {
        TODO("Not yet implemented")
    }

    override fun release() {

    }


    companion object {
        private const val PLAYVIDEO = 0x11
        private const val PLAYAUDIO = 0x12
        private const val ADDBITMAP = 0x13
    }
}