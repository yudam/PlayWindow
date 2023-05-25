package com.play.window.capture

import android.graphics.SurfaceTexture
import android.view.SurfaceHolder

/**
 * User: maodayu
 * Date: 2023/5/19
 * Time: 09:59
 */
interface ICamera {

    fun startPublish()

    fun openCamera(surface:SurfaceTexture)

    fun openCamera(surface:SurfaceHolder)

    fun closeCamera()
}