package com.lzx.imagehistogramanalyzer.domain.photo

import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import java.util.Locale

/**
 * 基于实时亮度直方图的本地拍摄建议。
 *
 * 第一版只生成“怎么调”的建议，不触发 CameraControl；后续 CAM-08~10 再把 exposureDelta
 * 和 torchAction 转换成真实相机控制。
 */
class RuleBasedPhotoCoach {
    fun analyze(analysis: RealtimeCameraAnalysis): PhotoCoachResult {
        val quality = analysis.qualityResult
        val shadowClippingRatio = analysis.histogram.rangeRatio(SHADOW_CLIPPING_RANGE)
        val highlightClippingRatio = analysis.histogram.rangeRatio(HIGHLIGHT_CLIPPING_RANGE)
        val mean = quality.meanGray
        val darkRatio = quality.darkRatio
        val brightRatio = quality.brightRatio
        val standardDeviation = quality.standardDeviation

        return when {
            mean < SEVERE_UNDEREXPOSED_MEAN || shadowClippingRatio > SHADOW_CLIPPING_RATIO -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.SEVERE_UNDEREXPOSED,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，暗部溢出约 ${shadowClippingRatio.formatPercent()}，画面可能严重欠曝。",
                    advice = "建议提高曝光补偿；如果主体仍然太暗，后续可尝试开启补光灯。",
                    exposureDelta = +2,
                    torchAction = TorchAction.TURN_ON,
                )
            }

            mean < DARK_MEAN || darkRatio > DOMINANT_RATIO -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.DARK,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，暗部占比 ${darkRatio.formatPercent()}，画面整体偏暗。",
                    advice = "建议小幅提高曝光补偿，或让主体靠近更亮的环境。",
                    exposureDelta = +1,
                    torchAction = TorchAction.KEEP,
                )
            }

            mean < SLIGHTLY_DARK_MEAN && darkRatio > SLIGHTLY_DARK_RATIO -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.SLIGHTLY_DARK,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，暗部占比 ${darkRatio.formatPercent()}，画面略微偏暗。",
                    advice = "建议略微提高曝光，注意不要让高光区域过曝。",
                    exposureDelta = +1,
                    torchAction = TorchAction.KEEP,
                )
            }

            mean > SEVERE_OVEREXPOSED_MEAN || highlightClippingRatio > HIGHLIGHT_CLIPPING_RATIO -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.SEVERE_OVEREXPOSED,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，高光溢出约 ${highlightClippingRatio.formatPercent()}，画面可能严重过曝。",
                    advice = "建议降低曝光补偿，并避免强光直接进入画面。",
                    exposureDelta = -2,
                    torchAction = TorchAction.TURN_OFF,
                )
            }

            mean > BRIGHT_MEAN || brightRatio > DOMINANT_RATIO -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.BRIGHT,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，亮部占比 ${brightRatio.formatPercent()}，画面整体偏亮。",
                    advice = "建议小幅降低曝光补偿，保留亮部细节。",
                    exposureDelta = -1,
                    torchAction = TorchAction.TURN_OFF,
                )
            }

            mean > SLIGHTLY_BRIGHT_MEAN && brightRatio > SLIGHTLY_BRIGHT_RATIO -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.SLIGHTLY_BRIGHT,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，亮部占比 ${brightRatio.formatPercent()}，画面略微偏亮。",
                    advice = "建议略微降低曝光，避免天空、灯牌等区域丢失细节。",
                    exposureDelta = -1,
                    torchAction = TorchAction.KEEP,
                )
            }

            standardDeviation < LOW_CONTRAST_STANDARD_DEVIATION -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.LOW_CONTRAST,
                    reason = "灰度标准差 ${standardDeviation.formatOneDecimal()}，画面层次变化偏弱。",
                    advice = "建议调整构图或光线方向，增加明暗层次；曝光补偿暂时保持不变。",
                    exposureDelta = 0,
                    torchAction = TorchAction.KEEP,
                )
            }

            else -> {
                PhotoCoachResult(
                    sceneStatus = PhotoSceneStatus.NORMAL,
                    reason = "平均亮度 ${mean.formatOneDecimal()}，暗部 ${darkRatio.formatPercent()}，亮部 ${brightRatio.formatPercent()}，整体分布较均衡。",
                    advice = "当前画面曝光较正常，可以保持现有拍摄参数。",
                    exposureDelta = 0,
                    torchAction = TorchAction.KEEP,
                )
            }
        }
    }

    private fun HistogramResult.rangeRatio(range: IntRange): Double {
        val total = pixelCount
        if (total <= 0L) return 0.0
        val count = range.sumOf { gray -> counts[gray].toLong() }
        return count.toDouble() / total.toDouble()
    }

    private fun Double.formatOneDecimal(): String = String.format(Locale.US, "%.1f", this)

    private fun Double.formatPercent(): String = String.format(Locale.US, "%.1f%%", this * 100.0)

    companion object {
        private val SHADOW_CLIPPING_RANGE = 0..5
        private val HIGHLIGHT_CLIPPING_RANGE = 250..255

        const val SEVERE_UNDEREXPOSED_MEAN = 60.0
        const val DARK_MEAN = 85.0
        const val SLIGHTLY_DARK_MEAN = 100.0
        const val SEVERE_OVEREXPOSED_MEAN = 205.0
        const val BRIGHT_MEAN = 180.0
        const val SLIGHTLY_BRIGHT_MEAN = 165.0
        const val DOMINANT_RATIO = 0.60
        const val SLIGHTLY_DARK_RATIO = 0.45
        const val SLIGHTLY_BRIGHT_RATIO = 0.45
        const val SHADOW_CLIPPING_RATIO = 0.20
        const val HIGHLIGHT_CLIPPING_RATIO = 0.18
        const val LOW_CONTRAST_STANDARD_DEVIATION = 35.0
    }
}
