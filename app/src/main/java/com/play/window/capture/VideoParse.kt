package com.play.window.capture

import com.play.window.render.SurfaceParam

/**
 * User: maodayu
 * Date: 2023/4/14
 * Time: 10:51
 */
object VideoParse {

    private val videoPlayMap = mutableMapOf<String, SurfaceParam>()

    fun getSurfaceParam(url: String?): SurfaceParam? {
        if (url == null) return null
        return videoPlayMap[url]
    }

    fun addSurfaceParam(url: String, param: SurfaceParam) {
        videoPlayMap[url] = param
    }
}