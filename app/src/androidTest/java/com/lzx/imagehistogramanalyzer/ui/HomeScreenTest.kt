package com.lzx.imagehistogramanalyzer.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.lzx.imagehistogramanalyzer.ui.home.HomeScreen
import com.lzx.imagehistogramanalyzer.ui.theme.ImageHistogramAnalyzerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_showsRealPickerActionAndProductCapabilities() {
        var pickerOpened = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                HomeScreen(
                    hasSelectedImage = false,
                    onPickImage = { pickerOpened = true },
                    onResumeAnalysis = {},
                )
            }
        }

        composeRule.onNodeWithText("选择图片")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("标准灰度直方图").assertIsDisplayed()
        composeRule.onNodeWithText("图像质量判断").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("方案与性能对比").performScrollTo().assertIsDisplayed()

        assertTrue(pickerOpened)
    }

    @Test
    fun home_withExistingImage_canResumeAnalysis() {
        var resumed = false
        composeRule.setContent {
            ImageHistogramAnalyzerTheme {
                HomeScreen(
                    hasSelectedImage = true,
                    onPickImage = {},
                    onResumeAnalysis = { resumed = true },
                )
            }
        }

        composeRule.onNodeWithText("继续查看分析")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(resumed)
    }
}
