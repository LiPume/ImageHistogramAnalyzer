package com.lzx.imagehistogramanalyzer.domain.histogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaselineHistogramCalculatorTest {
    private val calculator = BaselineHistogramCalculator()

    @Test
    fun primaryColors_useRequiredFormulaAndRounding() {
        val result = calculator.calculate(
            intArrayOf(
                pixel(0, 0, 0),
                pixel(255, 255, 255),
                pixel(255, 0, 0),
                pixel(0, 255, 0),
                pixel(0, 0, 255),
            ),
        )

        assertEquals(1, result.counts[0])
        assertEquals(1, result.counts[255])
        assertEquals(1, result.counts[76])
        assertEquals(1, result.counts[150])
        assertEquals(1, result.counts[29])
        assertEquals(5L, result.pixelCount)
        assertEquals(5L, result.counts.sumOf { it.toLong() })
    }

    @Test
    fun transparentPixel_usesDecodedRgbAndIgnoresAlpha() {
        val transparentRed = pixel(red = 255, green = 0, blue = 0, alpha = 0)

        val result = calculator.calculate(intArrayOf(transparentRed))

        assertEquals(1, result.counts[76])
    }

    @Test
    fun repeatedPixels_produceExpectedFrequencyAndNormalizedHeight() {
        val result = calculator.calculate(
            intArrayOf(
                pixel(0, 0, 0),
                pixel(0, 0, 0),
                pixel(255, 255, 255),
                pixel(255, 0, 0),
            ),
        )

        assertEquals(2, result.counts[0])
        assertEquals(100, result.normalizedHeights[0])
        assertEquals(50, result.normalizedHeights[76])
        assertEquals(50, result.normalizedHeights[255])
    }

    @Test
    fun emptyPixels_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(intArrayOf())
        }
    }

    private fun pixel(red: Int, green: Int, blue: Int, alpha: Int = 255): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
