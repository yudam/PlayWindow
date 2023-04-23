package com.play.window.temp

import android.graphics.SurfaceTexture
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import com.play.window.R
import com.play.window.render.gles.EglCore

/**
 * 画中画的实现方式
 *
 * 1. SurfaceTexture的updateTexImage()必须在创建了EGLContext上下文的线程中调用，否则直接崩溃
 * 2. SurfaceTexture(sceneId)中纹理ID必须是OES纹理，否则在FBO中会一直抛出没有附着的纹理图像
 * 3. 模型矩阵必须初始化为单位矩阵，不然计算有问题，导致无画面黑屏
 *
 * 4. FBO的创建和Program的创建可能导致缓慢的内存泄漏，FBO使用完毕要及时删除，Program的创建可以复用
 */
class PipScene(val surface: SurfaceTexture) : HandlerThread("PipScene") {

    private var mEglCore: EglCore? = null
    private var mEGLSurface: EGLSurface? = null
    private var mHandler: Handler? = null

    private var sceneId: Int = -1
    private var fboSceneId:Int = -1

    private var glPosition: Int = -1
    private var glTexturePosition = -1
    private var glTextureUnit = -1

    private var vWidth: Int = -1
    private var vHeight: Int = -1

    private var mMainMatrix = FloatArray(16)
    private var mSubMatrix = FloatArray(16)

    private var mSurfaceTexture: SurfaceTexture? = null

    private val lock = Object()


    fun setVideoSize(width: Int, height: Int) {
        vWidth = width
        vHeight = height
    }

    fun getSurfaceTexture(): SurfaceTexture? {

        synchronized(lock) {
            if (mSurfaceTexture == null) {
                lock.wait()
            }
        }
        return mSurfaceTexture
    }

    fun release(){
        mHandler?.removeCallbacksAndMessages(null)

        mSurfaceTexture?.let {
           it.setOnFrameAvailableListener(null)
            it.release()
        }
        mEGLSurface?.let {
            mEglCore?.releaseSurface(it)
            mEglCore?.release()
        }

    }


    override fun onLooperPrepared() {
        mEglCore = EglCore()
        mEGLSurface = mEglCore?.createWindowSurface(surface)
        mEglCore?.makeCurrent(mEGLSurface!!)
        mHandler = Handler(looper) {
           // mSurfaceTexture?.updateTexImage()
           // drawScene()
            true
        }

        synchronized(lock) {
            sceneId = GlUtils.getTexture(true)
            mSurfaceTexture = SurfaceTexture(sceneId)
            mSurfaceTexture?.setOnFrameAvailableListener {
                mHandler!!.sendEmptyMessage(0x11)
            }
            lock.notifyAll()
        }

        mMainMatrix = getMainMatrix()
        mSubMatrix = getMatrix()
    }


    private fun drawScene() {
        // 清除缓存
        GlUtils.checkGlError("drawScene0")
        GLES30.glViewport(0, 0, vWidth, vHeight)
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GlUtils.checkGlError("drawScene1")

        // 启用混合模式，否则画中画只会有第二个纹理生效
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GlUtils.checkGlError("glBlendFunc")
        // 绘制FBO
        drawSubScene()
        drawMainScene(sceneId, true)
        drawMainScene(fboSceneId, false)

        GLES30.glDisable(GLES30.GL_BLEND)
        mEglCore?.swapBuffers(mEGLSurface!!)
    }


    private var mainProgram:Int = -1
    private var fboProgram:Int = -1


    private fun getProgram(isOES: Boolean):Int{
        if(isOES){
            if(mainProgram != -1) return mainProgram
        } else {
            if(fboProgram != -1) return fboProgram
        }
        val vertext = GlUtils.readRawResourse(R.raw.simple_vertex_shader)
        return if (isOES) {
            val fragment = GlUtils.readRawResourse(R.raw.simple_oes_shader)
            mainProgram = GlUtils.getProgram(vertext, fragment)
            mainProgram
        } else {
            val fragment = GlUtils.readRawResourse(R.raw.simple_fragment_shader)
            fboProgram = GlUtils.getProgram(vertext, fragment)
            fboProgram
        }
    }

    /**
     * 绘制，区分OES纹理和2D纹理
     */
    private fun drawMainScene(textureId: Int, isOES: Boolean,isFbo:Boolean = false) {
        val program = getProgram(isOES)
        loadAttrs(program)
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(glPosition)
        GLES20.glEnableVertexAttribArray(glTexturePosition)
        val vertextCoord  = GLDrawableUtils.getByteBuffer(GLDrawableUtils.common_vertext_coord)
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, vertextCoord)
        val frameBuffer = if(isFbo){
            GLDrawableUtils.getByteBuffer(GLDrawableUtils.common_fbo_fragment_coord)
        } else {
            GLDrawableUtils.getByteBuffer(GLDrawableUtils.common_fragment_coord)
        }
        GLES20.glVertexAttribPointer(glTexturePosition, 2, GLES20.GL_FLOAT, false, 0, frameBuffer)

        val mvpMatrix = GLES20.glGetUniformLocation(program, "aMvpMatrix")

        if(isFbo){
            GLES20.glUniformMatrix4fv(mvpMatrix, 1, false, mSubMatrix, 0)
        } else {
            GLES20.glUniformMatrix4fv(mvpMatrix, 1, false, mMainMatrix, 0)
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        if (isOES) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }
        GLES20.glUniform1i(glTextureUnit, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(glPosition)
        GLES20.glDisableVertexAttribArray(glTexturePosition)
        if (isOES) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        GLES20.glUseProgram(0)
    }

    private fun drawSubScene() {
        // 创建FBO并绑定
        if(fboSceneId == -1){
            fboSceneId = GlUtils.getFboTextureId(false, vWidth, vHeight)
        }
        val frameBuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffers, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, fboSceneId, 0)
        // 开始绘制FBO
        drawMainScene(sceneId, isOES = true, isFbo = true)
        // 切换到默认的帧缓存
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDeleteFramebuffers(1, frameBuffers,0)
    }

    private fun loadAttrs(mProgramId: Int) {
        glPosition = GLES20.glGetAttribLocation(mProgramId, "aPosition")
        glTexturePosition = GLES20.glGetAttribLocation(mProgramId, "aTextCoord")
        glTextureUnit = GLES20.glGetUniformLocation(mProgramId, "uTexture1")
    }


    private fun getMatrix(): FloatArray {
        // 模型矩阵注意初始化为单位矩阵
        val modelMatrix = FloatArray(16).apply {
            Matrix.setIdentityM(this, 0)
        }
        val cx = vWidth.toFloat() - vWidth.toFloat()/4
        val cy = vHeight.toFloat() - vHeight.toFloat()/4
        Matrix.translateM(modelMatrix, 0, cx, cy, 0f)
        Matrix.scaleM(modelMatrix, 0, vWidth.toFloat()/2, vHeight.toFloat()/2, 1f)

        val projectMatrix = FloatArray(16)
        Matrix.orthoM(projectMatrix, 0, 0f, vWidth.toFloat(), 0f, vHeight.toFloat(), -1f, 1f)

        val mvpMatrix = FloatArray(16)

        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, modelMatrix, 0)
        return mvpMatrix
    }

    private fun getMainMatrix(): FloatArray {
        val modelMatrix = FloatArray(16).apply {
            Matrix.setIdentityM(this, 0)
        }
        Matrix.translateM(modelMatrix, 0, vWidth.toFloat() / 2, vHeight.toFloat() / 2, 0f)
        Matrix.scaleM(modelMatrix, 0, vWidth.toFloat(), vWidth.toFloat(), 1f)

        val projectMatrix = FloatArray(16)
        Matrix.orthoM(projectMatrix, 0, 0f, vWidth.toFloat(), 0f, vHeight.toFloat(), -1f, 1f)

        val mvpMatrix = FloatArray(16)

        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, modelMatrix, 0)
        return mvpMatrix
    }
}