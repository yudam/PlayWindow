package com.play.window.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.play.window.R
import com.play.window.render.gles.GlUtil
import com.play.window.render.model.TextureInfo
import com.play.window.render.process.OpenglUtil
import java.util.concurrent.locks.ReentrantLock

/**
 * User: maodayu
 * Date: 2023/5/25
 * Time: 16:51
 */
class GraphProcess(val shareLock: ReentrantLock) {

    private var outPutInfo: TextureInfo? = null

    private var unitProcess: UnitProcess? = null

    private var outProcess: OutProcess? = null

    private var useFBO = true

    fun setFBO(isFBO: Boolean) {
        useFBO = isFBO
    }

    fun setRenderSize(width: Int, height: Int) {
        if (unitProcess == null) {
            unitProcess = UnitProcess()
        }
        unitProcess?.setRenderSize(width, height)
    }

    fun setSurfaceSize(width: Int,height: Int){

    }

    fun addTextureInfo(info: TextureInfo) {
        if(useFBO){
            if (unitProcess == null) {
                unitProcess = UnitProcess()
            }
            unitProcess?.addTextureInfo(info)
        } else {
            outPutInfo = info
        }
    }

    fun updateTextureInfo(info: TextureInfo){
        if (unitProcess == null) {
            unitProcess = UnitProcess()
        }
        unitProcess?.updateTextureInfo(info)
    }


    fun getOutPutInfo(): TextureInfo? {
        return outPutInfo
    }

    fun getOutProcess(): OutProcess? {
        return outProcess
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
            if (outProcess == null) {
                outProcess = OutProcess()
            }
            outProcess?.addTextureInfo(outPutInfo)

            // 输出纹理
            outProcess?.draw()
        } finally {
            shareLock.unlock()
        }
    }
}