package com.lzx.imagehistogramanalyzer.domain.insight

import com.lzx.imagehistogramanalyzer.domain.color.RgbHistogramAnalyzer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageInsightAnalyzerTest {
    private val analyzer = ImageInsightAnalyzer()
    private val rgbAnalyzer = RgbHistogramAnalyzer()

    @Test
    fun darkHistogram_generatesUnderExposureAdvice() {
        val insight = analyzer.analyze(
            histogram = histogramOf(0 to 80, 80 to 20),
            quality = ImageQualityResult(
                meanGray = 16.0,
                darkRatio = 0.8,
                brightRatio = 0.0,
                standardDeviation = 32.0,
                category = ImageQualityCategory.DARK,
            ),
            rgbStats = rgbAnalyzer.analyze(IntArray(100) { argb(20, 20, 20) }),
        )

        assertTrue(insight.brightnessDescription.contains("偏暗"))
        assertTrue(insight.exposureDescription.contains("欠曝"))
        assertTrue(insight.summary.contains("偏暗"))
        assertTrue(insight.advice.contains("提高曝光"))
    }

    @Test
    fun brightHistogram_generatesHighlightAdvice() {
        val insight = analyzer.analyze(
            histogram = histogramOf(255 to 70, 180 to 30),
            quality = ImageQualityResult(
                meanGray = 232.5,
                darkRatio = 0.0,
                brightRatio = 0.7,
                standardDeviation = 34.0,
                category = ImageQualityCategory.BRIGHT,
            ),
            rgbStats = rgbAnalyzer.analyze(IntArray(100) { argb(240, 240, 240) }),
        )

        assertTrue(insight.brightnessDescription.contains("偏亮"))
        assertTrue(insight.exposureDescription.contains("过曝"))
        assertTrue(insight.summary.contains("偏亮"))
        assertTrue(insight.advice.contains("降低曝光"))
    }

    @Test
    fun lowContrastHistogram_mentionsInsufficientLayers() {
        val insight = analyzer.analyze(
            histogram = histogramOf(128 to 100),
            quality = ImageQualityResult(
                meanGray = 128.0,
                darkRatio = 0.0,
                brightRatio = 0.0,
                standardDeviation = 0.0,
                category = ImageQualityCategory.LOW_CONTRAST,
            ),
            rgbStats = rgbAnalyzer.analyze(IntArray(100) { argb(128, 128, 128) }),
        )

        assertTrue(insight.contrastDescription.contains("层次变化可能不足"))
        assertTrue(insight.summary.contains("低对比度"))
        assertTrue(insight.advice.contains("提高对比度"))
    }

    @Test
    fun blueCast_generatesColorDescriptionAndAdvice() {
        val insight = analyzer.analyze(
            histogram = histogramOf(64 to 1, 192 to 1),
            quality = ImageQualityResult(
                meanGray = 128.0,
                darkRatio = 0.0,
                brightRatio = 0.5,
                standardDeviation = 64.0,
                category = ImageQualityCategory.NORMAL,
            ),
            rgbStats = rgbAnalyzer.analyze(intArrayOf(argb(80, 90, 160), argb(90, 100, 170))),
        )

        assertTrue(insight.colorDescription.contains("偏蓝"))
        assertTrue(insight.summary.contains("偏蓝"))
        assertTrue(insight.advice.contains("冷色"))
    }

    @Test
    fun histogramAndRgbPixelCountMismatch_isRejected() {
        val histogram = histogramOf(128 to 2)
        val rgbStats = rgbAnalyzer.analyze(intArrayOf(argb(128, 128, 128)))

        assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(
                histogram = histogram,
                quality = ImageQualityResult(
                    meanGray = 128.0,
                    darkRatio = 0.0,
                    brightRatio = 0.0,
                    standardDeviation = 0.0,
                    category = ImageQualityCategory.LOW_CONTRAST,
                ),
                rgbStats = rgbStats,
            )
        }
    }

    private fun histogramOf(vararg bins: Pair<Int, Int>): HistogramResult {
        val counts = IntArray(256)
        bins.forEach { (gray, count) -> counts[gray] = count }
        val maxCount = counts.maxOrNull() ?: error("无频次")
        val heights = IntArray(256) { index ->
            if (maxCount == 0) 0 else counts[index] * 100 / maxCount
        }
        return HistogramResult(
            counts = counts,
            normalizedHeights = heights,
            pixelCount = counts.sumOf { it.toLong() },
            maxCount = maxCount,
        )
    }

    private fun argb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
