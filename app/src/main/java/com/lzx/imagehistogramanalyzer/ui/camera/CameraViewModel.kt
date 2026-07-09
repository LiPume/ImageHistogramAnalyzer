package com.lzx.imagehistogramanalyzer.ui.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.imagehistogramanalyzer.data.camera.CameraSnapshotSaver
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import com.lzx.imagehistogramanalyzer.domain.camera.PreviewSnapshotHistogramAnalyzer
import com.lzx.imagehistogramanalyzer.domain.histogram.MonotonicNanoClock
import com.lzx.imagehistogramanalyzer.domain.histogram.NanoClock
import com.lzx.imagehistogramanalyzer.domain.photo.RuleBasedPhotoCoach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraViewModel(
    private val photoCoach: RuleBasedPhotoCoach = RuleBasedPhotoCoach(),
    private val snapshotAnalyzer: PreviewSnapshotHistogramAnalyzer = PreviewSnapshotHistogramAnalyzer(),
    private val clock: NanoClock = MonotonicNanoClock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun setCameraPermission(granted: Boolean) {
        _uiState.update { current ->
            current.copy(
                hasCameraPermission = granted,
                errorMessage = if (granted) null else current.errorMessage,
            )
        }
    }

    fun setBindingCamera(binding: Boolean) {
        _uiState.update { current ->
            current.copy(isBindingCamera = binding)
        }
    }

    fun onFrameAnalyzed(result: RealtimeCameraAnalysis) {
        _uiState.update { current ->
            current.copy(
                isBindingCamera = false,
                latestAnalysis = result,
                errorMessage = null,
            )
        }
    }

    fun judgeCurrentFrame() {
        _uiState.update { current ->
            val analysis = current.frozenFrame?.analysis ?: current.latestAnalysis ?: return@update current
            current.copy(coachResult = photoCoach.analyze(analysis))
        }
    }

    fun freezePreviewFrame(bitmap: Bitmap) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val stableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    val pixels = IntArray(stableBitmap.width * stableBitmap.height)
                    stableBitmap.getPixels(
                        pixels,
                        0,
                        stableBitmap.width,
                        0,
                        0,
                        stableBitmap.width,
                        stableBitmap.height,
                    )
                    val analysis = snapshotAnalyzer.analyze(
                        pixels = pixels,
                        width = stableBitmap.width,
                        height = stableBitmap.height,
                        analyzedAtNanos = clock.nowNanos(),
                    )
                    stableBitmap to analysis
                }
            }.onSuccess { (stableBitmap, analysis) ->
                _uiState.update { current ->
                    current.copy(
                        frozenFrame = FrozenCameraFrame(
                            bitmap = stableBitmap,
                            analysis = analysis,
                        ),
                        coachResult = photoCoach.analyze(analysis),
                        snapshotMessage = "已定格当前画面，下面的直方图和建议均基于该画面。",
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                onCameraError(error.message ?: "定格预览画面失败，请稍后重试")
            }
        }
    }

    fun resumeRealtimePreview() {
        _uiState.update { current ->
            current.copy(
                frozenFrame = null,
                coachResult = null,
                isSavingFrozenFrame = false,
                snapshotMessage = null,
                errorMessage = null,
            )
        }
    }

    fun saveFrozenFrame(context: Context) {
        val bitmap = _uiState.value.frozenFrame?.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isSavingFrozenFrame = true,
                    snapshotMessage = "正在保存到系统相册…",
                    errorMessage = null,
                )
            }
            runCatching {
                CameraSnapshotSaver(context.applicationContext).save(bitmap)
            }.onSuccess { uri ->
                _uiState.update { current ->
                    current.copy(
                        frozenFrame = current.frozenFrame?.copy(savedUri = uri),
                        isSavingFrozenFrame = false,
                        snapshotMessage = "已保存到系统相册。",
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { current ->
                    current.copy(
                        isSavingFrozenFrame = false,
                        errorMessage = "保存到相册失败：${error.message ?: "未知错误"}",
                    )
                }
            }
        }
    }

    fun onCameraError(message: String) {
        _uiState.update { current ->
            current.copy(
                isBindingCamera = false,
                isSavingFrozenFrame = false,
                errorMessage = message,
            )
        }
    }
}
