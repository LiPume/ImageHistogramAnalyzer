package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramNormalizer
import com.lzx.imagehistogramanalyzer.domain.histogram.MonotonicNanoClock
import com.lzx.imagehistogramanalyzer.domain.histogram.NanoClock
import com.lzx.imagehistogramanalyzer.domain.model.HistogramExecutionEngine
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

data class NativeHistogramComputation(
    val histogram: HistogramResult,
    val metrics: HistogramPerformanceMetrics,
)

/** Native v3：直接访问 Bitmap 像素，并由线程私有直方图并行统计后归并。 */
class NativeBitmapHistogramCalculator(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
    private val clock: NanoClock = MonotonicNanoClock,
    private val requestedWorkers: Int = Runtime.getRuntime().availableProcessors()
        .coerceIn(1, MAX_WORKERS),
) {
    val isAvailable: Boolean
        get() = NativeHistogramBridge.isAvailable

    fun calculate(
        bitmap: Bitmap,
        strategy: HistogramCalculationStrategy,
    ): NativeHistogramComputation {
        require(isAvailable) { "Native 直方图引擎不可用" }

        val timings = LongArray(NativeHistogramBridge.TIMING_FIELD_COUNT)
        val coreStart = clock.nowNanos()
        val counts = NativeHistogramBridge.calculate(
            bitmap = bitmap,
            strategy = strategy.nativeId,
            workerCount = requestedWorkers,
            timings = timings,
        )
        require(counts.size == HistogramResult.GRAY_LEVELS) { "Native 频次数量错误" }

        val normalizationStart = clock.nowNanos()
        val normalizedHeights = normalizer.normalize(counts)
        val normalizationNanos = clock.nowNanos() - normalizationStart
        val coreTotalNanos = clock.nowNanos() - coreStart
        val workerCount = timings[NativeHistogramBridge.WORKER_COUNT_INDEX].toInt()

        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = normalizedHeights,
            pixelCount = bitmap.width.toLong() * bitmap.height.toLong(),
            maxCount = counts.maxOrNull() ?: 0,
        )
        return NativeHistogramComputation(
            histogram = histogram,
            metrics = HistogramPerformanceMetrics(
                pixelReadNanos = timings[NativeHistogramBridge.LOCK_NANOS_INDEX],
                grayscaleConversionNanos = if (
                    strategy == HistogramCalculationStrategy.PRE_GRAYSCALE
                ) {
                    timings[NativeHistogramBridge.GRAYSCALE_NANOS_INDEX]
                } else {
                    null
                },
                countingNanos = timings[NativeHistogramBridge.COUNTING_NANOS_INDEX],
                normalizationNanos = normalizationNanos,
                mergingNanos = timings[NativeHistogramBridge.MERGING_NANOS_INDEX],
                coreTotalNanos = coreTotalNanos,
                executionEngine = HistogramExecutionEngine.NATIVE_V3,
                workerCount = workerCount,
            ),
        )
    }

    private val HistogramCalculationStrategy.nativeId: Int
        get() = when (this) {
            HistogramCalculationStrategy.PRE_GRAYSCALE -> 0
            HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING -> 1
        }

    companion object {
        private const val MAX_WORKERS = 8
    }
}
