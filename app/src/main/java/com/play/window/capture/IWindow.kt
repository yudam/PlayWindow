package com.play.window.capture

import android.graphics.Bitmap
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

    fun startPublish()

    fun stopPublish()

    fun startRecord(path:String)

    fun stopRecord()

    fun playVideo(surfaceTexture: SurfaceTexture):Int

    fun playVideo(info: DisplayInfo):Int

    fun addBitmap(bitmap: Bitmap, rect: GLRect,surfaceId:Int): Int

    fun updateBitmap(sceneId: Int, bitmap: Bitmap, rect: GLRect,surfaceId:Int): Int

    fun removeBitmap(sceneId: Int,surfaceId:Int)

    fun release()
}