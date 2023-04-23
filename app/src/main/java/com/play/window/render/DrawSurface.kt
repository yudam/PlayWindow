package com.play.window.render

import android.opengl.EGLContext
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.play.window.R
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.GlUtil
import com.play.window.render.gles.WindowSurface
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

    private var mProgram: TextureProgram? = null
    private var info: DisplayInfo? = null

    private var mEglCore: EglCore? = null;
    private var mWindowSurface: WindowSurface? = null

    private var lock = Object()

    val matrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }

    init {
        Thread(this).start()
    }

    override fun run() {
        Looper.prepare()
        mLooper = Looper.myLooper()
        synchronized(lock){
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
                        mEglCore = EglCore(shareContext,0)
                        mWindowSurface = WindowSurface(mEglCore!!,info!!.surfaceTexture)

                        drawHandler?.sendEmptyMessageDelayed(DRAWSCENE,50)
                    }

                    DRAWSCENE -> {
                        mWindowSurface?.makeCurrent()
                        drawScene()
                        mWindowSurface?.swapBuffers()
                        drawHandler?.sendEmptyMessageDelayed(DRAWSCENE,50)
                    }
                }
            }
        }
    }

    private fun drawScene(){

        if(mProgram == null){
            mProgram = TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                GlUtil.readRawResourse(R.raw.simple_oes_shader))
        }
        if (info?.mTetxureId != null) {
            mProgram?.render(parseVertexArray(info!!.rect), parseFragmentArray(info!!.rect), info!!.mTetxureId!!,
                matrix)
        }
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


    private fun parseVertexArray(rect: GLRect): FloatArray {

        val cx = (rect.cx / rect.pw) * 2 - 1
        val cy = (rect.cy / rect.ph)* 2 - 1
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
        val w = rect.width  / rect.pw
        val h = rect.height / rect.ph
        val fragment = floatArrayOf(
            cx - w / 2, cy - h / 2,      // top left
            cx + w / 2, cy - h / 2,      // top right
            cx - w / 2, cy + h / 2,      // bottom left
            cx + w / 2, cy + h / 2       // bottom right
        )
        return fragment
    }

    companion object {
        private const val ADDSCENE = 0x11
        private const val DRAWSCENE = 0x12
    }
}