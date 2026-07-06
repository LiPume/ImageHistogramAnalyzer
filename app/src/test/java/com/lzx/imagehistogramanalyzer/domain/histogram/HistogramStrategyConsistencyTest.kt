package com.lzx.imagehistogramanalyzer.domain.histogram

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HistogramStrategyConsistencyTest {
    private val preGrayscale = PreGrayscaleHistogramCalculator()
    private val whileCounting = BaselineHistogramCalculator()

    @Test
    fun bothStrategies_produceIdenticalHistogramForMixedPixels() {
        val pixels = intArrayOf(
            pixel(0, 0, 0),
            pixel(255, 255, 255),
            pixel(255, 0, 0),
            pixel(0, 255, 0),
            pixel(0, 0, 255),
            pixel(40, 80, 120),
            pixel(40, 80, 120),
            pixel(200, 130, 20, alpha = 0),
        )

        val first = preGrayscale.calculate(pixels)
        val second = whileCounting.calculate(pixels)

        assertArrayEquals(second.counts, first.counts)
        assertArrayEquals(second.normalizedHeights, first.normalizedHeights)
        assertEquals(second.pixelCount, first.pixelCount)
        assertEquals(second.maxCount, first.maxCount)
    }

    @Test
    fun calculators_reportTheirCourseStrategy() {
        assertEquals(HistogramCalculationStrategy.PRE_GRAYSCALE, preGrayscale.strategy)
        assertEquals(
            HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING,
            whileCounting.strategy,
        )
    }

    private fun pixel(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
