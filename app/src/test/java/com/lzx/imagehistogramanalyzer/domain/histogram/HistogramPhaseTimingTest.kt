package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistogramPhaseTimingTest {
    @Test
    fun preGrayscale_reportsConversionCountingAndNormalizationSeparately() {
        val calculator = PreGrayscaleHistogramCalculator(
            clock = IncrementingNanoClock(stepNanos = 10),
        )

        val measured = calculator.calculateMeasured(intArrayOf(pixel(10, 20, 30)))

        assertEquals(10L, measured.timings.grayscaleConversionNanos)
        assertEquals(10L, measured.timings.countingNanos)
        assertEquals(10L, measured.timings.normalizationNanos)
        assertEquals(1L, measured.histogram.pixelCount)
    }

    @Test
    fun whileCounting_reportsFusedStageAndNormalization() {
        val calculator = BaselineHistogramCalculator(
            clock = IncrementingNanoClock(stepNanos = 25),
        )

        val measured = calculator.calculateMeasured(intArrayOf(pixel(10, 20, 30)))

        assertNull(measured.timings.grayscaleConversionNanos)
        assertEquals(25L, measured.timings.countingNanos)
        assertEquals(25L, measured.timings.normalizationNanos)
        assertEquals(1L, measured.histogram.pixelCount)
    }

    @Test
    fun performanceMetrics_reportsUnattributedCoreOverhead() {
        val metrics = HistogramPerformanceMetrics(
            pixelReadNanos = 10,
            grayscaleConversionNanos = 20,
            countingNanos = 30,
            normalizationNanos = 5,
            mergingNanos = null,
            coreTotalNanos = 80,
        )

        assertEquals(15L, metrics.overheadNanos)
    }

    private fun pixel(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private class IncrementingNanoClock(
        private val stepNanos: Long,
    ) : NanoClock {
        private var currentNanos = 0L

        override fun nowNanos(): Long = currentNanos.also { currentNanos += stepNanos }
    }
}
