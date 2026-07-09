package com.lzx.imagehistogramanalyzer.data.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeLumaHistogramAnalyzer
import com.lzx.imagehistogramanalyzer.domain.histogram.MonotonicNanoClock
import com.lzx.imagehistogramanalyzer.domain.histogram.NanoClock

/** CameraX ImageAnalysis 适配器：节流读取 Y 通道并交给纯 Kotlin 分析器。 */
class RealtimeFrameAnalyzer(
    private val lumaAnalyzer: RealtimeLumaHistogramAnalyzer = RealtimeLumaHistogramAnalyzer(),
    private val minIntervalNanos: Long = DEFAULT_INTERVAL_NANOS,
    private val clock: NanoClock = MonotonicNanoClock,
    private val onAnalyzed: (RealtimeCameraAnalysis) -> Unit,
    private val onError: (Throwable) -> Unit,
) : ImageAnalysis.Analyzer {
    private var lastAnalyzedNanos: Long = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = clock.nowNanos()
            if (now - lastAnalyzedNanos < minIntervalNanos) return
            lastAnalyzedNanos = now

            val yPlane = image.planes.firstOrNull()
                ?: error("实时分析缺少 Y 通道")
            val buffer = yPlane.buffer
            buffer.rewind()
            val lumaBytes = ByteArray(buffer.remaining())
            buffer.get(lumaBytes)

            val result = lumaAnalyzer.analyze(
                lumaBytes = lumaBytes,
                width = image.width,
                height = image.height,
                rowStride = yPlane.rowStride,
                pixelStride = yPlane.pixelStride,
                analyzedAtNanos = now,
            )
            onAnalyzed(result)
        } catch (error: Throwable) {
            onError(error)
        } finally {
            image.close()
        }
    }

    companion object {
        /** 实时预览不追求每帧都算，约 400ms 更新一次即可稳定展示趋势。 */
        const val DEFAULT_INTERVAL_NANOS: Long = 400_000_000L
    }
}
