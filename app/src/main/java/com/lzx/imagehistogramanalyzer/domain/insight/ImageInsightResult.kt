package com.lzx.imagehistogramanalyzer.domain.insight

/** 面向用户展示的本地自然语言图像质量判断结果。 */
data class ImageInsightResult(
    val brightnessDescription: String,
    val exposureDescription: String,
    val contrastDescription: String,
    val colorDescription: String,
    val summary: String,
    val advice: String,
)
