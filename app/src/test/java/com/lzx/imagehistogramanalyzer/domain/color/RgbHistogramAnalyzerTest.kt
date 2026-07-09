package com.lzx.imagehistogramanalyzer.domain.color

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RgbHistogramAnalyzerTest {
    private val analyzer = RgbHistogramAnalyzer()

    @Test
    fun pureRedPixels_areCountedAsRedCast() {
        val result = analyzer.analyze(intArrayOf(argb(255, 0, 0), argb(255, 0, 0)))

        assertEquals(2L, result.pixelCount)
        assertEquals(2, result.redCounts[255])
        assertEquals(2, result.greenCounts[0])
        assertEquals(2, result.blueCounts[0])
        assertEquals(255.0, result.avgRed, TOLERANCE)
        assertEquals(0.0, result.avgGreen, TOLERANCE)
        assertEquals(0.0, result.avgBlue, TOLERANCE)
        assertEquals(ColorChannel.RED, result.dominantChannel)
        assertEquals(255.0, result.channelImbalance, TOLERANCE)
        assertEquals(ColorCastStatus.RED_CAST, result.colorCastStatus)
    }

    @Test
    fun pureGreenPixels_areCountedAsGreenCast() {
        val result = analyzer.analyze(intArrayOf(argb(0, 255, 0)))

        assertEquals(1L, result.pixelCount)
        assertEquals(1, result.redCounts[0])
        assertEquals(1, result.greenCounts[255])
        assertEquals(1, result.blueCounts[0])
        assertEquals(ColorChannel.GREEN, result.dominantChannel)
        assertEquals(ColorCastStatus.GREEN_CAST, result.colorCastStatus)
    }

    @Test
    fun pureBluePixels_areCountedAsBlueCast() {
        val result = analyzer.analyze(intArrayOf(argb(0, 0, 255)))

        assertEquals(1L, result.pixelCount)
        assertEquals(1, result.redCounts[0])
        assertEquals(1, result.greenCounts[0])
        assertEquals(1, result.blueCounts[255])
        assertEquals(ColorChannel.BLUE, result.dominantChannel)
        assertEquals(ColorCastStatus.BLUE_CAST, result.colorCastStatus)
    }

    @Test
    fun grayPixels_areBalanced() {
        val result = analyzer.analyze(intArrayOf(argb(128, 128, 128), argb(64, 64, 64)))

        assertEquals(96.0, result.avgRed, TOLERANCE)
        assertEquals(96.0, result.avgGreen, TOLERANCE)
        assertEquals(96.0, result.avgBlue, TOLERANCE)
        assertEquals(0.0, result.channelImbalance, TOLERANCE)
        assertNull(result.dominantChannel)
        assertEquals(ColorCastStatus.BALANCED, result.colorCastStatus)
    }

    @Test
    fun slightBlueDifference_isClassifiedAsSlightBlue() {
        val result = analyzer.analyze(intArrayOf(argb(128, 128, 144)))

        assertEquals(ColorChannel.BLUE, result.dominantChannel)
        assertEquals(16.0, result.channelImbalance, TOLERANCE)
        assertEquals(ColorCastStatus.SLIGHT_BLUE, result.colorCastStatus)
    }

    @Test
    fun alphaChannel_isIgnored() {
        val transparentRed = (0x00 shl 24) or (255 shl 16)
        val opaqueRed = argb(255, 0, 0)

        val transparentResult = analyzer.analyze(intArrayOf(transparentRed))
        val opaqueResult = analyzer.analyze(intArrayOf(opaqueRed))

        assertArrayEquals(opaqueResult.redCounts, transparentResult.redCounts)
        assertArrayEquals(opaqueResult.greenCounts, transparentResult.greenCounts)
        assertArrayEquals(opaqueResult.blueCounts, transparentResult.blueCounts)
        assertEquals(opaqueResult.colorCastStatus, transparentResult.colorCastStatus)
    }

    @Test
    fun emptyPixels_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            analyzer.analyze(IntArray(0))
        }
    }

    private fun argb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

    companion object {
        private const val TOLERANCE = 0.000_001
    }
}
