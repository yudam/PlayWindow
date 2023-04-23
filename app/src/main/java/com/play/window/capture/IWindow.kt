package com.play.window.capture

import android.graphics.SurfaceTexture
import android.view.Surface
import com.play.window.model.DisplayInfo
import com.play.window.model.GLRect

/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 11:54
 */
interface IWindow {

    fun playVideo(info: DisplayInfo)

    fun playVideo(url: String,surface: Surface)

    fun release()
}