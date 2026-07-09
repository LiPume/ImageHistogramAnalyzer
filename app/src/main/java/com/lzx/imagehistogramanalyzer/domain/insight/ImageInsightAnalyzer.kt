package com.lzx.imagehistogramanalyzer.domain.insight

import com.lzx.imagehistogramanalyzer.domain.color.ColorCastStatus
import com.lzx.imagehistogramanalyzer.domain.color.RgbChannelStats
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer
import java.util.Locale

/**
 * 基于灰度直方图、质量指标和 RGB 通道统计生成本地自然语言判断。
 *
 * 第一版不联网、不调用 AI API，所有结论都能追溯到现有统计指标。
 */
class ImageInsightAnalyzer {
    fun analyze(
        histogram: HistogramResult,
        quality: ImageQualityResult,
        rgbStats: RgbChannelStats,
    ): ImageInsightResult {
        require(histogram.pixelCount == rgbStats.pixelCount) {
            "灰度直方图和 RGB 统计必须来自同一批像素"
        }

        val shadowClippingRatio = histogram.ratioIn(SHADOW_CLIPPING_RANGE)
        val highlightClippingRatio = histogram.ratioIn(HIGHLIGHT_CLIPPING_RANGE)
        val effectiveGrayRange = histogram.effectiveGrayRange()

        val brightnessDescription = quality.brightnessDescription()
        val exposureDescription = exposureDescription(
            quality = quality,
            shadowClippingRatio = shadowClippingRatio,
            highlightClippingRatio = highlightClippingRatio,
        )
        val contrastDescription = contrastDescription(quality, effectiveGrayRange)
        val colorDescription = rgbStats.colorDescription()
        val summary = summary(quality, rgbStats)
        val advice = advice(quality, rgbStats)

        return ImageInsightResult(
            brightnessDescription = brightnessDescription,
            exposureDescription = exposureDescription,
            contrastDescription = contrastDescription,
            colorDescription = colorDescription,
            summary = summary,
            advice = advice,
        )
    }

    private fun ImageQualityResult.brightnessDescription(): String = when (category) {
        ImageQualityCategory.DARK -> {
            "当前图像整体偏暗，平均灰度为 ${meanGray.formatOneDecimal()}，暗部像素占比为 ${darkRatio.formatPercent()}。"
        }

        ImageQualityCategory.BRIGHT -> {
            "当前图像整体偏亮，平均灰度为 ${meanGray.formatOneDecimal()}，亮部像素占比为 ${brightRatio.formatPercent()}。"
        }

        ImageQualityCategory.LOW_CONTRAST -> {
            "当前图像亮度集中在中间区域，平均灰度为 ${meanGray.formatOneDecimal()}。"
        }

        ImageQualityCategory.NORMAL -> {
            "当前图像整体亮度较均衡，平均灰度为 ${meanGray.formatOneDecimal()}。"
        }
    }

    private fun exposureDescription(
        quality: ImageQualityResult,
        shadowClippingRatio: Double,
        highlightClippingRatio: Double,
    ): String = when {
        shadowClippingRatio > SHADOW_CLIPPING_WARNING_RATIO -> {
            "暗部溢出占比为 ${shadowClippingRatio.formatPercent()}，画面可能存在欠曝或阴影细节丢失。"
        }

        highlightClippingRatio > HIGHLIGHT_CLIPPING_WARNING_RATIO -> {
            "高光溢出占比为 ${highlightClippingRatio.formatPercent()}，画面可能存在过曝或高光细节丢失。"
        }

        quality.category == ImageQualityCategory.DARK -> {
            "暗部比例较高，曝光可能偏低。"
        }

        quality.category == ImageQualityCategory.BRIGHT -> {
            "亮部比例较高，曝光可能偏高。"
        }

        else -> "暗部和高光溢出比例较低，曝光状态相对稳定。"
    }

    private fun contrastDescription(
        quality: ImageQualityResult,
        effectiveGrayRange: IntRange,
    ): String {
        val rangeText = "${effectiveGrayRange.first}–${effectiveGrayRange.last}"
        return if (quality.standardDeviation < ImageQualityAnalyzer.LOW_CONTRAST_STANDARD_DEVIATION) {
            "灰度标准差为 ${quality.standardDeviation.formatOneDecimal()}，有效灰度范围约为 $rangeText，图像层次变化可能不足。"
        } else {
            "灰度标准差为 ${quality.standardDeviation.formatOneDecimal()}，有效灰度范围约为 $rangeText，图像层次变化较明显。"
        }
    }

    private fun RgbChannelStats.colorDescription(): String {
        val channelText = "R=${avgRed.formatOneDecimal()}，G=${avgGreen.formatOneDecimal()}，B=${avgBlue.formatOneDecimal()}"
        return when (colorCastStatus) {
            ColorCastStatus.BALANCED -> "RGB 三通道均值差异较小（$channelText），色彩整体较均衡。"
            ColorCastStatus.SLIGHT_RED -> "R 通道略高（$channelText），画面可能存在轻微偏红或偏暖。"
            ColorCastStatus.SLIGHT_GREEN -> "G 通道略高（$channelText），画面可能存在轻微偏绿。"
            ColorCastStatus.SLIGHT_BLUE -> "B 通道略高（$channelText），画面可能存在轻微偏蓝或偏冷。"
            ColorCastStatus.RED_CAST -> "R 通道明显高于其他通道（$channelText），画面可能偏红或偏暖。"
            ColorCastStatus.GREEN_CAST -> "G 通道明显高于其他通道（$channelText），画面可能偏绿。"
            ColorCastStatus.BLUE_CAST -> "B 通道明显高于其他通道（$channelText），画面可能偏蓝或偏冷。"
        }
    }

    private fun summary(
        quality: ImageQualityResult,
        rgbStats: RgbChannelStats,
    ): String {
        val brightness = when (quality.category) {
            ImageQualityCategory.DARK -> "偏暗"
            ImageQualityCategory.BRIGHT -> "偏亮"
            ImageQualityCategory.LOW_CONTRAST -> "低对比度"
            ImageQualityCategory.NORMAL -> "正常"
        }
        val color = when (rgbStats.colorCastStatus) {
            ColorCastStatus.BALANCED -> "色彩较均衡"
            ColorCastStatus.SLIGHT_RED -> "轻微偏红"
            ColorCastStatus.SLIGHT_GREEN -> "轻微偏绿"
            ColorCastStatus.SLIGHT_BLUE -> "轻微偏蓝"
            ColorCastStatus.RED_CAST -> "偏红"
            ColorCastStatus.GREEN_CAST -> "偏绿"
            ColorCastStatus.BLUE_CAST -> "偏蓝"
        }
        return "综合判断：图像亮度状态为$brightness，$color。"
    }

    private fun advice(
        quality: ImageQualityResult,
        rgbStats: RgbChannelStats,
    ): String {
        val brightnessAdvice = when (quality.category) {
            ImageQualityCategory.DARK -> "建议提高曝光补偿或改善光照条件。"
            ImageQualityCategory.BRIGHT -> "建议降低曝光补偿，避免高光区域继续丢失细节。"
            ImageQualityCategory.LOW_CONTRAST -> "建议适当增强光线层次或后期提高对比度。"
            ImageQualityCategory.NORMAL -> "当前亮度和对比度基础较好，可保持现有拍摄条件。"
        }
        val colorAdvice = when (rgbStats.colorCastStatus) {
            ColorCastStatus.BALANCED -> "色彩方面暂不需要明显调整。"
            ColorCastStatus.SLIGHT_RED,
            ColorCastStatus.RED_CAST,
            -> "如需更自然的色彩，可适当降低暖色倾向或校正白平衡。"
            ColorCastStatus.SLIGHT_GREEN,
            ColorCastStatus.GREEN_CAST,
            -> "如需更自然的色彩，可检查环境绿光影响或校正白平衡。"
            ColorCastStatus.SLIGHT_BLUE,
            ColorCastStatus.BLUE_CAST,
            -> "如需更自然的色彩，可适当降低冷色倾向或校正白平衡。"
        }
        return "$brightnessAdvice $colorAdvice"
    }

    private fun HistogramResult.ratioIn(range: IntRange): Double {
        val clippedCount = range.sumOf { gray -> counts[gray].toLong() }
        return clippedCount.toDouble() / pixelCount
    }

    private fun HistogramResult.effectiveGrayRange(): IntRange {
        val first = counts.indexOfFirst { it > 0 }.takeIf { it >= 0 } ?: 0
        val last = counts.indexOfLast { it > 0 }.takeIf { it >= 0 } ?: first
        return first..last
    }

    private fun Double.formatOneDecimal(): String = String.format(Locale.US, "%.1f", this)

    private fun Double.formatPercent(): String = String.format(Locale.US, "%.1f%%", this * 100.0)

    companion object {
        private val SHADOW_CLIPPING_RANGE = 0..5
        private val HIGHLIGHT_CLIPPING_RANGE = 250..255
        private const val SHADOW_CLIPPING_WARNING_RATIO = 0.20
        private const val HIGHLIGHT_CLIPPING_WARNING_RATIO = 0.18
    }
}
