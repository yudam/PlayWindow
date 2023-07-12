package com.play.window.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.play.window.R
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.TextureInfo
import com.play.window.render.process.OpenglUtil

/**
 * User: maodayu
 * Date: 2023/5/30
 * Time: 10:38
 */
class OutProcess {

    private var outPutTextureInfo: TextureInfo? = null

    private var mTextureProgram:TextureProgram? = null

    /**
     * 通过反转180度来解决FBO导致的图像翻转问题
     */
    private val outMatrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.rotateM(this, 0, 180f, 1f, 0f, 0f)
    }


    fun getOutPutTextureInfo(): TextureInfo?{
        return outPutTextureInfo
    }

    fun addTextureInfo(info: TextureInfo?) {
        outPutTextureInfo = info
    }

    fun draw(){
        outPutTextureInfo?.let {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glViewport(0, 0, it.rect.pw.toInt(), it.rect.ph.toInt())
            if (mTextureProgram == null) {
                mTextureProgram = TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                    GlUtil.readRawResourse(R.raw.simple_fragment_shader))
            }
            GLES20.glViewport(0, 0, it.rect.pw.toInt(), it.rect.ph.toInt())
            mTextureProgram?.render(OpenglUtil.parseVertexArray(it.rect), OpenglUtil.parseFragmentArray(it.rect), it.texture,
                outMatrix, it.isOES)
        }
    }

}