package com.play.window.render

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import com.play.window.render.gles.GlUtil

/**
 * User: maodayu
 * Date: 2023/4/13
 * Time: 19:19
 * 执行OpenGL操作
 */
open class TextureProgram(val vertex: String, val fragment: String) {

    protected var mProgram: Int = -1
    private var mPosition: Int = -1
    private var mTextCoord: Int = -1
    private var mMvpMatrix: Int = -1
    private var mainTexture: Int = -1


    private var textureId: Int = -1
    private var vertexArray: FloatArray = floatArrayOf()
    private var fragmentArray: FloatArray = floatArrayOf()
    private var mvpMatrix: FloatArray = floatArrayOf()

    init {
        initGl()
    }

    protected open fun initGl() {
        mProgram = GlUtil.createProgram(vertex, fragment)
        initBaseValue()
    }


    private fun initBaseValue() {
        mPosition = GLES20.glGetAttribLocation(mProgram, "aPosition")
        mTextCoord = GLES20.glGetAttribLocation(mProgram, "aTextCoord")
        mMvpMatrix = GLES20.glGetUniformLocation(mProgram, "aMvpMatrix")
        mainTexture = GLES20.glGetUniformLocation(mProgram, "uTexture1")
    }

    /**
     * 绘制
     */
    private fun draw(isOES: Boolean = true) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(mProgram)
        setVertexAttribPointer(mPosition, vertexArray)
        setVertexAttribPointer(mTextCoord, fragmentArray)
        setUniformMatrix4fv(mMvpMatrix, mvpMatrix)
        GlUtil.checkGlError("------ glUseProgram ------")
        onInitValue()
        GlUtil.checkGlError("------ onInitValue ------")
        if (textureId != GlUtil.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            if (isOES) {
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            } else {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            }
            setUniform1i(mainTexture, 0)
        }
        GlUtil.checkGlError("------ glActiveTexture ------")
        drawAfter()
        GlUtil.checkGlError("------ drawAfter ------")
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(mPosition)
        GLES20.glDisableVertexAttribArray(mTextCoord)
        GLES20.glUseProgram(0)
        GLES30.glDisable(GLES20.GL_BLEND)
        GlUtil.checkGlError("------ glDrawArrays ------")
    }

    open fun onInitValue() {

    }

    open fun drawAfter() {

    }


    /**
     * @param vertex 顶点坐标
     * @param fragment 纹理坐标
     *
     */
    fun render(vertex: FloatArray, fragment: FloatArray, textureId: Int, matrix: FloatArray, isOES: Boolean = true) {
        this.vertexArray = vertex
        this.fragmentArray = fragment
        this.textureId = textureId
        this.mvpMatrix = matrix
        draw(isOES)
    }


    fun setVertexAttribPointer(attrKey: Int, attrValue: FloatArray) {
        GLES20.glEnableVertexAttribArray(attrKey)
        val buffer = GlUtil.createFloatBuffer(attrValue)
        GLES20.glVertexAttribPointer(attrKey, GlUtil.PER2, GLES20.GL_FLOAT, false, 0, buffer)
    }

    fun setUniformMatrix4fv(uniformKey: Int, uniformValue: FloatArray) {
        val buffer = GlUtil.createFloatBuffer(uniformValue)
        GLES20.glUniformMatrix4fv(uniformKey, 1, false, buffer)
    }


    fun setUniform1i(uniformKey: Int, uniformValue: Int) {
        GLES20.glUniform1i(uniformKey, uniformValue)
    }

    fun setUniform1f(uniformKey: Int, uniformValue: Float) {
        GLES20.glUniform1f(uniformKey, uniformValue)
    }

    fun setUniform2iv(uniformKey: Int, uniformValue: IntArray) {
        GLES20.glUniform2iv(uniformKey, 1, uniformValue, 0)
    }

}

