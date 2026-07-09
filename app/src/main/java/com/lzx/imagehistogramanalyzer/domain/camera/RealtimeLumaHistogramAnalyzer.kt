package com.lzx.imagehistogramanalyzer.domain.camera

import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramNormalizer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer

/** 将 CameraX Y 通道实时帧转成 256-bin 亮度直方图和质量指标。 */
class RealtimeLumaHistogramAnalyzer(
    private val normalizer: HistogramNormalizer = HistogramNormalizer(),
    private val qualityAnalyzer: ImageQualityAnalyzer = ImageQualityAnalyzer(),
) {
    fun analyze(
        lumaBytes: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        analyzedAtNanos: Long,
    ): RealtimeCameraAnalysis {
        require(width > 0) { "实时帧宽度必须大于 0" }
        require(height > 0) { "实时帧高度必须大于 0" }
        require(pixelStride > 0) { "Y 通道 pixelStride 必须大于 0" }
        val minRowStride = (width - 1) * pixelStride + 1
        require(rowStride >= minRowStride) { "Y 通道 rowStride 不能覆盖当前帧宽度" }
        val lastRowStart = (height - 1) * rowStride
        val lastPixelIndex = lastRowStart + (width - 1) * pixelStride
        require(lastPixelIndex < lumaBytes.size) { "Y 通道数据长度不足以覆盖当前帧" }

        val counts = IntArray(HistogramResult.GRAY_LEVELS)
        var row = 0
        while (row < height) {
            val rowOffset = row * rowStride
            var column = 0
            while (column < width) {
                val index = rowOffset + column * pixelStride
                val gray = lumaBytes[index].toInt() and 0xFF
                counts[gray] += 1
                column += 1
            }
            row += 1
        }

        val normalized = normalizer.normalize(counts)
        val maxCount = counts.maxOrNull() ?: 0
        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = normalized,
            pixelCount = width.toLong() * height.toLong(),
            maxCount = maxCount,
        )
        return RealtimeCameraAnalysis(
            histogram = histogram,
            qualityResult = qualityAnalyzer.analyze(histogram),
            frameWidth = width,
            frameHeight = height,
            analyzedAtNanos = analyzedAtNanos,
        )
    }
}
