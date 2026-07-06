# ImageHistogramAnalyzer agent instructions

Use `.github/skills/image-histogram-android/SKILL.md` for Android, Gradle, Compose, image-processing, histogram, performance, testing, and project-documentation work in this repository.

Supporting workflow skills:

- `.github/skills/android-test-gate/SKILL.md`: require proportional tests after each meaningful code slice.
- `.github/skills/project-doc-sync/SKILL.md`: synchronize TODO, design, testing, and usage documents from evidence.
- `.github/skills/project-git-checkpoint/SKILL.md`: create safe, tested, authorized Git checkpoints at logical boundaries.

Before editing:

1. Read `docs/迭代计划与TODO.md` and select one task ID.
2. Inspect Git status and relevant files.
3. Keep the single-app architecture and course scope unless the user explicitly changes them.

Non-negotiable boundaries:

- Do not add login, registration, database, backend, cloud, Retrofit, Room, Hilt, or unrelated features.
- Keep `MainActivity.kt` thin and keep histogram math outside Compose/Canvas.
- Preserve the required grayscale formula, 256 bins, 0..100 normalization, and transparent performance measurement.
- Prove Baseline/Optimized equality before reporting speed improvements.
- Update the matching TODO and documentation with every material code change.
- Keep `main` runnable and never commit build output, local configuration, or `references/open_source/`.
