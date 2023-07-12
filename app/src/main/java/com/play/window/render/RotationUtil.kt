package com.play.window.render

/**
 * User: maodayu
 * Date: 2023/6/21
 * Time: 11:15
 * 用于设置角度和翻转，获取输出的纹理坐标
 */
object RotationUtil {

    /**
     *  纹理的坐标以左上角为原点
     */
    private val TEXTURE_NO_ROTATION = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )

    private val TEXTURE_ROTATION_90 = floatArrayOf(
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 0.0f
    )

    private val TEXTURE_ROTATION_180 = floatArrayOf(
        1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    )

    private val TEXTURE_ROTATION_270 = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    /**
     * 根据旋转角度、翻转获取纹理坐标
     */
    fun getRotation(rotation: Int, flipHorizontal: Boolean, flipVertical: Boolean): FloatArray {
        var rotationArray = when (rotation) {
            0 -> TEXTURE_NO_ROTATION
            90 -> TEXTURE_ROTATION_90
            180 -> TEXTURE_ROTATION_180
            else -> TEXTURE_ROTATION_270
        }

        if(flipHorizontal){
            rotationArray = floatArrayOf(
                flip(rotationArray[0]),rotationArray[1],
                flip(rotationArray[2]),rotationArray[3],
                flip(rotationArray[4]),rotationArray[5],
                flip(rotationArray[6]),rotationArray[7]
            )
        }

        if(flipVertical){
            rotationArray = floatArrayOf(
                rotationArray[0], flip(rotationArray[1]),
                rotationArray[2], flip(rotationArray[3]),
                rotationArray[4], flip(rotationArray[5]),
                rotationArray[6], flip(rotationArray[7]),
            )
        }

        return rotationArray
    }


    /**
     * 水平翻转 ： 将x轴坐标 0变成1  1变成0
     * 垂直翻转 ： 将y轴坐标 0变成1  1变成0
     */
    private fun flip(i: Float): Float {

        if (i == 0.0f) {
            return 1.0f
        }
        return 0.0f
    }


}