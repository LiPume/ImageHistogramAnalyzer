package com.lzx.imagehistogramanalyzer.domain.camera

import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult

/**
 * 实时相机帧分析结果。
 *
 * 第一版实时预览使用 CameraX `YUV_420_888` 的 Y 通道作为亮度信号，避免每帧做完整 YUV→RGB
 * 转换造成卡顿；静态图片验收仍使用课程指定 RGB 灰度公式。
 */
data class RealtimeCameraAnalysis(
    val histogram: HistogramResult,
    val qualityResult: ImageQualityResult,
    val frameWidth: Int,
    val frameHeight: Int,
    val analyzedAtNanos: Long,
    val source: RealtimeFrameSource = RealtimeFrameSource.Y_PLANE,
) {
    init {
        require(frameWidth > 0) { "实时帧宽度必须大于 0" }
        require(frameHeight > 0) { "实时帧高度必须大于 0" }
        require(histogram.pixelCount == frameWidth.toLong() * frameHeight.toLong()) {
            "实时帧直方图像素数必须等于宽高乘积"
        }
    }
}

enum class RealtimeFrameSource {
    Y_PLANE,
    PREVIEW_BITMAP,
}
