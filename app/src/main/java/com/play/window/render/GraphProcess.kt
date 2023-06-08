package com.play.window.render

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.play.window.R
import com.play.window.model.GLRect
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.GlFrameBuffer
import com.play.window.render.model.TextureInfo
import com.play.window.temp.GlUtils
import java.util.concurrent.locks.ReentrantLock

/**
 * User: maodayu
 * Date: 2023/5/25
 * Time: 16:51
 */
class GraphProcess(val shareLock: ReentrantLock) {

    private var outPutInfo: TextureInfo? = null

    private var unitProcess: UnitProcess? = null

    private var useFBO = true

    fun setFBO(isFBO: Boolean) {
        useFBO = isFBO
    }

    /**
     * 通过反转180度来解决FBO导致的图像翻转问题
     */
    private val outMatrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.rotateM(this, 0, 180f, 0f, 0f, 1f)
    }

    fun setRenderSize(width: Int, height: Int) {
        if (unitProcess == null) {
            unitProcess = UnitProcess()
        }
        unitProcess?.setRenderSize(width, height)
    }

    fun addTextureInfo(info: TextureInfo) {
        if (useFBO) {
            if (unitProcess == null) {
                unitProcess = UnitProcess()
            }
            unitProcess?.addTextureInfo(info)
        } else {
            outPutInfo = info
        }
    }


    fun getOutPutInfo(): TextureInfo? {
        return outPutInfo
    }

    fun draw() {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        /**
         * 这里的锁是为了防止纹理的读写出现的安全问题
         */
        shareLock.lock()

        try {

            if (useFBO) {
                /**
                 * 在FBO中绘制纹理
                 */
                unitProcess?.draw()

                outPutInfo = unitProcess?.getOutPutTextureInfo()
            }

            /**
             * 输出纹理绘制
             */
            outPutInfo?.let {
                if (it.mProgram == null) {
                    it.mProgram = TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_fragment_shader))
                }
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(0, 0, it.rect.pw.toInt(), it.rect.ph.toInt())
                it.mProgram?.render(parseVertexArray(it.rect), parseFragmentArray(it.rect), it.texture,
                    outMatrix, it.isOES)
            }
        } finally {
            shareLock.unlock()
        }
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
}