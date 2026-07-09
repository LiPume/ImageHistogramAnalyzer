package com.lzx.imagehistogramanalyzer.ui.camera

import com.lzx.imagehistogramanalyzer.domain.camera.RealtimeCameraAnalysis

data class CameraUiState(
    val hasCameraPermission: Boolean = false,
    val isBindingCamera: Boolean = false,
    val latestAnalysis: RealtimeCameraAnalysis? = null,
    val errorMessage: String? = null,
)
