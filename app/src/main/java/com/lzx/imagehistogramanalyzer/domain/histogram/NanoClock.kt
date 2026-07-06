package com.lzx.imagehistogramanalyzer.domain.histogram

/** 单调纳秒时钟；允许测试注入确定性时间，不依赖 Android Framework。 */
fun interface NanoClock {
    fun nowNanos(): Long
}

/** JVM 与 Android 均可使用的单调时钟，只在性能阶段边界调用。 */
object MonotonicNanoClock : NanoClock {
    override fun nowNanos(): Long = System.nanoTime()
}
