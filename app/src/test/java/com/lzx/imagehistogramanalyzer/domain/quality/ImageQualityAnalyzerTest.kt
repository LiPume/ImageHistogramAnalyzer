package com.lzx.imagehistogramanalyzer.domain.quality

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageQualityAnalyzerTest {
    private val analyzer = ImageQualityAnalyzer()

    @Test
    fun blackHistogram_isDarkAndHasExpectedMetrics() {
        val result = analyzer.analyze(histogramOf(0 to 100))

        assertEquals(0.0, result.meanGray, TOLERANCE)
        assertEquals(1.0, result.darkRatio, TOLERANCE)
        assertEquals(0.0, result.brightRatio, TOLERANCE)
        assertEquals(0.0, result.standardDeviation, TOLERANCE)
        assertEquals(ImageQualityCategory.DARK, result.category)
    }

    @Test
    fun whiteHistogram_isBrightAndHasExpectedMetrics() {
        val result = analyzer.analyze(histogramOf(255 to 100))

        assertEquals(255.0, result.meanGray, TOLERANCE)
        assertEquals(0.0, result.darkRatio, TOLERANCE)
        assertEquals(1.0, result.brightRatio, TOLERANCE)
        assertEquals(0.0, result.standardDeviation, TOLERANCE)
        assertEquals(ImageQualityCategory.BRIGHT, result.category)
    }

    @Test
    fun darkMeanFixture_isDarkWithoutDominantDarkBins() {
        val result = analyzer.analyze(histogramOf(80 to 100))

        assertEquals(80.0, result.meanGray, TOLERANCE)
        assertEquals(0.0, result.darkRatio, TOLERANCE)
        assertEquals(ImageQualityCategory.DARK, result.category)
    }

    @Test
    fun brightMeanFixture_isBrightWithoutDominantBrightBins() {
        val result = analyzer.analyze(histogramOf(180 to 100))

        assertEquals(180.0, result.meanGray, TOLERANCE)
        assertEquals(0.0, result.brightRatio, TOLERANCE)
        assertEquals(ImageQualityCategory.BRIGHT, result.category)
    }

    @Test
    fun uniformMiddleGray_isLowContrast() {
        val result = analyzer.analyze(histogramOf(128 to 20))

        assertEquals(128.0, result.meanGray, TOLERANCE)
        assertEquals(0.0, result.standardDeviation, TOLERANCE)
        assertEquals(ImageQualityCategory.LOW_CONTRAST, result.category)
    }

    @Test
    fun balancedWideDistribution_isNormal() {
        val result = analyzer.analyze(histogramOf(64 to 50, 192 to 50))

        assertEquals(128.0, result.meanGray, TOLERANCE)
        assertEquals(64.0, result.standardDeviation, TOLERANCE)
        assertEquals(0.0, result.darkRatio, TOLERANCE)
        assertEquals(0.5, result.brightRatio, TOLERANCE)
        assertEquals(ImageQualityCategory.NORMAL, result.category)
    }

    @Test
    fun dominantDarkPixels_overrideMiddleMean() {
        val result = analyzer.analyze(histogramOf(63 to 60, 255 to 40))

        assertEquals(0.60, result.darkRatio, TOLERANCE)
        assertEquals(ImageQualityCategory.DARK, result.category)
    }

    @Test
    fun shadowSideDominantHistogram_isDarkEvenWhenStrictDarkBinsAreNotDominant() {
        val result = analyzer.analyze(histogramOf(70 to 40, 110 to 45, 180 to 15))

        assertEquals(0.0, result.darkRatio, TOLERANCE)
        assertEquals(ImageQualityCategory.DARK, result.category)
    }

    @Test
    fun highlightSideDominantHistogram_isBrightEvenWhenStrictBrightBinsAreNotDominant() {
        val result = analyzer.analyze(histogramOf(80 to 15, 145 to 45, 185 to 40))

        assertEquals(0.0, result.brightRatio, TOLERANCE)
        assertEquals(ImageQualityCategory.BRIGHT, result.category)
    }

    @Test
    fun histogramCountMismatch_isRejected() {
        val counts = IntArray(256).apply { this[128] = 9 }
        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = IntArray(256).apply { this[128] = 100 },
            pixelCount = 10,
            maxCount = 9,
        )

        assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(histogram)
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

    companion object {
        private const val TOLERANCE = 0.000_001
    }
}
