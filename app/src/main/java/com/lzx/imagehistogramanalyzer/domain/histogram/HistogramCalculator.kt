package com.lzx.imagehistogramanalyzer.domain.histogram

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

interface HistogramCalculator {
    val strategy: HistogramCalculationStrategy

    fun calculateMeasured(
        pixels: IntArray,
        cancellationCheck: () -> Unit = {},
    ): MeasuredHistogramResult

    fun calculate(
        pixels: IntArray,
        cancellationCheck: () -> Unit = {},
    ): HistogramResult = calculateMeasured(pixels, cancellationCheck).histogram
}
