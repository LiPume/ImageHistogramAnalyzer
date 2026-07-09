package com.lzx.imagehistogramanalyzer.domain.color

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult

/**
 * RGB 三通道统计结果。
 *
 * 频次数组在构造时复制，避免外部修改影响分析结果；Alpha 通道不参与统计。
 */
class RgbChannelStats(
    redCounts: IntArray,
    greenCounts: IntArray,
    blueCounts: IntArray,
    val pixelCount: Long,
    val avgRed: Double,
    val avgGreen: Double,
    val avgBlue: Double,
    val dominantChannel: ColorChannel?,
    val channelImbalance: Double,
    val colorCastStatus: ColorCastStatus,
) {
    val redCounts: IntArray = redCounts.copyOf()
    val greenCounts: IntArray = greenCounts.copyOf()
    val blueCounts: IntArray = blueCounts.copyOf()

    init {
        require(this.redCounts.size == HistogramResult.GRAY_LEVELS) { "R 通道频次数组必须包含 256 项" }
        require(this.greenCounts.size == HistogramResult.GRAY_LEVELS) { "G 通道频次数组必须包含 256 项" }
        require(this.blueCounts.size == HistogramResult.GRAY_LEVELS) { "B 通道频次数组必须包含 256 项" }
        require(pixelCount > 0) { "像素数量必须大于 0" }
        require(avgRed in CHANNEL_VALUE_RANGE) { "R 通道均值必须在 0..255" }
        require(avgGreen in CHANNEL_VALUE_RANGE) { "G 通道均值必须在 0..255" }
        require(avgBlue in CHANNEL_VALUE_RANGE) { "B 通道均值必须在 0..255" }
        require(channelImbalance >= 0.0) { "通道差异度不能为负数" }
        require(this.redCounts.sumOf { it.toLong() } == pixelCount) { "R 通道频次总和必须等于像素数量" }
        require(this.greenCounts.sumOf { it.toLong() } == pixelCount) { "G 通道频次总和必须等于像素数量" }
        require(this.blueCounts.sumOf { it.toLong() } == pixelCount) { "B 通道频次总和必须等于像素数量" }
    }

    companion object {
        private val CHANNEL_VALUE_RANGE = 0.0..255.0
    }
}
