package com.lzx.imagehistogramanalyzer.ui.analyzer

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lzx.imagehistogramanalyzer.data.image.BitmapDecoder
import com.lzx.imagehistogramanalyzer.data.image.BitmapPixelReader
import com.lzx.imagehistogramanalyzer.data.image.ImageRepository
import com.lzx.imagehistogramanalyzer.data.image.NativeBitmapHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.BaselineHistogramCalculator
import com.lzx.imagehistogramanalyzer.domain.histogram.MonotonicNanoClock
import com.lzx.imagehistogramanalyzer.domain.histogram.PreGrayscaleHistogramCalculator

class AnalyzerViewModelFactory(
    private val contentResolver: ContentResolver,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AnalyzerViewModel::class.java)) {
            "不支持的 ViewModel 类型：${modelClass.name}"
        }
        val clock = MonotonicNanoClock
        return AnalyzerViewModel(
            imageRepository = ImageRepository(BitmapDecoder(contentResolver)),
            pixelReader = BitmapPixelReader(),
            histogramCalculators = listOf(
                PreGrayscaleHistogramCalculator(clock = clock),
                BaselineHistogramCalculator(clock = clock),
            ),
            nativeCalculator = NativeBitmapHistogramCalculator(clock = clock),
            clock = clock,
        ) as T
    }
}
