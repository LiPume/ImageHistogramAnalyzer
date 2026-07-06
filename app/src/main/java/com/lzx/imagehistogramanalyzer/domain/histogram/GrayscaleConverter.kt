package com.lzx.imagehistogramanalyzer.domain.histogram

import kotlin.math.roundToInt

/** 统一两种方案的灰度公式和取整规则，防止算法流程不同导致结果漂移。 */
object GrayscaleConverter {
    fun fromArgb(pixel: Int): Int {
        val red = pixel ushr 16 and CHANNEL_MASK
        val green = pixel ushr 8 and CHANNEL_MASK
        val blue = pixel and CHANNEL_MASK
        return fromRgb(red, green, blue)
    }

    /**
     * 整数定点快路径与课程公式数学等价；仅在精确落到 .5 的颜色上回退原浮点计算，
     * 保持与旧版 Double + roundToInt 的全部 RGB 输出完全一致。
     */
    internal fun fromRgb(red: Int, green: Int, blue: Int): Int {
        val weighted = red * RED_FIXED + green * GREEN_FIXED + blue * BLUE_FIXED
        val base = weighted / FIXED_SCALE
        val remainder = weighted - base * FIXED_SCALE

        if (remainder == HALF_SCALE) {
            return fromRgbFloatingPoint(red, green, blue)
        }
        return base + if (remainder > HALF_SCALE) 1 else 0
    }

    private fun fromRgbFloatingPoint(red: Int, green: Int, blue: Int): Int =
        (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT)
            .roundToInt()
            .coerceIn(0, CHANNEL_MASK)

    private const val RED_WEIGHT = 0.299
    private const val GREEN_WEIGHT = 0.587
    private const val BLUE_WEIGHT = 0.114
    private const val RED_FIXED = 299
    private const val GREEN_FIXED = 587
    private const val BLUE_FIXED = 114
    private const val FIXED_SCALE = 1_000
    private const val HALF_SCALE = FIXED_SCALE / 2
    private const val CHANNEL_MASK = 0xFF
}
