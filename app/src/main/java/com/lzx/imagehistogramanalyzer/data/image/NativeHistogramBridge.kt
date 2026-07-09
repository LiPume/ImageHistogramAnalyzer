package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

data class NativeHistogramChannels(
    val grayCounts: IntArray,
    val redCounts: IntArray,
    val greenCounts: IntArray,
    val blueCounts: IntArray,
) {
    init {
        require(grayCounts.size == HistogramResult.GRAY_LEVELS)
        require(redCounts.size == HistogramResult.GRAY_LEVELS)
        require(greenCounts.size == HistogramResult.GRAY_LEVELS)
        require(blueCounts.size == HistogramResult.GRAY_LEVELS)
    }
}

/** JNI 边界：Native 层直接锁定软件 Bitmap，避免复制到约 48MiB Java IntArray。 */
object NativeHistogramBridge {
    val isAvailable: Boolean = runCatching {
        System.loadLibrary("histogram_native")
    }.isSuccess

    fun calculate(
        bitmap: Bitmap,
        strategy: Int,
        workerCount: Int,
        timings: LongArray,
    ): NativeHistogramChannels {
        check(isAvailable) { "Native 直方图库不可用" }
        require(timings.size >= TIMING_FIELD_COUNT)
        val counts = nativeCalculate(bitmap, strategy, workerCount, timings)
        require(counts.size == CHANNEL_COUNTS_FIELD_COUNT * HistogramResult.GRAY_LEVELS) {
            "Native 频次数量错误"
        }
        return NativeHistogramChannels(
            grayCounts = counts.copyOfRange(GRAY_COUNTS_OFFSET, RED_COUNTS_OFFSET),
            redCounts = counts.copyOfRange(RED_COUNTS_OFFSET, GREEN_COUNTS_OFFSET),
            greenCounts = counts.copyOfRange(GREEN_COUNTS_OFFSET, BLUE_COUNTS_OFFSET),
            blueCounts = counts.copyOfRange(BLUE_COUNTS_OFFSET, counts.size),
        )
    }

    fun grayForTest(red: Int, green: Int, blue: Int): Int {
        check(isAvailable) { "Native 直方图库不可用" }
        return nativeGray(red, green, blue)
    }

    private external fun nativeCalculate(
        bitmap: Bitmap,
        strategy: Int,
        workerCount: Int,
        timings: LongArray,
    ): IntArray

    private external fun nativeGray(red: Int, green: Int, blue: Int): Int

    const val TIMING_FIELD_COUNT = 6
    const val CHANNEL_COUNTS_FIELD_COUNT = 4
    const val GRAY_COUNTS_OFFSET = 0
    const val RED_COUNTS_OFFSET = HistogramResult.GRAY_LEVELS
    const val GREEN_COUNTS_OFFSET = HistogramResult.GRAY_LEVELS * 2
    const val BLUE_COUNTS_OFFSET = HistogramResult.GRAY_LEVELS * 3
    const val LOCK_NANOS_INDEX = 0
    const val GRAYSCALE_NANOS_INDEX = 1
    const val COUNTING_NANOS_INDEX = 2
    const val MERGING_NANOS_INDEX = 3
    const val NATIVE_TOTAL_NANOS_INDEX = 4
    const val WORKER_COUNT_INDEX = 5
}
