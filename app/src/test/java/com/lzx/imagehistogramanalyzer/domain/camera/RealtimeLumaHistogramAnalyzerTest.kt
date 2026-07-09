package com.lzx.imagehistogramanalyzer.domain.camera

import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RealtimeLumaHistogramAnalyzerTest {
    private val analyzer = RealtimeLumaHistogramAnalyzer()

    @Test
    fun analyzeCompactYPlane_buildsHistogramAndQualityResult() {
        val result = analyzer.analyze(
            lumaBytes = byteArrayOf(
                0,
                64,
                128.toByte(),
                255.toByte(),
            ),
            width = 2,
            height = 2,
            rowStride = 2,
            pixelStride = 1,
            analyzedAtNanos = 123L,
        )

        assertEquals(4L, result.histogram.pixelCount)
        assertEquals(1, result.histogram.counts[0])
        assertEquals(1, result.histogram.counts[64])
        assertEquals(1, result.histogram.counts[128])
        assertEquals(1, result.histogram.counts[255])
        assertEquals(100, result.histogram.normalizedHeights[0])
        assertEquals(2, result.frameWidth)
        assertEquals(2, result.frameHeight)
        assertEquals(123L, result.analyzedAtNanos)
        assertEquals(ImageQualityCategory.NORMAL, result.qualityResult.category)
    }

    @Test
    fun analyzePaddedYPlane_respectsRowStrideAndPixelStride() {
        val result = analyzer.analyze(
            lumaBytes = byteArrayOf(
                10, -1, 20, -1, 30, -1, 99,
                40, -1, 50, -1, 60, -1, 99,
            ),
            width = 3,
            height = 2,
            rowStride = 7,
            pixelStride = 2,
            analyzedAtNanos = 456L,
        )

        assertEquals(6L, result.histogram.pixelCount)
        listOf(10, 20, 30, 40, 50, 60).forEach { gray ->
            assertEquals(1, result.histogram.counts[gray])
        }
        assertEquals(0, result.histogram.counts[99])
    }

    @Test
    fun invalidPlaneData_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(
                lumaBytes = byteArrayOf(1, 2, 3),
                width = 4,
                height = 1,
                rowStride = 4,
                pixelStride = 1,
                analyzedAtNanos = 0L,
            )
        }
    }
}
