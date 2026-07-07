package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap

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
    ): IntArray {
        check(isAvailable) { "Native 直方图库不可用" }
        require(timings.size >= TIMING_FIELD_COUNT)
        return nativeCalculate(bitmap, strategy, workerCount, timings)
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
    const val LOCK_NANOS_INDEX = 0
    const val GRAYSCALE_NANOS_INDEX = 1
    const val COUNTING_NANOS_INDEX = 2
    const val MERGING_NANOS_INDEX = 3
    const val NATIVE_TOTAL_NANOS_INDEX = 4
    const val WORKER_COUNT_INDEX = 5
}
