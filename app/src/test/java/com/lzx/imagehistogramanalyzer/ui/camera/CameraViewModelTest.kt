package com.lzx.imagehistogramanalyzer.ui.camera

import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentController
import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentState
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoSceneStatus
import com.lzx.imagehistogramanalyzer.domain.photo.TorchAction
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraViewModelTest {
    @Test
    fun judgeCurrentFrame_withoutAnalysis_keepsEmptyCoachResult() {
        val viewModel = CameraViewModel(cameraAdjustmentDispatcher = Dispatchers.Unconfined)

        viewModel.judgeCurrentFrame()

        assertNull(viewModel.uiState.value.coachResult)
    }

    @Test
    fun judgeCurrentFrame_withLatestAnalysis_generatesCoachResult() {
        val viewModel = CameraViewModel(cameraAdjustmentDispatcher = Dispatchers.Unconfined)
        viewModel.onFrameAnalyzed(analysisOf(0 to 80, 80 to 20))

        viewModel.judgeCurrentFrame()

        assertEquals(
            PhotoSceneStatus.SEVERE_UNDEREXPOSED,
            viewModel.uiState.value.coachResult?.sceneStatus,
        )
    }

    @Test
    fun applySuggestedAdjustment_withoutCoachResult_doesNotRetakeFrame() {
        val controller = FakeAdjustmentController()
        val viewModel = CameraViewModel(cameraAdjustmentDispatcher = Dispatchers.Unconfined)
        viewModel.setAdjustmentController(controller)

        viewModel.applySuggestedAdjustment()

        assertEquals(0, controller.autoApplyCount)
        assertTrue(viewModel.uiState.value.adjustmentState.message!!.contains("先定格分析"))
    }

    @Test
    fun manualExposureAdjustment_canBeRepeatedByUser() {
        val controller = FakeAdjustmentController()
        val viewModel = CameraViewModel(cameraAdjustmentDispatcher = Dispatchers.Unconfined)
        viewModel.setAdjustmentController(controller)

        viewModel.increaseExposure()
        viewModel.increaseExposure()

        assertEquals(2, controller.exposureDeltaSum)
        assertEquals(2, viewModel.uiState.value.adjustmentState.exposureIndex)
    }

    @Test
    fun applySuggestedAdjustment_usesCurrentCoachResultAndRequestsRetakeAfterUserAction() {
        val controller = FakeAdjustmentController()
        val viewModel = CameraViewModel(cameraAdjustmentDispatcher = Dispatchers.Unconfined)
        viewModel.setAdjustmentController(controller)
        viewModel.onFrameAnalyzed(analysisOf(0 to 80, 80 to 20))
        viewModel.judgeCurrentFrame()

        viewModel.applySuggestedAdjustment()

        assertEquals(1, controller.autoApplyCount)
        assertEquals(2, viewModel.uiState.value.adjustmentState.exposureIndex)
        assertTrue(viewModel.uiState.value.adjustmentState.isTorchOn)
        assertEquals(1, viewModel.uiState.value.retakeRequestId)
        assertTrue(viewModel.uiState.value.adjustmentState.message!!.contains("重新定格"))
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

    private class FakeAdjustmentController : CameraAdjustmentController {
        var exposureDeltaSum = 0
        var autoApplyCount = 0
        private var state = CameraAdjustmentState(
            isExposureSupported = true,
            exposureIndex = 0,
            minExposureIndex = -4,
            maxExposureIndex = 4,
            hasFlashUnit = true,
            isTorchOn = false,
        )

        override fun currentState(): CameraAdjustmentState = state

        override suspend fun adjustExposureBy(delta: Int): CameraAdjustmentState {
            exposureDeltaSum += delta
            state = state.copy(
                exposureIndex = (state.exposureIndex + delta)
                    .coerceIn(state.minExposureIndex, state.maxExposureIndex),
                message = "曝光补偿已调整。",
            )
            return state
        }

        override suspend fun toggleTorch(): CameraAdjustmentState {
            state = state.copy(
                isTorchOn = !state.isTorchOn,
                message = "补光灯已切换。",
            )
            return state
        }

        override suspend fun applySuggestion(coachResult: PhotoCoachResult): CameraAdjustmentState {
            autoApplyCount += 1
            adjustExposureBy(coachResult.exposureDelta)
            state = when (coachResult.torchAction) {
                TorchAction.TURN_ON -> state.copy(isTorchOn = true)
                TorchAction.TURN_OFF -> state.copy(isTorchOn = false)
                TorchAction.KEEP -> state
            }.copy(message = "已按建议调整。")
            return state
        }
    }
}
