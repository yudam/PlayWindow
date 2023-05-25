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

    private val textureList = mutableListOf<TextureInfo>()

    val matrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }

    init {
        Thread(this).start()
    }

    override fun run() {
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
                        if(info?.surface != null){
                            mWindowSurface = WindowSurface(mEglCore!!, info!!.surface)
                        } else {
                            mWindowSurface = WindowSurface(mEglCore!!, info!!.surfaceTexture)
                        }
                        textureList.add(TextureInfo(info!!.mTetxureId!!, info!!.rect))
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

        drawHandler?.sendEmptyMessageDelayed(DRAWSCENE, delay/1000/1000)


    }

    private fun drawScene() {
        mWindowSurface?.makeCurrent()
        //clear操作一定要在最外层做，否则会清除之前的渲染导致混合不生效
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        textureList.forEach {
            if (it.mProgram == null) {
                it.mProgram =  if(it.isOES){
                    TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_oes_shader))
                } else {
                    TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_fragment_shader))
                }
            }
            GLES20.glViewport(0,0,it.rect.pw.toInt(),it.rect.ph.toInt())
            it.mProgram?.render(parseVertexArray(it.rect), parseFragmentArray(it.rect), it.texture,
                matrix,it.isOES)
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

    fun addBitmap(texture:Int,rect: GLRect){
        textureList.add(TextureInfo(texture,rect,false))
    }


    fun getSurfaceTexture():SurfaceTexture{
        return info!!.surfaceTexture!!
    }

    private fun parseVertexArray(rect: GLRect): FloatArray {

        val cx = (rect.cx / rect.pw) * 2 - 1
        val cy = (rect.cy / rect.ph) * 2 - 1
        val w = (rect.width / rect.pw) * 2
        val h = (rect.height / rect.ph) * 2

        val vertex = floatArrayOf(
            cx - w / 2, cy + h / 2,      // bottom left
            cx + w / 2, cy + h / 2,      // bottom right
            cx - w / 2, cy - h / 2,      // top left
            cx + w / 2, cy - h / 2       // top right
        )
        return vertex
    }

    private fun parseFragmentArray(rect: GLRect): FloatArray {
        val cx = rect.cx / rect.pw
        val cy = rect.cy / rect.ph
        val w = rect.width / rect.pw
        val h = rect.height / rect.ph
        val fragment = floatArrayOf(
            cx - w / 2, cy - h / 2,      // top left
            cx + w / 2, cy - h / 2,      // top right
            cx - w / 2, cy + h / 2,      // bottom left
            cx + w / 2, cy + h / 2       // bottom right
        )
        return fragment
    }

    fun getInfo():DisplayInfo?{
     return info
    }

    companion object {
        private const val ADDSCENE = 0x11
        private const val DRAWSCENE = 0x12
    }
}