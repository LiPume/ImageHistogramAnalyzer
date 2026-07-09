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
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L

        var index = 0
        while (index < pixels.size) {
            val pixel = pixels[index]
            val red = (pixel shr RED_SHIFT) and CHANNEL_MASK
            val green = (pixel shr GREEN_SHIFT) and CHANNEL_MASK
            val blue = pixel and CHANNEL_MASK

            redCounts[red]++
            greenCounts[green]++
            blueCounts[blue]++
            redSum += red.toLong()
            greenSum += green.toLong()
            blueSum += blue.toLong()
            index++
        }

        val pixelCount = pixels.size.toLong()
        val avgRed = redSum.toDouble() / pixelCount
        val avgGreen = greenSum.toDouble() / pixelCount
        val avgBlue = blueSum.toDouble() / pixelCount
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
