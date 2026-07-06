package com.lzx.imagehistogramanalyzer.domain.histogram

import kotlin.math.roundToInt
import org.junit.Assert.fail
import org.junit.Test

class GrayscaleConverterTest {
    @Test
    fun optimizedConverter_matchesPreviousFloatingPointResultForEveryRgbColor() {
        for (red in 0..255) {
            for (green in 0..255) {
                for (blue in 0..255) {
                    val expected = (red * 0.299 + green * 0.587 + blue * 0.114)
                        .roundToInt()
                        .coerceIn(0, 255)
                    val actual = GrayscaleConverter.fromRgb(red, green, blue)
                    if (expected != actual) {
                        fail(
                            "RGB($red,$green,$blue) 期望灰度 $expected，实际 $actual",
                        )
                    }
                }
            }
        }
    }
}
