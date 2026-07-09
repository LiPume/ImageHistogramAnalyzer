package com.lzx.imagehistogramanalyzer.domain.photo

import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedPhotoCoachTest {
    private val coach = RuleBasedPhotoCoach()

    @Test
    fun severeUnderexposedFrame_recommendsExposureUpAndTorchOn() {
        val result = coach.analyze(analysisOf(0 to 80, 80 to 20))

        assertEquals(PhotoSceneStatus.SEVERE_UNDEREXPOSED, result.sceneStatus)
        assertEquals(+2, result.exposureDelta)
        assertEquals(TorchAction.TURN_ON, result.torchAction)
        assertTrue(result.reason.contains("严重欠曝"))
        assertTrue(result.advice.contains("提高曝光补偿"))
    }

    @Test
    fun brightFrame_recommendsExposureDownAndTorchOff() {
        val result = coach.analyze(analysisOf(255 to 70, 180 to 30))

        assertEquals(PhotoSceneStatus.SEVERE_OVEREXPOSED, result.sceneStatus)
        assertEquals(-2, result.exposureDelta)
        assertEquals(TorchAction.TURN_OFF, result.torchAction)
        assertTrue(result.reason.contains("严重过曝"))
        assertTrue(result.advice.contains("降低曝光补偿"))
    }

    @Test
    fun lowContrastFrame_keepsCameraParameters() {
        val result = coach.analyze(analysisOf(128 to 100))

        assertEquals(PhotoSceneStatus.LOW_CONTRAST, result.sceneStatus)
        assertEquals(0, result.exposureDelta)
        assertEquals(TorchAction.KEEP, result.torchAction)
        assertTrue(result.advice.contains("增加明暗层次"))
    }

    @Test
    fun balancedFrame_keepsCurrentParameters() {
        val result = coach.analyze(analysisOf(64 to 50, 192 to 50))

        assertEquals(PhotoSceneStatus.NORMAL, result.sceneStatus)
        assertEquals(0, result.exposureDelta)
        assertEquals(TorchAction.KEEP, result.torchAction)
        assertTrue(result.advice.contains("保持现有拍摄参数"))
    }

    private fun analysisOf(vararg bins: Pair<Int, Int>): RealtimeCameraAnalysis {
        val counts = IntArray(256)
        bins.forEach { (gray, count) -> counts[gray] = count }
        val maxCount = counts.maxOrNull() ?: 0
        val normalized = IntArray(256) { index ->
            if (maxCount == 0) 0 else counts[index] * 100 / maxCount
        }
        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = normalized,
            pixelCount = counts.sumOf { it.toLong() },
            maxCount = maxCount,
        )
        return RealtimeCameraAnalysis(
            histogram = histogram,
            qualityResult = ImageQualityAnalyzer().analyze(histogram),
            frameWidth = 10,
            frameHeight = 10,
            analyzedAtNanos = 1L,
        )
    }
}
