package com.lzx.imagehistogramanalyzer.domain.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSnapshotHistogramAnalyzerTest {
    private val analyzer = PreviewSnapshotHistogramAnalyzer()

    @Test
    fun analyzePreviewSnapshot_usesCourseRgbGrayscaleFormula() {
        val pixels = intArrayOf(
            argb(255, 0, 0),
            argb(0, 255, 0),
            argb(0, 0, 255),
            argb(255, 255, 255),
        )

        val result = analyzer.analyze(
            pixels = pixels,
            width = 2,
            height = 2,
            analyzedAtNanos = 100L,
        )

        assertEquals(1, result.histogram.counts[76])
        assertEquals(1, result.histogram.counts[150])
        assertEquals(1, result.histogram.counts[29])
        assertEquals(1, result.histogram.counts[255])
        assertEquals(4L, result.histogram.pixelCount)
        assertEquals(RealtimeFrameSource.PREVIEW_BITMAP, result.source)
    }

    @Test
    fun invalidPixelSize_isRejected() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(
                pixels = intArrayOf(argb(0, 0, 0)),
                width = 2,
                height = 2,
                analyzedAtNanos = 100L,
            )
        }
    }

    private fun argb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
