package com.lzx.imagehistogramanalyzer.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.lzx.imagehistogramanalyzer.domain.histogram.HistogramCalculationStrategy
import com.lzx.imagehistogramanalyzer.domain.model.HistogramExecutionEngine
import com.lzx.imagehistogramanalyzer.domain.model.HistogramPerformanceMetrics
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityCategory
import com.lzx.imagehistogramanalyzer.domain.model.ImageQualityResult
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerScreen
import com.lzx.imagehistogramanalyzer.ui.analyzer.ANALYZER_LIST_TEST_TAG
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerUiState
import com.lzx.imagehistogramanalyzer.ui.component.PerformanceCard
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme
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
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                AnalyzerScreen(
                    uiState = AnalyzerUiState(
                        histogram = HistogramResult(
                            counts = counts,
                            normalizedHeights = heights,
                            pixelCount = 10,
                            maxCount = 10,
                        ),
                        qualityResult = ImageQualityResult(
                            meanGray = 128.0,
                            darkRatio = 0.1,
                            brightRatio = 0.2,
                            standardDeviation = 42.0,
                            category = ImageQualityCategory.NORMAL,
                        ),
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
        composeRule.onNodeWithText("图像质量分析")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("正常").assertIsDisplayed()
        composeRule.onNodeWithText("128.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "暗部 10.0%，中间调 70.0%，亮部 20.0%",
        ).performScrollTo().assertIsDisplayed()
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
}
