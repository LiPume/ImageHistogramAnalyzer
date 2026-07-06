package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

/**
 * 方案一：先把全部彩色像素转换成灰度数组，再单独统计灰度频次。
 *
 * 该方案逻辑直观，但需要额外的灰度数组和第二次遍历。
 */
class PreGrayscaleHistogramCalculator(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
    private val clock: NanoClock = MonotonicNanoClock,
) : HistogramCalculator {
    override val strategy = HistogramCalculationStrategy.PRE_GRAYSCALE

    override fun calculateMeasured(
        pixels: IntArray,
        cancellationCheck: () -> Unit,
    ): MeasuredHistogramResult {
        require(pixels.isNotEmpty()) { "待分析像素不能为空" }

        val grayscaleStart = clock.nowNanos()
        val grayscalePixels = IntArray(pixels.size)
        var chunkStart = 0
        while (chunkStart < pixels.size) {
            cancellationCheck()
            val chunkEnd = minOf(chunkStart + CANCELLATION_CHECK_INTERVAL, pixels.size)
            var index = chunkStart
            while (index < chunkEnd) {
                grayscalePixels[index] = GrayscaleConverter.fromArgb(pixels[index])
                index++
            }
            chunkStart = chunkEnd
        }
        val grayscaleNanos = clock.nowNanos() - grayscaleStart

        val countingStart = clock.nowNanos()
        val counts = IntArray(HistogramResult.GRAY_LEVELS)
        chunkStart = 0
        while (chunkStart < grayscalePixels.size) {
            cancellationCheck()
            val chunkEnd = minOf(
                chunkStart + CANCELLATION_CHECK_INTERVAL,
                grayscalePixels.size,
            )
            var index = chunkStart
            while (index < chunkEnd) {
                counts[grayscalePixels[index]]++
                index++
            }
            chunkStart = chunkEnd
        }

        val maxCount = counts.maxOrNull() ?: 0
        val countingNanos = clock.nowNanos() - countingStart

        val normalizationStart = clock.nowNanos()
        val normalizedHeights = normalizer.normalize(counts)
        val normalizationNanos = clock.nowNanos() - normalizationStart

        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = normalizedHeights,
            pixelCount = pixels.size.toLong(),
            maxCount = maxCount,
        )
        return MeasuredHistogramResult(
            histogram = histogram,
            timings = HistogramStageTimings(
                grayscaleConversionNanos = grayscaleNanos,
                countingNanos = countingNanos,
                normalizationNanos = normalizationNanos,
            ),
        )
    }

    companion object {
        private const val CANCELLATION_CHECK_INTERVAL = 16_384
    }
}
