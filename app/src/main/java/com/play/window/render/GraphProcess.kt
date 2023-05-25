package com.play.window.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.play.window.R
import com.play.window.model.GLRect
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.TextureInfo

/**
 * User: maodayu
 * Date: 2023/5/25
 * Time: 16:51
 */
class GraphProcess {

    private val textureList = mutableListOf<TextureInfo>()

    private val matrix = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
    }

    fun setRenderSize(width:Int,height:Int){

    }

    fun addTextureInfo(info: TextureInfo){
        textureList.add(info)
    }


    fun draw(){
        GLES20.glClearColor(0f,0f,0f,0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        textureList.forEach {
            if (it.mProgram == null) {
                it.mProgram =  if(it.isOES){
                    TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_oes_shader))
                } else {
                    TextureProgram(GlUtil.readRawResourse(R.raw.simple_vertex_shader),
                        GlUtil.readRawResourse(R.raw.simple_fragment_shader))
                }
            }
            GLES20.glViewport(0,0,it.rect.pw.toInt(),it.rect.ph.toInt())
            it.mProgram?.render(parseVertexArray(it.rect), parseFragmentArray(it.rect), it.texture,
                matrix,it.isOES)
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