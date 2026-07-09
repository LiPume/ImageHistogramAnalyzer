package com.lzx.imagehistogramanalyzer.domain.photo

/** 实时拍摄建议结果；只给出本地规则建议，不直接修改相机参数。 */
data class PhotoCoachResult(
    val sceneStatus: PhotoSceneStatus,
    val reason: String,
    val advice: String,
    val exposureDelta: Int,
    val torchAction: TorchAction,
)

enum class PhotoSceneStatus {
    SEVERE_UNDEREXPOSED,
    DARK,
    SLIGHTLY_DARK,
    SEVERE_OVEREXPOSED,
    BRIGHT,
    SLIGHTLY_BRIGHT,
    LOW_CONTRAST,
    NORMAL,
}

enum class TorchAction {
    KEEP,
    TURN_ON,
    TURN_OFF,
}
