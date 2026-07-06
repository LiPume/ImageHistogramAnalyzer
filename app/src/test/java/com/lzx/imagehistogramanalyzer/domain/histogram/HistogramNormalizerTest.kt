package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HistogramNormalizerTest {
    private val normalizer = HistogramNormalizer()

    @Test
    fun zeroHistogram_returnsAllZeroHeights() {
        val normalized = normalizer.normalize(IntArray(HistogramResult.GRAY_LEVELS))

        assertArrayEquals(IntArray(HistogramResult.GRAY_LEVELS), normalized)
    }

    @Test
    fun frequencies_areRoundedAndMappedToOneHundred() {
        val counts = IntArray(HistogramResult.GRAY_LEVELS).apply {
            this[0] = 3
            this[1] = 2
            this[2] = 1
        }

        val normalized = normalizer.normalize(counts)

        assertEquals(100, normalized[0])
        assertEquals(67, normalized[1])
        assertEquals(33, normalized[2])
    }

    @Test
    fun invalidSize_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizer.normalize(IntArray(255))
        }
    }

    @Test
    fun negativeFrequency_isRejected() {
        val counts = IntArray(HistogramResult.GRAY_LEVELS).apply { this[10] = -1 }

        assertThrows(IllegalArgumentException::class.java) {
            normalizer.normalize(counts)
        }
    }
}
