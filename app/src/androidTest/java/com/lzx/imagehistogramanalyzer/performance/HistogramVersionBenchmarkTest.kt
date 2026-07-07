package com.lzx.imagehistogramanalyzer.performance

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.lzx.imagehistogramanalyzer.data.image.BitmapPixelReader
import com.lzx.imagehistogramanalyzer.data.image.NativeBitmapHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.BaselineHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.PreGrayscaleHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import java.io.File
import java.util.Locale
import org.junit.Assert.assertArrayEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** 手动性能基准；普通 connectedDebugAndroidTest 会跳过，避免把耗时阈值变成易波动测试。 */
class HistogramVersionBenchmarkTest {
    @Test
    fun benchmarkKotlinV2AndNativeV3() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val enabled = InstrumentationRegistry.getArguments()
            .getString(BENCHMARK_ARGUMENT)
            .toBoolean()
        assumeTrue("仅在显式启用时运行性能基准", enabled)

        val output = mutableListOf(CSV_HEADER)
        val nativeCalculator = NativeBitmapHistogramCalculator(requestedWorkers = 8)
        check(nativeCalculator.isAvailable) { "当前设备不支持 Native v3" }

        // Debug/ART 使用 JIT；先跨方案预热热函数，再进行各尺寸自己的预热和采样。
        val jitWarmupBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        jitWarmupBitmap.eraseColor(Color.rgb(72, 136, 218))
        repeat(GLOBAL_JIT_WARMUP_RUNS) {
            HistogramCalculationStrategy.entries.forEach { strategy ->
                calculateWithKotlin(jitWarmupBitmap, strategy.kotlinCalculator)
                nativeCalculator.calculate(jitWarmupBitmap, strategy)
            }
        }
        jitWarmupBitmap.recycle()

        TEST_SIZES.forEach { size ->
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(72, 136, 218))
            val runs = if (size.pixelCount >= LARGE_IMAGE_PIXELS) LARGE_RUNS else NORMAL_RUNS

            HistogramCalculationStrategy.entries.forEach { strategy ->
                val kotlinCalculator = strategy.kotlinCalculator
                val kotlinSamples = benchmark(
                    warmups = WARMUP_RUNS,
                    runs = runs,
                ) { calculateWithKotlin(bitmap, kotlinCalculator) }
                val nativeSamples = benchmark(
                    warmups = WARMUP_RUNS,
                    runs = runs,
                ) { nativeCalculator.calculate(bitmap, strategy).metrics }

                val pixels = BitmapPixelReader().read(bitmap)
                val expected = kotlinCalculator.calculate(pixels)
                val native = nativeCalculator.calculate(bitmap, strategy)
                assertArrayEquals(expected.counts, native.histogram.counts)

                output += row(
                    version = "v2.0",
                    engine = "Kotlin 单线程",
                    strategy = strategy.chineseName,
                    size = size,
                    runs = runs,
                    samples = kotlinSamples,
                    maxCount = expected.maxCount,
                )
                output += row(
                    version = "v3.0",
                    engine = "Native Bitmap直读+多线程",
                    strategy = strategy.chineseName,
                    size = size,
                    runs = runs,
                    samples = nativeSamples,
                    maxCount = native.histogram.maxCount,
                )

                // 大图额外记录线程缩放，避免只比较“单线程 Kotlin”和“8 线程 Native”而看不出并行收益。
                if (size.pixelCount >= LARGE_IMAGE_PIXELS) {
                    THREAD_SCALING_WORKERS.forEach { workers ->
                        val scalingCalculator = NativeBitmapHistogramCalculator(
                            requestedWorkers = workers,
                        )
                        val scalingSamples = benchmark(
                            warmups = WARMUP_RUNS,
                            runs = runs,
                        ) { scalingCalculator.calculate(bitmap, strategy).metrics }
                        val scalingResult = scalingCalculator.calculate(bitmap, strategy)
                        assertArrayEquals(expected.counts, scalingResult.histogram.counts)
                        output += row(
                            version = "v3.0",
                            engine = "Native 线程缩放实验",
                            strategy = strategy.chineseName,
                            size = size,
                            runs = runs,
                            samples = scalingSamples,
                            maxCount = scalingResult.histogram.maxCount,
                        )
                    }
                }
            }
            bitmap.recycle()
            Runtime.getRuntime().gc()
        }

        val report = output.joinToString(separator = "\n", postfix = "\n")
        val reportFile = File(instrumentation.targetContext.filesDir, REPORT_FILE_NAME)
        reportFile.writeText(report)
        Log.i(LOG_TAG, "中文性能基准已写入：${reportFile.absolutePath}\n$report")
    }

    private fun calculateWithKotlin(
        bitmap: Bitmap,
        calculator: HistogramCalculator,
    ): HistogramPerformanceMetrics {
        val coreStart = System.nanoTime()
        val pixelStart = System.nanoTime()
        val pixels = BitmapPixelReader().read(bitmap)
        val pixelNanos = System.nanoTime() - pixelStart
        val measured = calculator.calculateMeasured(pixels)
        return HistogramPerformanceMetrics(
            pixelReadNanos = pixelNanos,
            grayscaleConversionNanos = measured.timings.grayscaleConversionNanos,
            countingNanos = measured.timings.countingNanos,
            normalizationNanos = measured.timings.normalizationNanos,
            mergingNanos = measured.timings.mergingNanos,
            coreTotalNanos = System.nanoTime() - coreStart,
        )
    }

    private fun benchmark(
        warmups: Int,
        runs: Int,
        block: () -> HistogramPerformanceMetrics,
    ): List<HistogramPerformanceMetrics> {
        repeat(warmups) { block() }
        return List(runs) { block() }
    }

    private fun row(
        version: String,
        engine: String,
        strategy: String,
        size: TestSize,
        runs: Int,
        samples: List<HistogramPerformanceMetrics>,
        maxCount: Int,
    ): String {
        val core = samples.map { it.coreTotalNanos }.sorted()
        val sample = samples[samples.size / 2]
        val workers = samples.maxOf { it.workerCount }
        return listOf(
            version,
            engine,
            strategy,
            "${size.width}×${size.height}",
            size.pixelCount,
            workers,
            WARMUP_RUNS,
            runs,
            medianMillis(samples.map { it.pixelReadNanos }),
            nullableMedianMillis(samples.map { it.grayscaleConversionNanos }),
            medianMillis(samples.map { it.countingNanos }),
            medianMillis(samples.map { it.normalizationNanos }),
            nullableMedianMillis(samples.map { it.mergingNanos }),
            nanosToMillis(core[core.size / 2]),
            nanosToMillis(core[((core.size * 9 + 9) / 10) - 1]),
            nanosToMillis(core.last()),
            maxCount,
            sample.executionEngine.name,
        ).joinToString(",")
    }

    private fun medianMillis(values: List<Long>): String {
        val sorted = values.sorted()
        return nanosToMillis(sorted[sorted.size / 2])
    }

    private fun nullableMedianMillis(values: List<Long?>): String {
        val nonNull = values.filterNotNull()
        return if (nonNull.isEmpty()) "不适用" else medianMillis(nonNull)
    }

    private fun nanosToMillis(nanos: Long): String =
        String.format(Locale.US, "%.3f", nanos / 1_000_000.0)

    private val HistogramCalculationStrategy.kotlinCalculator: HistogramCalculator
        get() = when (this) {
            HistogramCalculationStrategy.PRE_GRAYSCALE -> PreGrayscaleHistogramCalculator()
            HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING -> {
                BaselineHistogramCalculator()
            }
        }

    private val HistogramCalculationStrategy.chineseName: String
        get() = when (this) {
            HistogramCalculationStrategy.PRE_GRAYSCALE -> "优先灰度化"
            HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING -> "统计时灰度化"
        }

    private data class TestSize(val width: Int, val height: Int) {
        val pixelCount: Long = width.toLong() * height
    }

    companion object {
        private const val BENCHMARK_ARGUMENT = "runPerformanceBenchmark"
        private const val REPORT_FILE_NAME = "模拟器性能测试_v3.csv"
        private const val LOG_TAG = "HistogramBenchmark"
        private const val WARMUP_RUNS = 5
        private const val GLOBAL_JIT_WARMUP_RUNS = 20
        private const val NORMAL_RUNS = 10
        private const val LARGE_RUNS = 10
        private const val LARGE_IMAGE_PIXELS = 10_000_000L
        private val THREAD_SCALING_WORKERS = listOf(1, 2, 4, 6)
        private val TEST_SIZES = listOf(
            TestSize(512, 512),
            TestSize(1440, 1080),
            TestSize(3072, 4096),
        )
        private const val CSV_HEADER =
            "版本,执行引擎,方案,分辨率,像素数,线程数,预热次数,正式次数," +
                "像素访问中位ms,灰度转换中位ms,统计或融合中位ms,归一化中位ms," +
                "合并中位ms,核心中位ms,P90ms,最差ms,最高频次,引擎标识"
    }
}
