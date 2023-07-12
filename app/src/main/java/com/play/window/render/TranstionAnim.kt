package com.play.window.render

import android.opengl.GLES20
import android.util.Log
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.GlFrameBuffer

/**
 * User: maodayu
 * Date: 2023/5/26
 * Time: 15:42
 * 转场动画
 */
class TranstionAnim {


    private var startTime: Long = 0L
    private var animDuration: Long = 3000L

    private var mRenderWidth: Int = 0
    private var mRenderHeight: Int = 0

    private var mFBO1: GlFrameBuffer? = null
    private var mFBO2: GlFrameBuffer? = null

    private var isFinish: Boolean = false

    private val execute = TransitionExecute()

    fun setRenderSize(width: Int, height: Int) {
        mRenderWidth = width
        mRenderHeight = height
    }

    fun initTranstion() {
        isFinish = false
        startTime = System.currentTimeMillis()

        Log.i("MDY", "initTranstion: "+Thread.currentThread().name)
        GlUtil.checkGlError("initTranstion")
        mFBO1 = GlUtil.prepareFrameBuffer(mRenderWidth, mRenderHeight)
        mFBO2 = GlUtil.prepareFrameBuffer(mRenderWidth, mRenderHeight)
    }

    fun openFbo1() {
        mFBO1?.let {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, it.frameBufferId)
        }
    }

    fun openFbo2() {
        mFBO2?.let {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, it.frameBufferId)
        }
    }

    fun isFinish() = isFinish

    fun anim() {
        val process = (System.currentTimeMillis() - startTime) / animDuration.toFloat()

        Log.i("MDY", "process: "+process)
        if (process < 1) {
            if (mFBO1 != null && mFBO2 != null) {
                GLES20.glViewport(0,0,mRenderWidth,mRenderHeight)
                execute.drawFrame(mFBO1!!.textureId, mFBO2!!.textureId, process)
            }
        } else {
            isFinish = true
        }
    }


}