package com.lzx.imagehistogramanalyzer.domain.model

/** 基于灰度直方图得到的质量指标；不持有 Bitmap，便于算法测试和 UI 展示。 */
data class ImageQualityResult(
    val meanGray: Double,
    val darkRatio: Double,
    val brightRatio: Double,
    val standardDeviation: Double,
    val category: ImageQualityCategory,
) {
    init {
        require(meanGray in 0.0..255.0)
        require(darkRatio in 0.0..1.0)
        require(brightRatio in 0.0..1.0)
        require(standardDeviation >= 0.0)
    }
}

enum class ImageQualityCategory {
    DARK,
    BRIGHT,
    LOW_CONTRAST,
    NORMAL,
}
