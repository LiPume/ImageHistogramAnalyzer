package com.lzx.imagehistogramanalyzer.ui.camera

import androidx.lifecycle.ViewModel
import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CameraViewModel : ViewModel() {
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

    fun onCameraError(message: String) {
        _uiState.update { current ->
            current.copy(
                isBindingCamera = false,
                errorMessage = message,
            )
        }
    }
}
