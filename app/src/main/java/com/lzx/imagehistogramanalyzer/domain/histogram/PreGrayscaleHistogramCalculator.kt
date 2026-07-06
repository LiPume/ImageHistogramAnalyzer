package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

/**
 * 方案一：先把全部彩色像素转换成灰度数组，再单独统计灰度频次。
 *
 * 该方案逻辑直观，但需要额外的灰度数组和第二次遍历。
 */
class PreGrayscaleHistogramCalculator(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
) : HistogramCalculator {
    override val strategy = HistogramCalculationStrategy.PRE_GRAYSCALE

    override fun calculate(
        pixels: IntArray,
        cancellationCheck: () -> Unit,
    ): HistogramResult {
        require(pixels.isNotEmpty()) { "待分析像素不能为空" }

        val grayscalePixels = IntArray(pixels.size)
        pixels.forEachIndexed { index, pixel ->
            checkCancellation(index, cancellationCheck)
            grayscalePixels[index] = GrayscaleConverter.fromArgb(pixel)
        }

        val counts = IntArray(HistogramResult.GRAY_LEVELS)
        grayscalePixels.forEachIndexed { index, gray ->
            checkCancellation(index, cancellationCheck)
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

    private fun checkCancellation(index: Int, cancellationCheck: () -> Unit) {
        if (index % CANCELLATION_CHECK_INTERVAL == 0) cancellationCheck()
    }

    companion object {
        private const val CANCELLATION_CHECK_INTERVAL = 16_384
    }
}
