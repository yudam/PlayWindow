package com.play.window.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.play.window.R
import com.play.window.render.gles.GlUtil
import com.play.window.temp.GLDrawableUtils

/**
 * User: maodayu
 * Date: 2023/5/26
 * Time: 16:32
 */
class TransitionExecute {

    private val animProgram: Int

    private var mProgram: Int = -1
    private var mPosition: Int = -1
    private var mTextCoord: Int = -1
    private var mMvpMatrix: Int = -1

    private var mTexture: Int = -1
    private var mTexture2: Int = -1
    private var mProgress: Int = -1
    private var mDirection: Int = -1

    private val vertexArray: FloatArray
    private val fragmentArray: FloatArray

    private val mvpMatrix = FloatArray(16)

    init {
        animProgram = GlUtil.createProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
            GlUtil.readRawResourse(R.raw.squeeze_fragment))
        vertexArray = GLDrawableUtils.common_vertext_coord
        fragmentArray = GLDrawableUtils.common_fragment_coord
        Matrix.setIdentityM(mvpMatrix,0)
        initValue()
    }


    fun drawFrame(textureId1: Int, textureId2: Int, process: Float) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(animProgram)
        GlUtil.checkGlError("glUseProgram")

        setVertexAttribPointer(mPosition, vertexArray)
        setVertexAttribPointer(mTextCoord, fragmentArray)
        setUniformMatrix4fv(mMvpMatrix,mvpMatrix)
        setUniform1f(mProgress,process)
        GlUtil.checkGlError("setVertexAttribPointer")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId1)
        setUniform1i(mTexture,0)
        GlUtil.checkGlError("glActiveTexture 1")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId2)
        setUniform1i(mTexture,1)
        GlUtil.checkGlError("glActiveTexture 2")

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError("glDrawArrays")

        GLES20.glDisableVertexAttribArray(mPosition)
        GLES20.glDisableVertexAttribArray(mTextCoord)
        GLES20.glUseProgram(0)
        GlUtil.checkGlError("glDisableVertexAttribArray")
    }


    private fun initValue() {
        mPosition = GLES20.glGetAttribLocation(mProgram, "aPosition")
        mTextCoord = GLES20.glGetAttribLocation(mProgram, "aTextCoord")
        mMvpMatrix = GLES20.glGetUniformLocation(mProgram, "aMvpMatrix")

        mTexture = GLES20.glGetUniformLocation(mProgram, "uTexture")
        mTexture2 = GLES20.glGetUniformLocation(mProgram, "uTexture2")
        mProgress = GLES20.glGetUniformLocation(mProgram, "progress")
        mDirection = GLES20.glGetUniformLocation(mProgram, "direction")
    }

    private fun setVertexAttribPointer(attrKey: Int, attrValue: FloatArray) {
        GLES20.glEnableVertexAttribArray(attrKey)
        val buffer = GlUtil.createFloatBuffer(attrValue)
        GLES20.glVertexAttribPointer(attrKey, GlUtil.PER2, GLES20.GL_FLOAT, false, 0, buffer)
    }

    private fun setUniformMatrix4fv(uniformKey: Int, uniformValue: FloatArray) {
        val buffer = GlUtil.createFloatBuffer(uniformValue)
        GLES20.glUniformMatrix4fv(uniformKey, 1, false, buffer)
    }


    private fun setUniform1i(uniformKey: Int, uniformValue: Int) {
        GLES20.glUniform1i(uniformKey, uniformValue)
    }

    private fun setUniform1f(uniformKey: Int, uniformValue: Float) {
        GLES20.glUniform1f(uniformKey, uniformValue)
    }
}