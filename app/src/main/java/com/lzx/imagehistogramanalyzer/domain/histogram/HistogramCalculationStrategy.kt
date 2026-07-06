package com.lzx.imagehistogramanalyzer.domain.histogram

/** 课程要求的两种直方图计算流程。 */
enum class HistogramCalculationStrategy {
    PRE_GRAYSCALE,
    GRAYSCALE_WHILE_COUNTING,
}
