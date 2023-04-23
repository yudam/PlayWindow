package com.play.window.render

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.play.window.WindowApp
import com.play.window.render.gles.GlUtil

/**
 * User: maodayu
 * Date: 2023/4/13
 * Time: 19:19
 * 执行OpenGL操作
 */
class TextureProgram(vertex: String, fragment: String) {

    private var mProgram: Int = -1
    private var mPosition: Int = -1
    private var mTextCoord: Int = -1
    private var mMvpMatrix: Int = -1
    private var mainTexture: Int = -1


    private var tetxureId: Int = -1
    private var vertexArray: FloatArray = floatArrayOf()
    private var fragmentArray: FloatArray = floatArrayOf()
    private var mvpMatrix: FloatArray = floatArrayOf()

    init {
        mProgram = GlUtil.createProgram(vertex, fragment)
        initBaseValue()
    }


    private fun initBaseValue() {
        mPosition = GLES20.glGetAttribLocation(mProgram, "aPosition")
        mTextCoord = GLES20.glGetAttribLocation(mProgram, "aTextCoord")
        mMvpMatrix = GLES20.glGetUniformLocation(mProgram, "aMvpMatrix")
        mainTexture = GLES20.glGetUniformLocation(mProgram, "uTexture1")
    }

    private fun draw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glUseProgram(mProgram)
        GlUtil.checkGlError("glUseProgram:")
        setVertexAttribPointer(mPosition, vertexArray)
        GlUtil.checkGlError("setVertexAttribPointer:")
        setVertexAttribPointer(mTextCoord,fragmentArray)
        GlUtil.checkGlError("setVertexAttribPointer:")
        setUniformMatrix4fv(mMvpMatrix,mvpMatrix)
        GlUtil.checkGlError("setUniformMatrix4fv:")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GlUtil.checkGlError("glActiveTexture:")
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tetxureId)
        GlUtil.checkGlError("glBindTexture:"+tetxureId)
        setUniform1i(mainTexture, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError("glDrawArrays:")


        GLES20.glDisableVertexAttribArray(mPosition)
        GlUtil.checkGlError("glDrawArrays mPosition:")
        GLES20.glDisableVertexAttribArray(mTextCoord)
        GlUtil.checkGlError("glDrawArrays mTextCoord:")
        GLES20.glUseProgram(0)
        GlUtil.checkGlError("glUseProgram0:")
    }


    /**
     * @param vertex 顶点坐标
     * @param fragment 纹理坐标
     *
     */
    fun render(vertex: FloatArray, fragment: FloatArray, textureId: Int, matrix: FloatArray) {

        Log.i(WindowApp.TAG, "vertext: "+vertex.toString())
        Log.i(WindowApp.TAG, "fragment: "+fragment.toString())
        this.vertexArray = vertex
        this.fragmentArray = fragment
        this.tetxureId = textureId
        this.mvpMatrix = matrix
        draw()
    }


    fun setVertex(array: FloatArray) {
        this.vertexArray = array
    }

    fun setFragment(array: FloatArray) {
        this.fragmentArray = array
    }

    fun setTextureid(textureId: Int) {
        this.tetxureId = textureId
    }

    fun setMatrix(matrix: FloatArray) {
        this.mvpMatrix = matrix
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

