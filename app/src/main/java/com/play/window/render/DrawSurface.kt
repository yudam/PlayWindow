package com.play.window.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.play.window.R
import com.play.window.WindowApp
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.GlUtil
import com.play.window.render.gles.WindowSurface
import com.play.window.render.model.TextureInfo
import com.play.window.render.surface.SceneInfo

/**
 * User: maodayu
 * Date: 2023/4/17
 * Time: 09:56
 * 处理Surface的渲染,每一个Surface都有对应的DrawSurface
 */
class DrawSurface(val shareContext: EGLContext?) : Runnable {

    private var mLooper: Looper? = null
    private var drawHandler: Handler? = null

    private var info: DisplayInfo? = null

    private var mEglCore: EglCore? = null;
    private var mWindowSurface: WindowSurface? = null

    private var lock = Object()

    private var frameRate = 30

    private var graphProcess: GraphProcess? = null

    private val renderLock = Object()

    init {
        Thread(this).start()
    }

    override fun run() {
        graphProcess = GraphProcess()
        Looper.prepare()
        mLooper = Looper.myLooper()
        synchronized(lock) {
            scheduleEvent()
            lock.notifyAll()
        }
        Looper.loop()
        mWindowSurface?.release()
        mEglCore?.release()
    }


    private fun scheduleEvent() {
        if (mLooper == null) return
        drawHandler = object : Handler(mLooper!!) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    ADDSCENE -> {
                        info = msg.obj as DisplayInfo
                        frameRate = info!!.fps
                        mEglCore = EglCore(shareContext, 0)
                        if (info?.surface != null) {
                            mWindowSurface = WindowSurface(mEglCore!!, info!!.surface)
                        } else {
                            mWindowSurface = WindowSurface(mEglCore!!, info!!.surfaceTexture)
                        }
                        graphProcess?.addTextureInfo(TextureInfo(info!!.mTetxureId!!, info!!.rect))
                        drawTimeControll()
                    }

                    DRAWSCENE -> {
                        drawTimeControll()
                    }
                }
            }
        }
    }

    private var expectTime = 0L

    private var cumulative = 0L

    private fun drawTimeControll() {

        val currTime = System.nanoTime()

        if (expectTime == 0L) {
            expectTime = currTime
        } else {
            cumulative += (expectTime - currTime)
        }

        drawScene()

        val per = (1000 * 1000 * 1000) / frameRate
        expectTime += per

        var delay = expectTime - System.nanoTime() + cumulative

        if (delay < 0) delay = 0

        if (delay < 5 * 1000) delay = 5 * 1000

        drawHandler?.sendEmptyMessageDelayed(DRAWSCENE, delay / 1000 / 1000)


    }

    private fun drawScene() {
        mWindowSurface?.makeCurrent()
        synchronized(renderLock) {
            graphProcess?.draw()
        }
        mWindowSurface?.swapBuffers()
    }


    /**
     *  添加渲染表面
     */
    fun addDisplaySurface(info: DisplayInfo) {
        synchronized(lock) {
            if (drawHandler == null) {
                lock.wait()
            }
        }
        drawHandler?.let {
            it.sendMessage(it.obtainMessage(ADDSCENE, info))
        }

    }

    fun addBitmap(texture: Int, rect: GLRect) {
        graphProcess?.addTextureInfo(TextureInfo(texture, rect, false))
    }


    fun getSurfaceTexture(): SurfaceTexture {
        return info!!.surfaceTexture!!
    }

    fun getInfo(): DisplayInfo? {
        return info
    }

    companion object {
        private const val ADDSCENE = 0x11
        private const val DRAWSCENE = 0x12
    }
}