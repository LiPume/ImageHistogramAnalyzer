package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import kotlin.math.roundToInt

/**
 * MVP 基础算法：单线程遍历 ARGB 像素并统计灰度频次。
 *
 * Alpha 通道不参与课程指定公式，透明像素仍按其解码后的 RGB 值统计。
 */
class BaselineHistogramCalculator(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
) {
    fun calculate(
        pixels: IntArray,
        cancellationCheck: () -> Unit = {},
    ): HistogramResult {
        require(pixels.isNotEmpty()) { "待分析像素不能为空" }

        val counts = IntArray(HistogramResult.GRAY_LEVELS)
        pixels.forEachIndexed { index, pixel ->
            // 大图每处理一块检查一次取消，避免新图片选择后旧任务继续占用 CPU。
            if (index % CANCELLATION_CHECK_INTERVAL == 0) cancellationCheck()

            val red = pixel ushr 16 and CHANNEL_MASK
            val green = pixel ushr 8 and CHANNEL_MASK
            val blue = pixel and CHANNEL_MASK
            val gray = (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT)
                .roundToInt()
                .coerceIn(0, CHANNEL_MASK)
            counts[gray]++
        }

        val maxCount = counts.maxOrNull() ?: 0
        return HistogramResult(
            counts = counts,
            normalizedHeights = normalizer.normalize(counts),
            pixelCount = pixels.size.toLong(),
            maxCount = maxCount,
        )
    }

    companion object {
        private const val RED_WEIGHT = 0.299
        private const val GREEN_WEIGHT = 0.587
        private const val BLUE_WEIGHT = 0.114
        private const val CHANNEL_MASK = 0xFF
        private const val CANCELLATION_CHECK_INTERVAL = 16_384
    }
}
