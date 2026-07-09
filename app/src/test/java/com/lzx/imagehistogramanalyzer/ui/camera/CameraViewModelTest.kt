package com.lzx.imagehistogramanalyzer.ui.camera

import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoSceneStatus
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraViewModelTest {
    @Test
    fun judgeCurrentFrame_withoutAnalysis_keepsEmptyCoachResult() {
        val viewModel = CameraViewModel()

        viewModel.judgeCurrentFrame()

        assertNull(viewModel.uiState.value.coachResult)
    }

    @Test
    fun judgeCurrentFrame_withLatestAnalysis_generatesCoachResult() {
        val viewModel = CameraViewModel()
        viewModel.onFrameAnalyzed(analysisOf(0 to 80, 80 to 20))

        viewModel.judgeCurrentFrame()

        assertEquals(
            PhotoSceneStatus.SEVERE_UNDEREXPOSED,
            viewModel.uiState.value.coachResult?.sceneStatus,
        )
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
