package com.play.window.temp

import android.graphics.Bitmap
import android.opengl.GLES10
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.play.window.WindowApp
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder

/**
 * User: maodayu
 * Date: 2023/1/5
 * Time: 19:58
 */
object GlUtils {

    /**
     * 加载着色器
     */
    fun loadShaders(type: Int, glsl: String): Int {
        val shader = GLES20.glCreateShader(type)
        checkGlError("loadShaders-type")
        GLES20.glShaderSource(shader, glsl)
        GLES20.glCompileShader(shader)
        checkGlError("loadShaders-Compile")
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("MDY", "loadShaders failed: " + GLES20.glGetShaderInfoLog(shader))
        }
        checkGlError("loadShaders")
        return shader
    }


    /**
     * 创建工程
     */
    fun getProgram(vertextShader: String, fragmentShader: String): Int {
        val programId = GLES20.glCreateProgram()
        checkGlError("glCreateProgram")
        val vertexId = loadShaders(GLES20.GL_VERTEX_SHADER, vertextShader)
        val fragmentId = loadShaders(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        Log.i("glError", "vertexId: " + vertexId + "   fragmentId:" + fragmentId)
        GLES20.glAttachShader(programId, vertexId)
        GLES20.glAttachShader(programId, fragmentId)
        GLES20.glLinkProgram(programId)
        checkGlError("glLinkProgram")
        val linked = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            Log.i("MDY", "loadProgram: link failed")
        }
        checkGlError("getProgram")
        return programId
    }


    /**
     * 创建纹理ID
     */
    fun getTexture(isOES: Boolean = false): Int {
        return getTexture(null, isOES)
    }

    /**
     * glTexParameteri：表示对纹理的设置
     *
     * 当纹理大小和渲染的屏幕大小不一致时处理办法,
     * @see GLES20.GL_TEXTURE_MIN_FILTER 纹理大于屏幕时
     * @see GLES20.GL_TEXTURE_MAG_FILTER 纹理小于屏幕时
     *
     * @see GLES20.GL_LINEAR 使用纹理中最接近的一个像素的颜色作为需要绘制的像素颜色
     * @see GLES20.GL_NEAREST 使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色。

     * 纹理坐标系被称为ST坐标系，S对应x，T对应y，GL_TEXTURE_WRAP_S和GL_TEXTURE_WRAP_T表示超出范围的纹理的处理方式
     * @see GLES20.GL_CLAMP_TO_EDGE 采样纹理边缘，通过边缘补充剩余部分
     * @see GLES20.GL_REPEAT 重复纹理
     * @see GLES20.GL_MIRRORED_REPEAT 镜像重复
     */
    fun getTexture(bitmap: Bitmap? = null, isOES: Boolean = false): Int {
        val intArray = IntArray(1)
        GLES20.glGenTextures(1, intArray, 0)
        if (isOES) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, intArray[0])
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE)
            bitmap?.let {
                GLUtils.texImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, it, 0)
            }
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, intArray[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE)
            bitmap?.let {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, it, 0)
            }
        }
        checkGlError("getTexture")
        return intArray[0]
    }

    fun getFboTextureId(isOES:Boolean,width:Int,height:Int):Int{
        checkGlError("getFboTextureId")
        val intArray = IntArray(1)
        GLES20.glGenTextures(1, intArray, 0)
        checkGlError("glGenTextures")
        if (isOES) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, intArray[0])
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, intArray[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null)
        }
        checkGlError("getTexture")
        return intArray[0]
    }


    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error)
            Log.e("MDY", msg)
            throw RuntimeException(msg)
        }
    }


    /**
     *
     */
    fun readRawResourse(id: Int): String {
        val inputStream = WindowApp.instance().resources.openRawResource(id)
        val reader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(reader)
        val builder = StringBuilder()
        try {
            var nextLine: String?
            while (bufferedReader.readLine().also { nextLine = it } != null) {
                builder.append(nextLine)
                builder.append("\n")
            }
        } catch (e: Exception) {
            Log.i("MDY", "readRawResourse: failed")
        } finally {
            bufferedReader.close()
        }
        return builder.toString()
    }

    /**
     * 获取MVP矩阵，对顶点坐标的范围进行计算，得出合适的范围，然后纹理贴上去才不会拉伸
     * mvp=projection⋅view⋅model⋅local
     * 初始坐标位于左下角
     *
     *
     * Model矩阵：通过平移、旋转、缩放来设置模型的位置，可以认为是对应纹理的位置和宽高，主要防止图片变形
     * 投影矩阵：设置将Model矩阵的那一部分映射到[-1,1]的区间上，这里的left，top，right，bottom
     * 指的是Model矩阵中设置的相关位置，投影矩阵原点位于左下角。
     */
    fun getModelMatrix(width: Int, height: Int): FloatArray {

        Log.i("MDY", "width: $width  height:$height  ")

        val modelMatrix = FloatArray(16).apply {
            Matrix.setIdentityM(this, 0)
        }
        Matrix.translateM(modelMatrix, 0, width.toFloat() / 2, height.toFloat() / 2, 0f)
        // x和y缩放两倍
        Matrix.scaleM(modelMatrix, 0, width.toFloat()/2, height.toFloat()/2, 1f)

//        //创建投影矩阵，
//        val projectMatrix = FloatArray(16)
//        Matrix.orthoM(projectMatrix, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)
//
//        // 计算MVP矩阵，projectMatrix和modelMatrix的顺序要注意
//        val mvpMatrix = FloatArray(16)
//        Matrix.multiplyMM(mvpMatrix, 0, projectMatrix, 0, modelMatrix, 0)
//
//        Log.i("MDY", "mvpMatrix: " + mvpMatrix.toList().toString())
        return modelMatrix
    }


    /**
     * 模型矩阵的缩放有点意思，放大width，height，其实是方法width*2，height*2
     *
     */
}