package com.lzx.imagehistogramanalyzer.data.image

import android.graphics.Bitmap
import com.lzx.imagehistogramanalyzer.domain.histogram.BaselineHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.histogram.PreGrayscaleHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.color.RgbHistogramAnalyzer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramExecutionEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeHistogramCalculatorTest {
    @Test
    fun nativeLibrary_isAvailableOnInstrumentedDevice() {
        assertTrue(NativeHistogramBridge.isAvailable)
    }

    @Test
    fun nativeGray_matchesKotlinAtPrimaryAndHalfBoundaryColors() {
        val colors = listOf(
            Triple(0, 0, 0),
            Triple(255, 255, 255),
            Triple(255, 0, 0),
            Triple(0, 255, 0),
            Triple(0, 0, 255),
            // 该颜色直接整数四舍五入会得到 23，旧版浮点语义结果是 22。
            Triple(0, 36, 12),
        )

        colors.forEach { (red, green, blue) ->
            val pixel = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            val expected = com.lzx.imagehistogramanalyzer.domain.histogram.GrayscaleConverter
                .fromArgb(pixel)
            assertEquals(expected, NativeHistogramBridge.grayForTest(red, green, blue))
        }
    }

    @Test
    fun nativeAndKotlin_matchAtEveryFloatingPointHalfBoundary() {
        // 整数快路径仅在加权和尾数恰为 .500 时回退浮点；穷举这些边界即可覆盖取整风险。
        val boundaryPixels = ArrayList<Int>(EXPECTED_HALF_BOUNDARY_COLOR_COUNT)
        for (red in 0..255) {
            for (green in 0..255) {
                for (blue in 0..255) {
                    if ((red * 299 + green * 587 + blue * 114) % 1000 == 500) {
                        boundaryPixels += (0xFF shl 24) or
                            (red shl 16) or
                            (green shl 8) or
                            blue
                    }
                }
            }
        }
        assertEquals(EXPECTED_HALF_BOUNDARY_COLOR_COUNT, boundaryPixels.size)

        val width = 256
        val height = (boundaryPixels.size + width - 1) / width
        val pixels = IntArray(width * height) { index ->
            boundaryPixels.getOrElse(index) { 0xFF000000.toInt() }
        }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val kotlinPixels = BitmapPixelReader().read(bitmap)
        val nativeCalculator = NativeBitmapHistogramCalculator(requestedWorkers = 4)

        val expected = BaselineHistogramCalculator().calculate(kotlinPixels)
        HistogramCalculationStrategy.entries.forEach { strategy ->
            val actual = nativeCalculator.calculate(bitmap, strategy).histogram
            assertArrayEquals(expected.counts, actual.counts)
            assertArrayEquals(expected.normalizedHeights, actual.normalizedHeights)
        }
        bitmap.recycle()
    }

    @Test
    fun nativeAndKotlin_produceIdenticalBinsForBothStrategies() {
        val width = 64
        val height = 48
        val pixels = IntArray(width * height) { index ->
            val red = index * 31 and 0xFF
            val green = index * 17 and 0xFF
            val blue = index * 7 and 0xFF
            val alpha = when (index % 3) {
                0 -> 255
                1 -> 128
                else -> 0
            }
            (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val readPixels = BitmapPixelReader().read(bitmap)
        val nativeCalculator = NativeBitmapHistogramCalculator(requestedWorkers = 4)
        val expectedRgb = RgbHistogramAnalyzer().analyze(readPixels)

        val kotlinPre = PreGrayscaleHistogramCalculator().calculate(readPixels)
        val nativePre = nativeCalculator.calculate(
            bitmap,
            HistogramCalculationStrategy.PRE_GRAYSCALE,
        )
        assertArrayEquals(kotlinPre.counts, nativePre.histogram.counts)
        assertArrayEquals(kotlinPre.normalizedHeights, nativePre.histogram.normalizedHeights)
        assertArrayEquals(expectedRgb.redCounts, nativePre.rgbStats.redCounts)
        assertArrayEquals(expectedRgb.greenCounts, nativePre.rgbStats.greenCounts)
        assertArrayEquals(expectedRgb.blueCounts, nativePre.rgbStats.blueCounts)

        val kotlinFused = BaselineHistogramCalculator().calculate(readPixels)
        val nativeFused = nativeCalculator.calculate(
            bitmap,
            HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING,
        )
        assertArrayEquals(kotlinFused.counts, nativeFused.histogram.counts)
        assertArrayEquals(kotlinFused.normalizedHeights, nativeFused.histogram.normalizedHeights)
        assertArrayEquals(expectedRgb.redCounts, nativeFused.rgbStats.redCounts)
        assertArrayEquals(expectedRgb.greenCounts, nativeFused.rgbStats.greenCounts)
        assertArrayEquals(expectedRgb.blueCounts, nativeFused.rgbStats.blueCounts)
        assertEquals(HistogramExecutionEngine.NATIVE_V3, nativeFused.metrics.executionEngine)
        assertEquals(4, nativeFused.metrics.workerCount)

        bitmap.recycle()
    }

    companion object {
        private const val EXPECTED_HALF_BOUNDARY_COLOR_COUNT = 16_782
    }
}
