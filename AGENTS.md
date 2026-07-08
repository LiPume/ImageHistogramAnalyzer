# ImageHistogramAnalyzer agent instructions

## Project profile

- Android native single-module app (`:app`), Kotlin, Jetpack Compose, Material 3, ViewModel + StateFlow.
- UI entry: `MainActivity.kt` → `ui/ImageHistogramApp.kt`; analyzer content lives under `ui/analyzer/`, reusable display components under `ui/component/`.
- There is no XML screen, Navigation Compose, Room, Retrofit, Hilt, Koin, database, backend, or cloud service. Do not introduce them for UI-only work.
- The app has a two-level flow (home → analyzer). Keep navigation as small saveable UI state unless the flow genuinely grows beyond this scope.

Use `.github/skills/image-histogram-android/SKILL.md` for Android, Gradle, Compose, image-processing, histogram, performance, testing, and project-documentation work in this repository.

Supporting workflow skills:

- `.github/skills/android-test-gate/SKILL.md`: require proportional tests after each meaningful code slice.
- `.github/skills/project-doc-sync/SKILL.md`: synchronize TODO, design, testing, and usage documents from evidence.
- `.github/skills/project-git-checkpoint/SKILL.md`: create safe, tested, authorized Git checkpoints at logical boundaries.
- `.github/skills/course-presentation/SKILL.md`: create and verify the project classroom acceptance PPTX from traceable code, test, document, and screenshot evidence.

Before editing:

1. Read `docs/迭代计划与TODO.md` and select one task ID.
2. Inspect Git status and relevant files.
3. Keep the single-app architecture and course scope unless the user explicitly changes them.

## UI and Compose rules

- Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `MaterialTheme.shapes`; new screen code must not hardcode colors, arbitrary text sizes, or repeated corner radii.
- Preserve the selected B “Sky & Citrus” identity: blue primary actions, mint quality accents, and citrus performance accents. The app intentionally uses a fixed light theme, even when the device uses dark mode.
- Use an 8dp-based spacing scale. Interactive targets must be at least 48dp. Apply `Scaffold` padding and system insets correctly.
- Screen state flows down and callbacks flow up. Keep expensive decoding, histogram work, quality analysis, and formatting out of composable bodies.
- Keep previewable components stateless and parameter-driven. Add app-themed light previews to new reusable visual components when practical.
- Use `remember` only for values that need to survive recomposition; use `derivedStateOf` only when inputs change more often than the derived result. Do not add either as decoration.
- Static, heterogeneous screen sections may use `LazyColumn`; domain lists require stable keys, while one-off static sections do not need artificial keys.
- Give meaningful images/actions clear semantics, mark section headings, expose custom chart meaning through `contentDescription`, and retain exact numeric values beside visual summaries.
- Prefer tonal surface hierarchy over shadows. Motion must be brief, interruptible, and optional; this project does not need decorative animation.

## Product UI rules

- Button labels must describe the immediate operation. Opening the system picker is “选择图片”, not “开始分析”.
- The home page must explain local image selection/camera availability, two required algorithms, quality indicators, and the 300ms core-computation target.
- The analyzer must always expose a clear return-to-home path without discarding an already selected image.
- Visualizations must answer a concrete question. Use only the quality tone distribution and 300ms budget progress unless another chart has a documented decision purpose.
- Every major screen must represent initial, loading, success, and error states; never leave an unexplained blank area.

## Testing and build gate

- Follow `.github/skills/android-test-gate/SKILL.md` after each meaningful Kotlin, Compose, resource, Manifest, or Gradle slice.
- Prefer the smallest test that proves the contract: JVM test for pure logic; stateless Compose test for text, state branches, semantics, and callback wiring; integration test only for Activity/lifecycle boundaries.
- Compose tests use visible text, roles, state descriptions, and test tags only where off-screen lazy content requires scrolling. Do not test private implementation details.
- Minimum UI verification: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`.
- When an emulator/device is available, also run `./gradlew :app:connectedDebugAndroidTest`. Report skipped commands and the exact reason; never claim unrun tests passed.

## Git checkpoints and final handoff

- Before editing, record `git status --short --branch`. Do not overwrite user changes or stage `test/` screenshots.
- After a verified TODO-sized change, synchronize `docs/迭代计划与TODO.md` and affected design/test/usage docs, then use `.github/skills/project-git-checkpoint/SKILL.md`.
- Stage explicit paths only. Never stage `build/`, `.gradle/`, `local.properties`, signing material, or `references/open_source/`.
- Final reports must list changed files, user-visible behavior, Material 3/Compose/accessibility audit results, commands actually run, remaining risks, and the next real-device checks.

Non-negotiable boundaries:

- Do not add login, registration, database, backend, cloud, Retrofit, Room, Hilt, or unrelated features.
- Keep `MainActivity.kt` thin and keep histogram math outside Compose/Canvas.
- Preserve the required grayscale formula, 256 bins, 0..100 normalization, and transparent performance measurement.
- Prove Baseline/Optimized equality before reporting speed improvements.
- Update the matching TODO and documentation with every material code change.
- Keep `main` runnable and never commit build output, local configuration, or `references/open_source/`.
- Do not copy third-party Skill text or repository source into app code. Record adopted ideas and implement project-specific rules independently.
