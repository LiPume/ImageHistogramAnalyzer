package com.lzx.imagehistogramanalyzer.domain.color

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import kotlin.math.max

/** 统计 ARGB 像素数组中的 R/G/B 三通道频次、均值和偏色状态。 */
class RgbHistogramAnalyzer {
    fun analyze(pixels: IntArray): RgbChannelStats {
        require(pixels.isNotEmpty()) { "待分析像素不能为空" }

        val redCounts = IntArray(HistogramResult.GRAY_LEVELS)
        val greenCounts = IntArray(HistogramResult.GRAY_LEVELS)
        val blueCounts = IntArray(HistogramResult.GRAY_LEVELS)

        var index = 0
        while (index < pixels.size) {
            val pixel = pixels[index]
            val red = (pixel shr RED_SHIFT) and CHANNEL_MASK
            val green = (pixel shr GREEN_SHIFT) and CHANNEL_MASK
            val blue = pixel and CHANNEL_MASK

            redCounts[red]++
            greenCounts[green]++
            blueCounts[blue]++
            index++
        }

        return analyzeCounts(
            redCounts = redCounts,
            greenCounts = greenCounts,
            blueCounts = blueCounts,
        )
    }

    fun analyzeCounts(
        redCounts: IntArray,
        greenCounts: IntArray,
        blueCounts: IntArray,
    ): RgbChannelStats {
        require(redCounts.size == HistogramResult.GRAY_LEVELS) { "R 通道频次数组必须包含 256 项" }
        require(greenCounts.size == HistogramResult.GRAY_LEVELS) { "G 通道频次数组必须包含 256 项" }
        require(blueCounts.size == HistogramResult.GRAY_LEVELS) { "B 通道频次数组必须包含 256 项" }
        require(redCounts.all { it >= 0 }) { "R 通道频次不能为负数" }
        require(greenCounts.all { it >= 0 }) { "G 通道频次不能为负数" }
        require(blueCounts.all { it >= 0 }) { "B 通道频次不能为负数" }

        val pixelCount = redCounts.sumOf { it.toLong() }
        require(pixelCount > 0) { "像素数量必须大于 0" }
        require(greenCounts.sumOf { it.toLong() } == pixelCount) {
            "G 通道频次总和必须等于 R 通道"
        }
        require(blueCounts.sumOf { it.toLong() } == pixelCount) {
            "B 通道频次总和必须等于 R 通道"
        }

        val avgRed = weightedAverage(redCounts, pixelCount)
        val avgGreen = weightedAverage(greenCounts, pixelCount)
        val avgBlue = weightedAverage(blueCounts, pixelCount)
        val channelImbalance = maxOf(avgRed, avgGreen, avgBlue) - minOf(avgRed, avgGreen, avgBlue)
        val dominantChannel = findDominantChannel(avgRed, avgGreen, avgBlue)
        val colorCastStatus = determineColorCastStatus(channelImbalance, dominantChannel)

        return RgbChannelStats(
            redCounts = redCounts,
            greenCounts = greenCounts,
            blueCounts = blueCounts,
            pixelCount = pixelCount,
            avgRed = avgRed,
            avgGreen = avgGreen,
            avgBlue = avgBlue,
            dominantChannel = dominantChannel,
            channelImbalance = channelImbalance,
            colorCastStatus = colorCastStatus,
        )
    }

    private fun weightedAverage(counts: IntArray, pixelCount: Long): Double {
        var weightedSum = 0L
        counts.forEachIndexed { value, count ->
            weightedSum += value.toLong() * count.toLong()
        }
        return weightedSum.toDouble() / pixelCount
    }

    private fun findDominantChannel(
        avgRed: Double,
        avgGreen: Double,
        avgBlue: Double,
    ): ColorChannel? {
        val maxValue = max(avgRed, max(avgGreen, avgBlue))
        val maxCount = listOf(avgRed, avgGreen, avgBlue).count { it == maxValue }
        if (maxCount != 1) return null

        return when (maxValue) {
            avgRed -> ColorChannel.RED
            avgGreen -> ColorChannel.GREEN
            else -> ColorChannel.BLUE
        }
    }

    private fun determineColorCastStatus(
        channelImbalance: Double,
        dominantChannel: ColorChannel?,
    ): ColorCastStatus {
        if (dominantChannel == null || channelImbalance < BALANCED_THRESHOLD) {
            return ColorCastStatus.BALANCED
        }

        val strongCast = channelImbalance >= OBVIOUS_CAST_THRESHOLD
        return when (dominantChannel) {
            ColorChannel.RED -> if (strongCast) ColorCastStatus.RED_CAST else ColorCastStatus.SLIGHT_RED
            ColorChannel.GREEN -> if (strongCast) ColorCastStatus.GREEN_CAST else ColorCastStatus.SLIGHT_GREEN
            ColorChannel.BLUE -> if (strongCast) ColorCastStatus.BLUE_CAST else ColorCastStatus.SLIGHT_BLUE
        }
    }

    companion object {
        const val BALANCED_THRESHOLD = 15.0
        const val OBVIOUS_CAST_THRESHOLD = 20.0

        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val CHANNEL_MASK = 0xFF
    }
}
