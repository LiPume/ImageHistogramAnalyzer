package com.lzx.imagehistogramanalyzer.domain.camera

import com.lzx.imagehistogramanalyzer.domain.histogram.GrayscaleConverter
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramNormalizer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer

/** 将定格的预览 Bitmap 像素按课程 RGB 灰度公式转成可展示的直方图结果。 */
class PreviewSnapshotHistogramAnalyzer(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
    private val qualityAnalyzer: ImageQualityAnalyzer = ImageQualityAnalyzer(),
) {
    fun analyze(
        pixels: IntArray,
        width: Int,
        height: Int,
        analyzedAtNanos: Long,
    ): RealtimeCameraAnalysis {
        require(width > 0) { "定格画面宽度必须大于 0" }
        require(height > 0) { "定格画面高度必须大于 0" }
        require(pixels.size == width * height) { "定格画面像素数量必须等于宽高乘积" }

        val counts = IntArray(HistogramResult.GRAY_LEVELS)
        var index = 0
        while (index < pixels.size) {
            val gray = GrayscaleConverter.fromArgb(pixels[index])
            counts[gray] += 1
            index += 1
        }

        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = normalizer.normalize(counts),
            pixelCount = pixels.size.toLong(),
            maxCount = counts.maxOrNull() ?: 0,
        )
        return RealtimeCameraAnalysis(
            histogram = histogram,
            qualityResult = qualityAnalyzer.analyze(histogram),
            frameWidth = width,
            frameHeight = height,
            analyzedAtNanos = analyzedAtNanos,
            source = RealtimeFrameSource.PREVIEW_BITMAP,
        )
    }
}
