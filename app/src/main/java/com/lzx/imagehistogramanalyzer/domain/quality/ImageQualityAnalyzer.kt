package com.lzx.imagehistogramanalyzer.domain.quality

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import kotlin.math.sqrt

/**
 * 仅遍历 256 个灰度频次计算质量指标，避免为了质量判断再次扫描原始图片。
 *
 * 判定优先级为偏暗、偏亮、低对比度、正常，使纯黑/纯白不会被误判为低对比度。
 */
class ImageQualityAnalyzer {
    fun analyze(histogram: HistogramResult): ImageQualityResult {
        val counts = histogram.counts
        var total = 0L
        var weightedSum = 0L
        var squaredWeightedSum = 0L
        var darkCount = 0L
        var brightCount = 0L

        counts.forEachIndexed { gray, count ->
            require(count >= 0) { "灰度频次不能为负数" }
            val countLong = count.toLong()
            total += countLong
            weightedSum += gray.toLong() * countLong
            squaredWeightedSum += gray.toLong() * gray * countLong
            if (gray <= DARK_MAX_GRAY) darkCount += countLong
            if (gray >= BRIGHT_MIN_GRAY) brightCount += countLong
        }
        require(total == histogram.pixelCount) { "灰度频次总和必须等于像素数量" }

        val mean = weightedSum.toDouble() / total
        val variance = (squaredWeightedSum.toDouble() / total - mean * mean)
            .coerceAtLeast(0.0)
        val standardDeviation = sqrt(variance)
        val darkRatio = darkCount.toDouble() / total
        val brightRatio = brightCount.toDouble() / total

        val category = when {
            mean < DARK_MEAN_THRESHOLD || darkRatio >= DOMINANT_RATIO -> {
                ImageQualityCategory.DARK
            }

            mean > BRIGHT_MEAN_THRESHOLD || brightRatio >= DOMINANT_RATIO -> {
                ImageQualityCategory.BRIGHT
            }

            standardDeviation < LOW_CONTRAST_STANDARD_DEVIATION -> {
                ImageQualityCategory.LOW_CONTRAST
            }

            else -> ImageQualityCategory.NORMAL
        }

        return ImageQualityResult(
            meanGray = mean,
            darkRatio = darkRatio,
            brightRatio = brightRatio,
            standardDeviation = standardDeviation,
            category = category,
        )
    }

    companion object {
        /** 暗部定义为灰度 0..63，亮部定义为灰度 192..255。 */
        const val DARK_MAX_GRAY = 63
        const val BRIGHT_MIN_GRAY = 192

        /** 经验阈值集中定义，后续课程要求变化时只需修改此处和对应测试。 */
        const val DARK_MEAN_THRESHOLD = 85.0
        const val BRIGHT_MEAN_THRESHOLD = 170.0
        const val DOMINANT_RATIO = 0.60
        const val LOW_CONTRAST_STANDARD_DEVIATION = 35.0
    }
}
