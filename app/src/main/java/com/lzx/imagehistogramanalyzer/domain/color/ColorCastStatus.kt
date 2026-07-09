package com.lzx.imagehistogramanalyzer.domain.color

/** 基于三通道均值差异得到的偏色判断。 */
enum class ColorCastStatus {
    /** 三通道均值接近，未发现明显偏色。 */
    BALANCED,

    /** 通道差异达到轻微偏色区间。 */
    SLIGHT_RED,
    SLIGHT_GREEN,
    SLIGHT_BLUE,

    /** 通道差异明显，画面可能存在对应颜色偏移。 */
    RED_CAST,
    GREEN_CAST,
    BLUE_CAST,
}
