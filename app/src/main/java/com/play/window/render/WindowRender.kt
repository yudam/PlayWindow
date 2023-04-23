package com.play.window.render

import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.play.window.R
import com.play.window.WindowApp
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect
import com.play.window.render.gles.EglCore
import com.play.window.render.gles.EglCore2
import com.play.window.render.gles.GlUtil

/**
 * User: maodayu
 * Date: 2023/4/13
 * Time: 19:16
 *
 */
class WindowRender(val sharedContext: EGLContext?) {


    fun addDisplaySurface(info: DisplayInfo) {
        val drawSurface = DrawSurface(sharedContext)
        drawSurface.addDisplaySurface(info)
    }
}