---
name: android-test-gate
description: Add and run proportional tests for each Android code slice in ImageHistogramAnalyzer. Use whenever Kotlin, Compose, ViewModel, Bitmap decoding, histogram math, quality analysis, performance code, Android resources, Manifest, or Gradle configuration is created or changed, and before marking a TODO complete or creating a Git checkpoint.
---

# Android Test Gate

Require evidence proportional to the changed code before declaring work complete.

## Select the smallest sufficient test layer

- Pure histogram/normalization/quality math: deterministic JVM unit tests.
- Coroutine or ViewModel state: coroutine test dispatcher, cancellation, success, failure, and stale-result tests.
- `ContentResolver`, Bitmap decoding, Activity Result, Manifest, or Android framework behavior: instrumented test or focused manual/device verification where JVM tests are unsuitable.
- Stateless Compose component: Compose semantics/assertion tests for important states and actions.
- Canvas: data-contract unit tests plus focused rendering/coordinate verification; do not rely only on screenshots.
- Gradle/dependency/resource changes: sync/build, unit tests, APK assembly, and merged-manifest/resource inspection when relevant.
- Performance changes: equality tests first, then repeatable benchmark/real-device measurements. Never encode a universal 300ms assertion in ordinary unit tests.

Do not add Hilt, Robolectric, screenshot frameworks, mocking libraries, or benchmark modules solely because an external template recommends them. Add tooling only when the current test cannot be reliable without it.

## Apply the gate

1. Inspect the code diff and identify observable contracts and failure modes.
2. Add or update the narrow test closest to the changed behavior.
3. Run the narrow test first for fast feedback.
4. Run the broader relevant suite.
5. Run `./gradlew test` and `./gradlew assembleDebug` before a normal checkpoint when the project supports them.
6. For device-only behavior, record device/emulator, Android version, steps, expected result, and actual result.
7. Update the TODO and test documentation through `$project-doc-sync`.

## Project-specific minimums

For histogram work, cover:

- black, white, red, green, blue, and a hand-calculated small matrix;
- documented rounding and transparent-pixel behavior;
- 256 bins, raw-count sum, zero input, maximum bin, and 0..100 normalization;
- Baseline/Optimized element-by-element equality.

For image workflow work, cover picker cancellation, unreadable input, replacement selection, oversized input, cancellation, and stale-result rejection.

For UI work, cover empty, loading, success, error, reselect, accessibility labels, and main action behavior as applicable.

## Test quality rules

- Test public behavior and data contracts, not private implementation details.
- Use fixed inputs and expected values; avoid sleeps, random timing thresholds, network, and external file dependencies.
- Keep test names descriptive in `given_when_then` or equivalent style.
- Do not weaken or delete a valid test merely to make a change pass.
- Treat flaky or failing tests as blockers unless the failure is proven unrelated and clearly reported.

## Report the gate

List tests added/changed, exact commands, pass/fail counts, unrun device checks, and residual risk. Never say “tested” without naming the evidence.
