package com.lzx.imagehistogramanalyzer.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lzx.imagehistogramanalyzer.domain.model.HistogramResult
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerScreen
import com.lzx.imagehistogramanalyzer.ui.analyzer.AnalyzerUiState
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
                    onPickImage = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("从相册选择图片")
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
                        decodeTimeNanos = 2_000_000,
                        calculationTimeNanos = 10_000_000,
                    ),
                    onPickImage = {},
                )
            }
        }

        composeRule.onNodeWithText("灰度直方图").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("256 个灰度等级的黑白直方图")
            .assertIsDisplayed()
        composeRule.onNodeWithText("已达到核心计算 300ms 目标").assertIsDisplayed()
    }
}
