package com.lzx.imagehistogramanalyzer.domain.histogram

import kotlin.math.roundToInt

/** 统一两种方案的灰度公式和取整规则，防止算法流程不同导致结果漂移。 */
object GrayscaleConverter {
    fun fromArgb(pixel: Int): Int {
        val red = pixel ushr 16 and CHANNEL_MASK
        val green = pixel ushr 8 and CHANNEL_MASK
        val blue = pixel and CHANNEL_MASK
        return (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT)
            .roundToInt()
            .coerceIn(0, CHANNEL_MASK)
    }

    private const val RED_WEIGHT = 0.299
    private const val GREEN_WEIGHT = 0.587
    private const val BLUE_WEIGHT = 0.114
    private const val CHANNEL_MASK = 0xFF
}
