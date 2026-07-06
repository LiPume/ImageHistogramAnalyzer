# Project contract

## Product and platform

- Project: 基于 Android 的移动端图像质量分析与直方图计算优化系统.
- Package: `com.lzx.imagehistogramanalyzer`.
- Planned UI: Kotlin + Jetpack Compose + Material 3.
- Current SDK contract: minSdk 23, targetSdk 36, compileSdk 36.1.
- Required output: grayscale distribution with 256 bins and normalized height 0..100, shown as a black-and-white histogram.
- Performance goal: primary histogram computation below 300ms on documented real-device tests.

Confirm the actual Gradle configuration before changing version numbers. Do not replace working local versions with copied examples from an external skill.

## Scope

In scope:

- System image selection, URI handling, preview, image dimensions, and pixel count.
- Grayscale histogram calculation, normalization, Canvas display, and timing.
- Mean gray, dark/bright ratios, standard deviation, and rule-based quality labels.
- Baseline/Optimized equality and performance comparison.
- Unit/UI/instrumented tests, reports, usage instructions, and defense material.

Out of scope unless the user explicitly changes requirements:

- Login, registration, account management, database, backend, cloud storage, networking, social sharing.
- Camera capture, editing suite, machine learning, Room, Retrofit, Hilt, Navigation graphs, and production-style multi-module architecture.

## Package ownership

```text
com.lzx.imagehistogramanalyzer/
├── MainActivity.kt                 # thin app entry
├── data/image/                     # ContentResolver, Bitmap decoding/pixel adapter
├── domain/model/                   # immutable result models
├── domain/histogram/               # grayscale, bins, normalization strategies
├── domain/quality/                 # metrics derived from histogram
├── domain/benchmark/               # fair comparison and timing summaries
└── ui/
    ├── analyzer/                   # ViewModel, UiState, screen orchestration
    ├── component/                  # stateless cards and HistogramCanvas
    └── theme/                      # Compose theme
```

Dependencies flow from UI orchestration toward models/computation and the image adapter. Do not let the domain computation import Compose. Keep Android-only decoding concerns out of mathematical functions.

## Fixed algorithm semantics

- Formula: `gray = red * 0.299 + green * 0.587 + blue * 0.114`.
- Default rounding decision: round to the nearest integer and clamp to `0..255`; change only if the course teacher specifies a different rule, then update all tests and documents.
- Keep 256 raw counts. Their sum equals the number of analyzed pixels.
- Normalize each height proportionally to the maximum bin; the maximum non-empty bin maps to 100.
- Define transparent-pixel handling once and test it. Do not let Baseline and Optimized use different rules.
- Canvas uses logical coordinates 256×100 and does not calculate source pixels.

## External-skill decisions

The upstream `awesome-android-agent-skills` repository is useful for Compose state hoisting, lifecycle-aware flows, structured concurrency, accessibility, testing layers, and profiling discipline.

Do not inherit these upstream defaults:

- Hilt for every dependency: manual constructor injection is enough here.
- Room/Retrofit/offline-first: there is no persistence or network scope.
- Multi-module convention plugins: the course app is intentionally a small single module.
- Navigation Compose: the planned MVP is one main workflow.
- Copied SDK/dependency versions: use this repository's tested Version Catalog and Gradle setup.
- Screenshot frameworks or immutable-collection libraries before a demonstrated need.
