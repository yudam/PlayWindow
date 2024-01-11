package com.play.window.model

import android.graphics.SurfaceTexture

/**
 * User: maodayu
 * Date: 2023/6/30
 * Time: 15:32
 */
class PreviewInfo(val surfaceTexture: SurfaceTexture,
                  val glRect: GLRect) {

    var previewSurfaceId: Int = 0

    val currSurfaceId:Int = 0

}
