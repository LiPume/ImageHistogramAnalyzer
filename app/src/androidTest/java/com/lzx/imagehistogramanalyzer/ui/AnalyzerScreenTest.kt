package com.lzx.imagehistogramanalyzer.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.moveBy
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.color.RgbHistogramAnalyzer
import com.lzx.imagehistogramanalyzer.domain.insight.ImageInsightAnalyzer
import com.lzx.imagehistogramanalyzer.domain.model.HistogramExecutionEngine
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewImageLayout
import com.lzx.imagehistogramanalyzer.domain.roi.PreviewRect
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerScreen
import com.lzx.imagehistogramanalyzer.ui.analyzer.ANALYZER_LIST_TEST_TAG
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerUiState
import com.lzx.imagehistogramanalyzer.ui.component.PerformanceCard
import com.lzx.imagehistogramanalyzer.ui.component.ROI_PREVIEW_TEST_TAG
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyzerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_showsImagePickerAndHandlesClick() {
        var clicked = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(),
                    onBackHome = {},
                    onPickImage = { clicked = true },
                    onSelectStrategy = {},
                    onCalculate = {},
                )
            }
        }

        composeRule.onNodeWithText("选择图片")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun histogramState_showsCanvasAndPerformanceResult() {
        val counts = IntArray(256).apply { this[128] = 10 }
        val heights = IntArray(256).apply { this[128] = 100 }
        val histogram = HistogramResult(
            counts = counts,
            normalizedHeights = heights,
            pixelCount = 10,
            maxCount = 10,
        )
        val qualityResult = ImageQualityResult(
            meanGray = 128.0,
            darkRatio = 0.1,
            brightRatio = 0.2,
            standardDeviation = 42.0,
            category = ImageQualityCategory.NORMAL,
        )
        val rgbStats = RgbHistogramAnalyzer().analyze(IntArray(10) { argb(128, 128, 128) })
        val insight = ImageInsightAnalyzer().analyze(
            histogram = histogram,
            quality = qualityResult,
            rgbStats = rgbStats,
        )
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(
                        histogram = histogram,
                        qualityResult = qualityResult,
                        rgbStats = rgbStats,
                        imageInsight = insight,
                        decodeTimeNanos = 2_000_000,
                        performanceMetrics = HistogramPerformanceMetrics(
                            pixelReadNanos = 1_000_000,
                            grayscaleConversionNanos = null,
                            countingNanos = 7_000_000,
                            normalizationNanos = 1_000_000,
                            coreTotalNanos = 10_000_000,
                            executionEngine = HistogramExecutionEngine.NATIVE_V3,
                            workerCount = 8,
                        ),
                        selectedStrategy = HistogramCalculationStrategy.GRAYSCALE_WHILE_COUNTING,
                    ),
                    onBackHome = {},
                    onPickImage = {},
                    onSelectStrategy = {},
                    onCalculate = {},
                )
            }
        }

        composeRule.onNodeWithText("灰度直方图").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("256 个灰度等级的黑白直方图")
            .assertIsDisplayed()
        composeRule.onNodeWithText("RGB 三通道")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithContentDescription(
            "RGB 三通道直方图，R 均值 128.0，G 均值 128.0，B 均值 128.0",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("通道均值：R 128.0 · G 128.0 · B 128.0")
            .assertIsDisplayed()
        composeRule.onNodeWithText("图像质量分析")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("正常").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("128.00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "暗部 10.0%，中间调 70.0%，亮部 20.0%",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("综合分析")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("综合判断：图像亮度状态为正常，色彩较均衡。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ANALYZER_LIST_TEST_TAG).performScrollToIndex(4)
        composeRule.onNodeWithText("已达到核心计算 300ms 目标")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("像素访问/复制").assertIsDisplayed()
        composeRule.onNodeWithText("Native 多线程 v3.0")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("8")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("灰度转换 + 频次统计（融合）").assertIsDisplayed()
        composeRule.onNodeWithText("多线程结果合并").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "核心计算 10.000 毫秒，占 300 毫秒性能预算的 3.3%",
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectedImage_requiresStrategyBeforeCalculation() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        var uiState by mutableStateOf(
            AnalyzerUiState(
                image = bitmap,
                metadata = ImageMetadata(
                    displayName = "test.jpg",
                    mimeType = "image/jpeg",
                    width = 2,
                    height = 2,
                ),
            ),
        )
        var calculated = false

        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = uiState,
                    onBackHome = {},
                    onPickImage = {},
                    onSelectStrategy = { strategy ->
                        uiState = uiState.copy(selectedStrategy = strategy)
                    },
                    onCalculate = { calculated = true },
                )
            }
        }

        composeRule.onNodeWithText("计算并绘制直方图")
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText("优先灰度化")
            .performScrollTo()
            .performClick()

        assertTrue(uiState.selectedStrategy == HistogramCalculationStrategy.PRE_GRAYSCALE)
        composeRule.onNodeWithText("计算并绘制直方图")
            .assertIsEnabled()
            .performClick()
        assertTrue(calculated)
    }

    @Test
    fun roiSelectionMode_dragConfirmSendsPreviewRectAndLayout() {
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        var uiState by mutableStateOf(
            AnalyzerUiState(
                image = bitmap,
                metadata = ImageMetadata(
                    displayName = "roi.jpg",
                    mimeType = "image/jpeg",
                    width = 80,
                    height = 60,
                ),
                selectedStrategy = HistogramCalculationStrategy.PRE_GRAYSCALE,
            ),
        )
        var confirmedRect: PreviewRect? = null
        var confirmedLayout: PreviewImageLayout? = null

        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = uiState,
                    onBackHome = {},
                    onPickImage = {},
                    onSelectStrategy = {},
                    onCalculate = {},
                    onStartRoiSelection = {
                        uiState = uiState.copy(isRoiSelectionMode = true)
                    },
                    onCancelRoiSelection = {
                        uiState = uiState.copy(isRoiSelectionMode = false)
                    },
                    onConfirmRoiSelection = { rect, layout ->
                        confirmedRect = rect
                        confirmedLayout = layout
                    },
                    onRestoreFullImage = {},
                )
            }
        }

        composeRule.onNodeWithText("选择局部分析区域")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("确认区域并重新分析")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(ROI_PREVIEW_TEST_TAG)
            .performTouchInput {
                down(center)
                moveBy(Offset(48f, 36f))
                up()
            }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("确认区域并重新分析")
            .assertIsEnabled()
            .performClick()

        assertNotNull(confirmedRect)
        assertNotNull(confirmedLayout)
        assertEquals(80, confirmedLayout!!.bitmapWidth)
        assertEquals(60, confirmedLayout!!.bitmapHeight)
        assertTrue(confirmedRect!!.right > confirmedRect!!.left)
        assertTrue(confirmedRect!!.bottom > confirmedRect!!.top)
    }

    @Test
    fun preGrayscalePerformance_showsSeparateStageTimings() {
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                PerformanceCard(
                    strategy = HistogramCalculationStrategy.PRE_GRAYSCALE,
                    decodeTimeNanos = 1_000_000,
                    metrics = HistogramPerformanceMetrics(
                        pixelReadNanos = 2_000_000,
                        grayscaleConversionNanos = 3_000_000,
                        countingNanos = 4_000_000,
                        normalizationNanos = 1_000_000,
                        coreTotalNanos = 11_000_000,
                    ),
                )
            }
        }

        composeRule.onNodeWithText("像素访问/复制").assertIsDisplayed()
        composeRule.onNodeWithText("灰度转换").assertIsDisplayed()
        composeRule.onNodeWithText("频次统计").assertIsDisplayed()
        composeRule.onNodeWithText("其他分配/调度开销").assertIsDisplayed()
        composeRule.onNodeWithText("核心计算").assertIsDisplayed()
    }

    @Test
    fun analyzerTopBar_returnsHome() {
        var returnedHome = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(),
                    onBackHome = { returnedHome = true },
                    onPickImage = {},
                    onSelectStrategy = {},
                    onCalculate = {},
                )
            }
        }

        composeRule.onNodeWithText("返回首页")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(returnedHome)
    }

    @Test
    fun loadingState_showsProgressAndDisablesPicker() {
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(isProcessing = true),
                    onBackHome = {},
                    onPickImage = {},
                    onSelectStrategy = {},
                    onCalculate = {},
                )
            }
        }

        composeRule.onNodeWithText("正在读取图片或计算直方图…")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("选择图片").assertIsNotEnabled()
    }

    @Test
    fun errorState_showsMessageAndAllowsReselect() {
        var reselected = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(
                        errorMessage = "图片文件可能已损坏或格式不受支持，请重新选择",
                    ),
                    onBackHome = {},
                    onPickImage = { reselected = true },
                    onSelectStrategy = {},
                    onCalculate = {},
                )
            }
        }

        composeRule.onNodeWithText("处理失败").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("图片文件可能已损坏或格式不受支持，请重新选择")
            .assertIsDisplayed()
        composeRule.onNodeWithText("选择图片").performClick()
        assertTrue(reselected)
    }

    @Test
    fun existingImage_showsReselectAction() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        var reselected = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(
                        image = bitmap,
                        metadata = ImageMetadata(
                            displayName = "existing.jpg",
                            mimeType = "image/jpeg",
                            width = 2,
                            height = 2,
                        ),
                    ),
                    onBackHome = {},
                    onPickImage = { reselected = true },
                    onSelectStrategy = {},
                    onCalculate = {},
                )
            }
        }

        composeRule.onNodeWithText("重新选择图片")
            .assertHasClickAction()
            .performClick()
        assertTrue(reselected)
    }

    private fun argb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
