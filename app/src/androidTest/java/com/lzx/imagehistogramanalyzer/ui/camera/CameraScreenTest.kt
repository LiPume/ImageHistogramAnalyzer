package com.lzx.imagehistogramanalyzer.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
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
