package com.play.window.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES20
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Surface
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.GlUtil
import com.play.window.render.gles.OffscreenSurface
import com.play.window.render.gles.WindowSurface
import com.play.window.render.model.Stream
import com.play.window.render.model.TextureInfo
import com.play.window.render.model.Transtion
import java.util.concurrent.locks.ReentrantLock

/**
 * User: maodayu
 * Date: 2023/4/17
 * Time: 09:56
 * 处理Surface的渲染,每一个Surface都有对应的DrawSurface
 *
 * 预览界面不需要传递输出纹理给额外的Surface，所以在单独的线程中执行
 */
class DrawSurface(val shareContext: EGLContext?, val shareLock: ReentrantLock,val ownThread:Boolean = false) : Runnable {

    private var mLooper: Looper? = null
    private var drawHandler: Handler? = null

    private var info: DisplayInfo? = null

    private var mEglCore: EglCore? = null;

    private var lock = Object()

    private var frameRate = 30

    private var graphProcess: GraphProcess? = null

    private val renderLock = Object()

    private var mSurface: Any? = null

    private var mRendeWidth = 0
    private var mRenderHeight = 0

    private var mWindowSurface: WindowSurface? = null
    private var mOffscreenSurface: OffscreenSurface? = null

    private var transtion: Transtion? = null
    private var transtionAnim: TranstionAnim? = null

    private var isEncode = false


    init {
        if(ownThread){
            Thread(this).apply {
                name = "Thread-DrawSurface"
            }.start()
        }
    }

    override fun run() {
        val eglCore = EglCore(shareContext, 0)
        Looper.prepare()
        mLooper = Looper.myLooper()
        synchronized(lock) {
            scheduleEvent(eglCore)
            lock.notifyAll()
        }
        Looper.loop()
        mWindowSurface?.release()
        mEglCore?.release()
    }


    fun scheduleEvent(eglCore: EglCore) {
        mEglCore = eglCore
        graphProcess = GraphProcess(shareLock)
        drawHandler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    SURFACEVALID -> {
                        createWindowSurface(mSurface)
                    }
                    ADDSCENE -> {
                        val sceneInfo = msg.obj as TextureInfo
                        if (isEncode) {
                            //graphProcess?.setFBO(false)
                        }
                        graphProcess?.addTextureInfo(sceneInfo)
                        graphProcess?.setRenderSize(mRendeWidth, mRenderHeight)
                        drawTimeControll()
                    }

                    DRAWSCENE -> {
                        drawTimeControll()
                    }

                    TRANSTIONANIM -> {
                        transtionAnim = TranstionAnim().apply {
                            GlUtil.checkGlError("TRANSTIONANIM")
                            setRenderSize(mRendeWidth, mRenderHeight)
                            initTranstion()
                        }
                    }
                }
            }
        }

        /**
         * 必须加上这句话，否则会有一个奇怪的现象：创建的纹理和FBO都为0
         */
        mEglCore?.makeCurrent(null, null)
        synchronized(lock) {
            lock.notifyAll()
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


    private var lastTime = 0L

    private fun drawScene() {
        mWindowSurface?.makeCurrent()
        if (transtionAnim != null && !transtionAnim!!.isFinish()) {
            drawTranstion()
        } else {
            if (isEncode) {
                val currTime = System.nanoTime()
                Log.i("DRAWSURFACE", "offset: " + (currTime - lastTime) / 1000 / 1000 + "    fps:" + frameRate)
                lastTime = currTime
            }
            synchronized(renderLock) {
                graphProcess?.draw()
            }
        }

        /**
         * 将当前时间戳传入EGL中，当通过Surface获取数据时，可以获取到对应的时间戳pts
         */
        mWindowSurface?.setPresentationTime(System.currentTimeMillis() * 1000 * 1000)
        mWindowSurface?.swapBuffers()
    }

    /**
     * 只有预览界面的DrawSurface才可以设置 Transtion，执行转场动画
     *
     */
    private fun drawTranstion() {
        transtionAnim?.openFbo1()
        graphProcess?.let {
            it.getOutProcess()?.addTextureInfo(transtion!!.nextInfo)
            it.getOutProcess()?.draw()
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        transtionAnim?.openFbo2()
        graphProcess?.let {
            it.getOutProcess()?.addTextureInfo(transtion!!.preInfo)
            it.getOutProcess()?.draw()
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        transtionAnim?.anim()
    }

    private fun createWindowSurface(surface: Any?) {
        when (surface) {
            is Surface -> {
                Log.i("MDY", "createWindowSurface: Surface")
                mWindowSurface = WindowSurface(mEglCore, surface, true)
            }
            is SurfaceTexture -> {
                Log.i("MDY", "createWindowSurface: SurfaceTexture")
                mWindowSurface = WindowSurface(mEglCore, surface)
            }
            else -> {
                mOffscreenSurface = OffscreenSurface(mEglCore, mRendeWidth, mRenderHeight)
            }
        }
    }

    /**
     *  添加渲染表面
     */
    fun addDisplaySurface(surface:Any?) {
        this.mSurface = surface
        synchronized(lock) {
            if (drawHandler == null) {
                lock.wait()
            }
        }
        drawHandler?.let {
            it.sendMessage(it.obtainMessage(SURFACEVALID))
        }

    }

    /**
     * 添加纹理
     */
    fun addScene(texture: Int, rect: GLRect, isOES: Boolean = true){
        synchronized(lock) {
            if (drawHandler == null) {
                lock.wait()
            }
        }
        drawHandler?.let {
            it.sendMessage(it.obtainMessage(ADDSCENE, TextureInfo(Stream.STREAM_PREVIEW,texture, rect, isOES)))
        }
    }

    fun updateScene(texture: Int, rect: GLRect, isOES: Boolean = true){
        graphProcess?.updateTextureInfo(TextureInfo(Stream.STREAM_PREVIEW,texture, rect, isOES))
    }

    /**
     * 添加额外的水印
     */

    fun addOverlay(texture: Int, rect: GLRect){
        graphProcess?.addTextureInfo(TextureInfo(Stream.STREAM_OVERLAY,texture, rect, false))
    }

    fun setTranstion(transtion: Transtion) {
        GlUtil.checkGlError("setTranstion")
        this.transtion = transtion
        drawHandler?.let {
            it.sendMessage(it.obtainMessage(TRANSTIONANIM))
        }
    }

    fun setRenderSize(width: Int, height: Int) {
        this.mRendeWidth = width
        this.mRenderHeight = height
    }

    fun setFps(fps: Int) {
        this.frameRate = fps
    }

    fun getFps(): Int {

        return frameRate
    }

    fun setSurface(surface: Any?) {
        this.mSurface = surface
    }

    fun getSurface(): Any? {
        return mSurface
    }

    fun getSurfaceTexture(): SurfaceTexture {
        return info!!.surfaceTexture!!
    }

    fun getTextureInfo(): TextureInfo? {
        return graphProcess?.getOutPutInfo()
    }

    fun setEncode(isEncode: Boolean) {
        this.isEncode = isEncode
    }

    companion object {
        private const val ADDSCENE = 0x11
        private const val DRAWSCENE = 0x12
        private const val SURFACEVALID = 0x13
        private const val TRANSTIONANIM = 0x14
    }
}