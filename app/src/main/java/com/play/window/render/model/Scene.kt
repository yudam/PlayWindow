package com.play.window.render.model

import android.graphics.Bitmap
import android.graphics.Rect
import com.play.window.model.GLRect

/**
 * User: maodayu
 * Date: 2023/4/25
 * Time: 14:16
 */

open class Scene(){

    var surfaceId:Int = -1
}

data class BitmapScene(val bitmap: Bitmap,val rect: GLRect):Scene()
