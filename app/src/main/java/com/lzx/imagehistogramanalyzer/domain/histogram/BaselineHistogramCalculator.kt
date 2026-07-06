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
        pixels.forEachIndexed { index, pixel ->
            // 大图每处理一块检查一次取消，避免新图片选择后旧任务继续占用 CPU。
            if (index % CANCELLATION_CHECK_INTERVAL == 0) cancellationCheck()

            val gray = GrayscaleConverter.fromArgb(pixel)
            counts[gray]++
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
