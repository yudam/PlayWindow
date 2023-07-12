package com.play.window.render

import android.opengl.GLES20
import com.play.window.render.gles.GlUtil

/**
 * User: maodayu
 * Date: 2023/6/25
 * Time: 15:55
 * 转场动画
 */
class TransitionProgram(vertex: String, fragment: String) : TextureProgram(vertex, fragment) {

    private var progress: Int = -1
    private var direction: Int = -1
    private var mTexture: Int = -1

    private var texture2Id: Int = -1
    private var progressValue: Float = 1f
    private var directionArray: IntArray = intArrayOf(1, 1)

    override fun initGl() {
        super.initGl()
        progress = GLES20.glGetUniformLocation(mProgram, "progress")
        direction = GLES20.glGetUniformLocation(mProgram, "direction")
        mTexture = GLES20.glGetUniformLocation(mProgram, "uTexture2")

    }


    override fun onInitValue() {
        setUniform1f(progress, progressValue)
        setUniform2iv(direction, directionArray)
    }

    override fun drawAfter() {
        if (texture2Id != GlUtil.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2Id)
            setUniform1i(mTexture, 1)
        }
    }

    fun setTexture2(texture: Int) {
        texture2Id = texture
    }

    fun setProgress(progress: Float) {
        progressValue = progress
    }

    fun setDirection(direction: IntArray) {
        directionArray = direction
    }
}