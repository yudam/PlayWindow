package com.play.window.render.model

/**
 * User: maodayu
 * Date: 2023/7/10
 * Time: 17:10
 */
class Stream(val stream_type: Int) {

    companion object{

        private const val stream_left = 0x11

        private const val stream_right = 0x12

        private const val stream_preview = 0x13

        private const val stream_overlay = 0x14


        val STREAM_LEFT = Stream(stream_left)

        val STREAM_RIGHT = Stream(stream_right)

        val STREAM_PREVIEW = Stream(stream_preview)

        val STREAM_OVERLAY = Stream(stream_overlay)
    }

}