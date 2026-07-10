package com.lzx.imagehistogramanalyzer.domain.camera

import com.lzx.imagehistogramanalyzer.domain.photo.PhotoCoachResult

/** 曝光补偿和补光灯控制抽象，方便 ViewModel 测试时替换 CameraX 实现。 */
interface CameraAdjustmentController {
    fun currentState(): CameraAdjustmentState

    suspend fun adjustExposureBy(delta: Int): CameraAdjustmentState

    suspend fun toggleTorch(): CameraAdjustmentState

    suspend fun applySuggestion(coachResult: PhotoCoachResult): CameraAdjustmentState
}
