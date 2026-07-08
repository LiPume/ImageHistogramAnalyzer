package com.lzx.imagehistogramanalyzer.ui.analyzer

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lzx.imagehistogramanalyzer.data.image.BitmapPixelReader
import com.lzx.imagehistogramanalyzer.data.image.DecodedImage
import com.lzx.imagehistogramanalyzer.data.image.ImageDecodeException
import com.lzx.imagehistogramanalyzer.data.image.ImageLoader
import com.lzx.imagehistogramanalyzer.data.image.ImageOpenException
import com.lzx.imagehistogramanalyzer.data.image.ImageTooLargeException
import com.lzx.imagehistogramanalyzer.domain.histogram.BaselineHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.PreGrayscaleHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.model.ImageMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalyzerViewModelTest {
    @Test
    fun oversizedImage_stopsLoadingAndShowsLimit() = runBlocking {
        val viewModel = createViewModel(
            ImageLoader { throw ImageTooLargeException(20_000_000, 16_000_000) },
        )

        viewModel.selectImage(TEST_URI)
        val state = viewModel.awaitErrorState()

        assertFalse(state.isProcessing)
        assertEquals(
            "图片约 20.0 百万像素，超过 MVP 的 16 百万像素安全上限",
            state.errorMessage,
        )
        assertNull(state.image)
    }

    @Test
    fun damagedImage_stopsLoadingAndShowsRecoverableMessage() = runBlocking {
        val viewModel = createViewModel(ImageLoader { throw ImageDecodeException() })

        viewModel.selectImage(TEST_URI)
        val state = viewModel.awaitErrorState()

        assertFalse(state.isProcessing)
        assertEquals(
            "图片文件可能已损坏或格式不受支持，请重新选择",
            state.errorMessage,
        )
    }

    @Test
    fun unreadableImage_stopsLoadingAndShowsReselectMessage() = runBlocking {
        val viewModel = createViewModel(ImageLoader { throw ImageOpenException() })

        viewModel.selectImage(TEST_URI)
        val state = viewModel.awaitErrorState()

        assertFalse(state.isProcessing)
        assertEquals("无法打开所选图片，请重新选择", state.errorMessage)
    }

    @Test
    fun replacementFailure_keepsPreviousSuccessfulImageAndMetadata() = runBlocking {
        val originalBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val originalMetadata = ImageMetadata(
            displayName = "original.png",
            mimeType = "image/png",
            width = 2,
            height = 2,
        )
        var loadCount = 0
        val viewModel = createViewModel(
            ImageLoader {
                loadCount += 1
                if (loadCount == 1) {
                    DecodedImage(originalBitmap, originalMetadata)
                } else {
                    throw ImageDecodeException()
                }
            },
        )

        viewModel.selectImage(Uri.parse("content://test/original"))
        withTimeout(2_000) {
            viewModel.uiState.first { !it.isProcessing && it.image != null }
        }

        viewModel.selectImage(Uri.parse("content://test/damaged-replacement"))
        val state = viewModel.awaitErrorState()

        assertSame(originalBitmap, state.image)
        assertEquals(originalMetadata, state.metadata)
        assertEquals(
            "图片文件可能已损坏或格式不受支持，请重新选择",
            state.errorMessage,
        )
    }

    private suspend fun AnalyzerViewModel.awaitErrorState(): AnalyzerUiState = withTimeout(2_000) {
        uiState.first { !it.isProcessing && it.errorMessage != null }
    }

    private fun createViewModel(imageLoader: ImageLoader) = AnalyzerViewModel(
        imageLoader = imageLoader,
        pixelReader = BitmapPixelReader(),
        histogramCalculators = listOf(
            PreGrayscaleHistogramCalculator(),
            BaselineHistogramCalculator(),
        ),
    )

    private companion object {
        val TEST_URI: Uri = Uri.parse("content://test/image")
    }
}
