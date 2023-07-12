package com.play.window.render.process

import com.play.window.model.GLRect

/**
 * User: maodayu
 * Date: 2023/6/25
 * Time: 16:34
 */
object OpenglUtil {

    /**
     *  顶点坐标的范围是[-1,1],GLRect中是按照[0,1]来设置的，所以需要转换到[-1,1]
     *
     */
    fun parseVertexArray(rect: GLRect): FloatArray {

        val cx = (rect.cx / rect.pw) * 2 - 1
        val cy = (rect.cy / rect.ph) * 2 - 1
        val w = (rect.width / rect.pw) * 2
        val h = (rect.height / rect.ph) * 2
        val vertex = floatArrayOf(
            cx - w / 2, cy - h / 2,      // bottom left
            cx + w / 2, cy - h / 2,      // bottom right
            cx - w / 2, cy + h / 2,      // top left
            cx + w / 2, cy + h / 2       // top right
        )
        return vertex
    }

    /**
     * 纹理的坐标以左下角为标准，图像的存储从左上角开始，所以图像的左上角像素生成的纹理部分就映射在左下角，
     * 所以整个图像就是上下颠倒的。
     *
     * 两个方法来解决：
     * 1. 纹理的坐标进行翻转，以左上角为原点来计算坐标
     * 2. 以左下角为原点来计算坐标，映射到顶点坐标时，通过Matrix来进行旋转操作，也就是左上对左下，右上对右下
     *
     *  顶点坐标和纹理坐标按照反的Z字形来设置：p0 p1 p2 p3
     *
     *  p2         p3
     *  ***********
     *          *
     *        *
     *      *
     *    *
     *  ***********
     *  p0        p1
     *
     *  这里的纹理坐标按照左下角为原点设置，所以需要通过Matrix对坐标做X轴翻转180度才能的到最终的纹理坐标
     */
    fun parseFragmentArray(rect: GLRect): FloatArray {
        val cx = rect.cx / rect.pw
        val cy = rect.cy / rect.ph
        val w = rect.width / rect.pw
        val h = rect.height / rect.ph

        val fragment = floatArrayOf(
            cx - w / 2, cy - h / 2,    // bottom left
            cx + w / 2, cy - h / 2,    // bottom right
            cx - w / 2, cy + h / 2,    // top left
            cx + w / 2, cy + h / 2     // top right
        )
        return fragment
    }

    /**
     * 根据视频的宽高计算缩放后的宽高
     */
    fun scaleVideoRect(glRect: GLRect, videoWidth: Int, videoHeight: Int): GLRect {

        val videoRatio = videoHeight.toFloat() / videoWidth.toFloat()

        if (glRect.ph > glRect.pw * videoRatio) {

            glRect.width = glRect.pw
            glRect.height = glRect.pw * videoRatio

        } else {
            glRect.width = glRect.ph / videoRatio
            glRect.height = glRect.ph

        }
        return glRect
    }
}