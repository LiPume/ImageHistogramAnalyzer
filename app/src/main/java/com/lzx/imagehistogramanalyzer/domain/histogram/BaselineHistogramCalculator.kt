package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

/**
 * MVP 基础算法：单线程遍历 ARGB 像素并统计灰度频次。
 *
 * Alpha 通道不参与课程指定公式，透明像素仍按其解码后的 RGB 值统计。
 */
class BaselineHistogramCalculator(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
    private val clock: NanoClock = MonotonicNanoClock,
) : HistogramCalculator {
    override val strategy = HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING

    override fun calculateMeasured(
        pixels: IntArray,
        cancellationCheck: () -> Unit,
    ): MeasuredHistogramResult {
        require(pixels.isNotEmpty()) { "待分析像素不能为空" }

        val countingStart = clock.nowNanos()
        val counts = IntArray(HistogramResult.GRAY_LEVELS)
        var chunkStart = 0
        while (chunkStart < pixels.size) {
            // 只在分块边界检查取消，避免在逐像素热循环中执行取模与回调判断。
            cancellationCheck()
            val chunkEnd = minOf(chunkStart + CANCELLATION_CHECK_INTERVAL, pixels.size)
            var index = chunkStart
            while (index < chunkEnd) {
                val gray = GrayscaleConverter.fromArgb(pixels[index])
                counts[gray]++
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
                grayscaleConversionNanos = null,
                countingNanos = countingNanos,
                normalizationNanos = normalizationNanos,
            ),
        )
    }

    companion object {
        private const val CANCELLATION_CHECK_INTERVAL = 16_384
    }
}
