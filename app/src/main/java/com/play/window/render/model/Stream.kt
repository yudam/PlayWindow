package com.play.window.render.model

/**
 * User: maodayu
 * Date: 2023/7/10
 * Time: 17:10
 */
class Stream(val stream_type: Int) {

    override fun toString(): String {
        return "stream_type : $stream_type"
    }

    companion object{

        private const val stream_left = 10

        private const val stream_right = 11

        private const val stream_preview = 12

        private const val stream_overlay = 13

        private const val stream_source = 14


        val STREAM_LEFT = Stream(stream_left)

        val STREAM_RIGHT = Stream(stream_right)

        val STREAM_SOURCE = Stream(stream_source)

        val STREAM_PREVIEW = Stream(stream_preview)

        val STREAM_OVERLAY = Stream(stream_overlay)

        val STREAM_EMPTY = Stream(0)
    }

}