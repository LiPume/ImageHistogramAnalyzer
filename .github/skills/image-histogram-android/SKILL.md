---
name: image-histogram-android
description: Build, review, test, or optimize the ImageHistogramAnalyzer Android course project. Use whenever changing this repository's Gradle configuration, Kotlin/Jetpack Compose UI, image selection or Bitmap decoding, grayscale histogram and quality-analysis logic, coroutines, tests, performance measurements, TODO status, or technical documentation.
---

# Image Histogram Android

Implement this course project as a small, testable Android app with transparent correctness and performance evidence.

## Start every task

1. Read `docs/迭代计划与TODO.md` and identify one task ID.
2. Inspect `git status`, relevant source, Gradle files, and existing tests before editing.
3. Read [project-contract.md](references/project-contract.md) before architecture, dependency, algorithm, or scope decisions.
4. Keep the change limited to the selected task. Preserve unrelated user changes.
5. Update the matching TODO and technical document in the same change.

## Keep the architecture proportional

- Keep one `app` module unless measured build or ownership problems justify another module.
- Keep `MainActivity` as a thin Compose entry point.
- Use `ui/analyzer` for screen state and orchestration, `ui/component` for stateless UI, `data/image` for Android image access, and `domain` for models and computation.
- Keep the histogram math independent from Compose and ViewModel. Isolate Android `Bitmap` access behind the image-processing boundary when practical.
- Pass immutable result models from computation to UI. Make Canvas consume normalized bins only.
- Prefer constructor injection. Do not introduce Hilt, Room, Retrofit, Navigation, a database, a backend, or multi-module build logic without an explicit requirement.

## Protect histogram correctness

- Use exactly 256 grayscale bins and the required formula: `gray = red * 0.299 + green * 0.587 + blue * 0.114`.
- Follow the documented rounding and alpha rules consistently in every implementation and test.
- Normalize bin heights to integer values from 0 through 100; handle zero and malformed input explicitly.
- Require Baseline and Optimized implementations to produce identical bins before comparing speed.
- Derive quality metrics from histogram bins instead of scanning the Bitmap again.
- Add deterministic tests before or with algorithm changes. Include black, white, primary colors, known small matrices, zero/max normalization, and algorithm equality.

## Handle Android images safely

- Use the system photo picker or Activity Result API and `ContentResolver`; do not resolve filesystem paths or request broad storage permission.
- Separate preview sizing from analysis semantics. Never silently downsample an image and label the result as full-image analysis.
- Read dimensions before allocating large buffers. Reuse chunk buffers and keep only the current image/result alive.
- Cancel obsolete work when a new image is selected, and reject stale results in UI state.

## Use Compose and coroutines deliberately

- Hoist screen state into `AnalyzerViewModel` and expose read-only `StateFlow` collected with lifecycle awareness.
- Keep reusable Composables stateless with `modifier: Modifier = Modifier` and event callbacks.
- Do not decode images, traverse pixels, normalize data, or update state inside composition or Canvas drawing.
- Run decode work on an IO dispatcher and CPU pixel work on a Default dispatcher through injectable/defaulted dependencies.
- Use structured concurrency and rethrow `CancellationException`. Check cancellation at chunk boundaries, not on every pixel unless profiling proves it acceptable.
- Add meaningful accessibility descriptions, 48dp touch targets, and Material theme colors; decorative images use a null description.

## Measure before optimizing

- Report decode, core computation, and rendering separately. Treat grayscale counting plus normalization as the primary 300ms metric unless the course definition changes.
- Use a monotonic nanosecond clock, convert only for display, warm up, repeat, and report median plus worst/percentile data.
- Measure final claims on a real device with a Release or benchmarkable build; record device, OS, image dimensions, build type, run count, and algorithm version.
- Optimize the measured bottleneck. Prefer bulk/chunked `getPixels()` and buffer reuse before NDK or parallelism.
- Never claim an optimization when outputs differ or measurements are not reproducible.

## Verify and document

Read [verification.md](references/verification.md) for the applicable test matrix and commands.

Before completing a change:

1. Run the narrowest relevant tests, then `./gradlew test` and `./gradlew assembleDebug` when configuration permits.
2. Check that ignored build, local, and external-reference files are not staged.
3. Update `docs/迭代计划与TODO.md` and any affected design, test, or usage document.
4. Summarize changed files, verification results, remaining risk, and the next task ID.
5. Use focused commit messages such as `feat:`, `test:`, `perf:`, `docs:`, or `build:`; keep `main` runnable.

## Reference routing

- Read [project-contract.md](references/project-contract.md) for fixed scope, SDK, package structure, algorithm semantics, and rejected upstream defaults.
- Read [verification.md](references/verification.md) before algorithm, image-decoding, UI, performance, or release verification.
- Treat `references/open_source/awesome-android-agent-skills/` as research only. Do not copy its skills wholesale or reintroduce networking/database/Hilt requirements.
