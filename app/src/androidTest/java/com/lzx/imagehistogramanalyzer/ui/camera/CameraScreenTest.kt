package com.lzx.imagehistogramanalyzer.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult
import com.lzx.imagehistogramanalyzer.domain.photo.PhotoSceneStatus
import com.lzx.imagehistogramanalyzer.domain.photo.TorchAction
import com.lzx.imagehistogramanalyzer.domain.quality.ImageQualityAnalyzer
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cameraWithoutPermission_showsPermissionRequestAction() {
        var requested = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(hasCameraPermission = false),
                    onBackHome = {},
                    onRequestPermission = { requested = true },
                    onCameraBindingChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    previewContent = {},
                )
            }
        }

        composeRule.onNodeWithText("需要相机权限").assertIsDisplayed()
        composeRule.onNodeWithText("授权相机并开始预览")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(requested)
    }

    @Test
    fun cameraWithAnalysis_showsRealtimeHistogramAndQualityMetrics() {
        val analysis = realtimeAnalysis()
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        latestAnalysis = analysis,
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    previewContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        ) {
                            Text("测试预览")
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("相机预览").assertIsDisplayed()
        composeRule.onNodeWithText("测试预览").assertIsDisplayed()
        composeRule.onNodeWithText("实时亮度直方图").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("256 个灰度等级的黑白直方图")
            .assertIsDisplayed()
        composeRule.onNodeWithText("640 × 480").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Y 通道亮度").assertIsDisplayed()
        composeRule.onNodeWithText("实时质量指标").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("正常").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("智能拍摄建议"))
        composeRule.onNodeWithText("智能拍摄建议").assertIsDisplayed()
        composeRule.onNodeWithText("智能判断当前画面").assertHasClickAction()
    }

    @Test
    fun cameraCoachAction_showsRuleBasedAdvice() {
        var judged = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        latestAnalysis = realtimeAnalysis(),
                        coachResult = PhotoCoachResult(
                            sceneStatus = PhotoSceneStatus.DARK,
                            reason = "平均亮度 70.0，暗部占比 65.0%，画面整体偏暗。",
                            advice = "建议小幅提高曝光补偿，或让主体靠近更亮的环境。",
                            exposureDelta = +1,
                            torchAction = TorchAction.KEEP,
                        ),
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = { judged = true },
                    previewContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        ) {
                            Text("测试预览")
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("智能拍摄建议"))
        composeRule.onNodeWithText("智能拍摄建议").assertIsDisplayed()
        composeRule.onNodeWithText("智能判断当前画面")
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("偏暗").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+1 档").assertIsDisplayed()
        composeRule.onNodeWithText("保持不变").assertIsDisplayed()
        composeRule.onNodeWithText("判断依据：平均亮度 70.0，暗部占比 65.0%，画面整体偏暗。")
            .assertIsDisplayed()

        assertTrue(judged)
    }

    private fun realtimeAnalysis(): RealtimeCameraAnalysis {
        val counts = IntArray(256).apply {
            this[64] = 160_000
            this[192] = 147_200
        }
        val normalized = IntArray(256).apply {
            this[64] = 100
            this[192] = 92
        }
        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = normalized,
            pixelCount = 307_200,
            maxCount = 160_000,
        )
        return RealtimeCameraAnalysis(
            histogram = histogram,
            qualityResult = ImageQualityAnalyzer().analyze(histogram),
            frameWidth = 640,
            frameHeight = 480,
            analyzedAtNanos = 123L,
        )
    }
}
