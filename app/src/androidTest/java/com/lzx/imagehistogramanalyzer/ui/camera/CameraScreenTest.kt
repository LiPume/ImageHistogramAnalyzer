package com.lzx.imagehistogramanalyzer.ui.camera

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeFrameSource
import com.lzx.imagehistogramanalyzer.domain.camera.CameraAdjustmentState
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
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = {},
                    onIncreaseExposure = {},
                    onToggleTorch = {},
                    onApplySuggestedAdjustment = {},
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = {},
                    onSaveFrozenFrame = {},
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
                        adjustmentState = adjustableCameraState(),
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = {},
                    onIncreaseExposure = {},
                    onToggleTorch = {},
                    onApplySuggestedAdjustment = {},
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = {},
                    onSaveFrozenFrame = {},
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
        composeRule.onNodeWithContentDescription("画面状态：质量良好").assertIsDisplayed()
        composeRule.onNodeWithText("质量良好").assertIsDisplayed()
        composeRule.onNodeWithText("实时亮度直方图").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("256 个灰度等级的黑白直方图")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("640 × 480"))
        composeRule.onNodeWithText("640 × 480").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("Y 通道亮度"))
        composeRule.onNodeWithText("Y 通道亮度").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("实时质量指标"))
        composeRule.onNodeWithText("实时质量指标").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("正常"))
        composeRule.onNodeWithText("正常").assertIsDisplayed()
    }

    @Test
    fun cameraPreview_showsControlsBelowPreviewAndDisablesAutoBeforeFreeze() {
        var decreased = false
        var increased = false
        var torchToggled = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        latestAnalysis = realtimeAnalysis(),
                        adjustmentState = adjustableCameraState(),
                        coachResult = PhotoCoachResult(
                            sceneStatus = PhotoSceneStatus.DARK,
                            reason = "平均亮度 70.0，暗部占比 65.0%，画面整体偏暗。",
                            advice = "建议小幅提高曝光补偿。",
                            exposureDelta = +1,
                            torchAction = TorchAction.KEEP,
                        ),
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = { decreased = true },
                    onIncreaseExposure = { increased = true },
                    onToggleTorch = { torchToggled = true },
                    onApplySuggestedAdjustment = {},
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = {},
                    onSaveFrozenFrame = {},
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

        composeRule.onNodeWithText("拍摄控制").assertIsDisplayed()
        composeRule.onNodeWithText("测试预览").assertIsDisplayed()
        composeRule.onNodeWithText("曝光 0（-2..2）").assertIsDisplayed()
        composeRule.onNodeWithText("展开控制").assertHasClickAction().performClick()
        composeRule.onNodeWithText("定格分析").assertHasClickAction().assertIsEnabled()
        composeRule.onNodeWithText("曝光 -").assertHasClickAction().performClick()
        composeRule.onNodeWithText("曝光 +").assertHasClickAction().performClick()
        composeRule.onNodeWithText("开灯").assertHasClickAction().performClick()
        composeRule.onNodeWithText("按建议重拍").assertIsNotEnabled()
        composeRule.onNodeWithText("控制按钮默认折叠；先定格当前画面，再按建议重拍。")
            .assertIsDisplayed()

        assertTrue(decreased)
        assertTrue(increased)
        assertTrue(torchToggled)
    }

    @Test
    fun cameraPreview_whenAdjustmentUnsupported_disablesUnavailableActions() {
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        latestAnalysis = realtimeAnalysis(),
                        adjustmentState = CameraAdjustmentState(
                            isExposureSupported = false,
                            hasFlashUnit = false,
                            message = "当前设备不支持曝光补偿或补光灯。",
                        ),
                        coachResult = PhotoCoachResult(
                            sceneStatus = PhotoSceneStatus.DARK,
                            reason = "平均亮度 70.0，画面整体偏暗。",
                            advice = "建议改善环境光。",
                            exposureDelta = +1,
                            torchAction = TorchAction.TURN_ON,
                        ),
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = {},
                    onIncreaseExposure = {},
                    onToggleTorch = {},
                    onApplySuggestedAdjustment = {},
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = {},
                    onSaveFrozenFrame = {},
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

        composeRule.onNodeWithText("不支持曝光补偿").assertIsDisplayed()
        composeRule.onNodeWithText("展开控制").assertHasClickAction().performClick()
        composeRule.onNodeWithText("当前设备不支持曝光补偿或补光灯。").assertIsDisplayed()
        composeRule.onNodeWithText("曝光 -").assertIsNotEnabled()
        composeRule.onNodeWithText("曝光 +").assertIsNotEnabled()
        composeRule.onNodeWithText("无闪光灯").assertIsNotEnabled()
        composeRule.onNodeWithText("按建议重拍").assertIsNotEnabled()
    }

    @Test
    fun frozenFrame_showsSnapshotHistogramSaveAndResumeActions() {
        var saved = false
        var resumed = false
        var retakeRequested = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        latestAnalysis = realtimeAnalysis(),
                        frozenFrame = FrozenCameraFrame(
                            bitmap = tinyBitmap(),
                            analysis = realtimeAnalysis(source = RealtimeFrameSource.PREVIEW_BITMAP),
                        ),
                        adjustmentState = adjustableCameraState(),
                        coachResult = PhotoCoachResult(
                            sceneStatus = PhotoSceneStatus.DARK,
                            reason = "平均亮度 70.0，暗部占比 65.0%，画面整体偏暗。",
                            advice = "建议小幅提高曝光补偿。",
                            exposureDelta = +1,
                            torchAction = TorchAction.KEEP,
                        ),
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = {},
                    onIncreaseExposure = {},
                    onToggleTorch = {},
                    onApplySuggestedAdjustment = { retakeRequested = true },
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = { resumed = true },
                    onSaveFrozenFrame = { saved = true },
                    previewContent = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("已定格的相机画面").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("画面状态：建议重拍").assertIsDisplayed()
        composeRule.onNodeWithText("画面质量偏差，建议按提示重拍").assertIsDisplayed()
        composeRule.onNodeWithText("按建议重拍").assertIsEnabled().performClick()
        composeRule.onNodeWithText("展开控制").assertHasClickAction().performClick()
        composeRule.onNodeWithText("定格分析").assertIsNotEnabled()
        composeRule.onNodeWithText("可按当前建议调整参数并重新定格一张，也可以继续手动微调。")
            .assertIsDisplayed()
        composeRule.onNodeWithText("保存到相册").performScrollTo().assertHasClickAction().performClick()
        composeRule.onNodeWithText("继续实时预览").performScrollTo().assertHasClickAction().performClick()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("定格画面直方图"))
        composeRule.onNodeWithText("定格画面直方图").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("定格预览图"))
        composeRule.onNodeWithText("定格预览图").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("智能拍摄建议"))
        composeRule.onNodeWithText("智能拍摄建议").assertIsDisplayed()

        assertTrue(retakeRequested)
        assertTrue(saved)
        assertTrue(resumed)
    }

    @Test
    fun frozenFrame_withNormalQualityShowsGoodBadgeAndDisablesRetake() {
        var retakeRequested = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        frozenFrame = FrozenCameraFrame(
                            bitmap = tinyBitmap(),
                            analysis = realtimeAnalysis(source = RealtimeFrameSource.PREVIEW_BITMAP),
                        ),
                        adjustmentState = adjustableCameraState(),
                        coachResult = PhotoCoachResult(
                            sceneStatus = PhotoSceneStatus.NORMAL,
                            reason = "平均亮度 128.0，整体分布较均衡。",
                            advice = "当前画面曝光较正常，可以保持现有拍摄参数。",
                            exposureDelta = 0,
                            torchAction = TorchAction.KEEP,
                        ),
                    ),
                    onBackHome = {},
                    onRequestPermission = {},
                    onCameraBindingChanged = {},
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = {},
                    onIncreaseExposure = {},
                    onToggleTorch = {},
                    onApplySuggestedAdjustment = { retakeRequested = true },
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = {},
                    onSaveFrozenFrame = {},
                    previewContent = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("画面状态：质量良好").assertIsDisplayed()
        composeRule.onNodeWithText("画面状态良好，可直接保存或继续预览").assertIsDisplayed()
        composeRule.onNodeWithText("无需重拍").assertIsNotEnabled()
        composeRule.onNodeWithText("展开控制").performClick()
        composeRule.onNodeWithText("当前画面状态良好，无需按建议重拍；可以直接保存或继续实时预览。")
            .assertIsDisplayed()

        assertTrue(!retakeRequested)
    }

    @Test
    fun cameraCoachCard_showsRuleBasedAdviceWithoutDuplicateJudgeButton() {
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                CameraScreen(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        latestAnalysis = realtimeAnalysis(),
                        frozenFrame = FrozenCameraFrame(
                            bitmap = tinyBitmap(),
                            analysis = realtimeAnalysis(source = RealtimeFrameSource.PREVIEW_BITMAP),
                        ),
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
                    onCameraAdjustmentControllerChanged = {},
                    onFrameAnalyzed = {},
                    onCameraError = {},
                    onJudgeCurrentFrame = {},
                    onDecreaseExposure = {},
                    onIncreaseExposure = {},
                    onToggleTorch = {},
                    onApplySuggestedAdjustment = {},
                    onFreezePreviewFrame = {},
                    onResumeRealtimePreview = {},
                    onSaveFrozenFrame = {},
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
        composeRule.onAllNodesWithText("智能判断当前画面").assertCountEquals(0)
        composeRule.onNodeWithText("偏暗").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("+1 档"))
        composeRule.onNodeWithText("+1 档").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("保持不变"))
        composeRule.onNodeWithText("保持不变").assertIsDisplayed()
        composeRule.onNodeWithTag(CAMERA_SCREEN_LIST_TAG)
            .performScrollToNode(hasText("判断依据：平均亮度 70.0，暗部占比 65.0%，画面整体偏暗。"))
        composeRule.onNodeWithText("判断依据：平均亮度 70.0，暗部占比 65.0%，画面整体偏暗。")
            .assertIsDisplayed()
    }

    private fun realtimeAnalysis(
        source: RealtimeFrameSource = RealtimeFrameSource.Y_PLANE,
    ): RealtimeCameraAnalysis {
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
            source = source,
        )
    }

    private fun tinyBitmap(): Bitmap =
        Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(120, 130, 140))
        }

    private fun adjustableCameraState(): CameraAdjustmentState =
        CameraAdjustmentState(
            isExposureSupported = true,
            exposureIndex = 0,
            minExposureIndex = -2,
            maxExposureIndex = 2,
            hasFlashUnit = true,
            isTorchOn = false,
        )
}
