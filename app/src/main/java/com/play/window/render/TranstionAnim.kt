package com.play.window.render

import android.opengl.GLES20
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
    private var animDuration: Long = 1000L

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
        startTime = System.currentTimeMillis()
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


    fun anim() {
        val process = (System.currentTimeMillis() - startTime) / animDuration.toFloat()
        if (process < 1) {
            if (mFBO1 != null && mFBO2 != null) {
                execute.drawFrame(mFBO1!!.textureId, mFBO2!!.textureId, process)
            }
        } else {
            isFinish = true
        }

    }
}