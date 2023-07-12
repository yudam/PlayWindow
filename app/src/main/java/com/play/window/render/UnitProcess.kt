package com.play.window.render

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.play.window.R
import com.play.window.model.GLRect
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.GlFrameBuffer
import com.play.window.render.model.Stream
import com.play.window.render.model.TextureInfo
import com.play.window.render.process.OpenglUtil

/**
 * User: maodayu
 * Date: 2023/6/6
 * Time: 19:34
 * 用于绘制出最终渲染的纹理，交给上层使用
 */
class UnitProcess {

    private var glFrameBuffer: GlFrameBuffer? = null

    private var outPutTextureInfo: TextureInfo? = null

    private val textureList = mutableListOf<TextureInfo>()

    private val matrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }


    fun getOutPutTextureInfo():TextureInfo?{
        return outPutTextureInfo
    }

    fun addTextureInfo(info: TextureInfo) {
        textureList.add(info)
    }

    fun updateTextureInfo(info: TextureInfo){
        textureList.forEach {
            if(it.stream == info.stream){
                it.texture = info.texture
                it.rect = info.rect
                it.isOES = info.isOES
            }
        }
    }

    /**
     * 外层必须优先调用该方法设置渲染范围，生成FBO
     */
    fun setRenderSize(width: Int, height: Int) {
        initOutPut(width,height)
    }

    private fun initOutPut(width: Int, height: Int) {
        glFrameBuffer = GlUtil.prepareFrameBuffer(width, height)
        val rect = GLRect(width / 2f, height / 2f, width.toFloat(), height.toFloat(), width.toFloat(), height.toFloat())
        outPutTextureInfo = TextureInfo(Stream.STREAM_PREVIEW,glFrameBuffer!!.textureId, rect, false)
    }


    fun draw(){
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if(glFrameBuffer == null) return

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, glFrameBuffer!!.frameBufferId)

        textureList.forEach {
            if (it.mProgram == null) {
                it.mProgram = if (it.isOES) {
                    TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_oes_shader))
                } else {
                    TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_fragment_shader))
                }
            }
            GLES20.glViewport(0, 0, it.rect.pw.toInt(), it.rect.ph.toInt())
            it.mProgram?.render(OpenglUtil.parseVertexArray(it.rect), OpenglUtil.parseFragmentArray(it.rect), it.texture,
                matrix, it.isOES)
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }
}