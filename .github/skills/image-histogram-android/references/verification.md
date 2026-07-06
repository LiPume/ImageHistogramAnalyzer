# Verification checklist

## Always applicable

```bash
git diff --check
./gradlew test
./gradlew assembleDebug
git status --short
```

Run only commands supported by the current project stage. If a command cannot run, report the exact blocker instead of implying success.

## Gradle or project setup

- Verify Gradle sync/build with the repository wrapper.
- Confirm minSdk, targetSdk, compileSdk, JVM target, Compose compiler/configuration, and dependency aliases agree.
- Inspect the merged manifest for one exported Launcher Activity after it is introduced.
- Confirm no storage permission, database, networking, or unrelated dependency was added.

## Histogram and normalization

Test at minimum:

- 1×1 black and white pixels.
- Pure red, green, and blue under the documented rounding rule.
- A small matrix with hand-calculated bins.
- One-bin peak maps to height 100.
- Proportional bins map within 0..100 without integer-overflow/order errors.
- Empty/invalid input follows the documented error contract.
- Raw bin sum equals analyzed pixel count.
- Baseline and Optimized arrays match element by element.

## Image selection and memory

- Test picker success, cancellation, replaced selection, unreadable URI, damaged image, and oversized image.
- Test JPEG, PNG, WebP, portrait, landscape, and transparent input where supported.
- Select multiple large images in succession and verify stale jobs cannot overwrite the latest state.
- Use Profiler for peak Bitmap and `IntArray` allocations before claiming memory safety.

## Compose UI

- Verify loading, empty, success, error, and reselect states.
- Confirm Canvas consumes only normalized bins and does not trigger processing during recomposition.
- Verify 0/128/255 direction, baseline orientation, black/white contrast, and logical 256×100 mapping.
- Check 48dp touch targets, meaningful content descriptions, heading/reading order, dark theme, and narrow screens.

## Performance evidence

- Keep decode, core calculation, and first rendering measurements separate.
- Use the same Bitmap, rounding rule, build type, and run count for Baseline and Optimized.
- Warm up before recorded runs and report median plus worst or percentile, not one lucky number.
- Record device model, Android version, CPU context if useful, image width/height/pixels, build type, and commit.
- Verify output equality before accepting speedup.
- Treat emulator and Debug results as development signals, not final proof of the 300ms requirement.

## Documentation gate

- Update the exact task ID in `docs/迭代计划与TODO.md`.
- Update algorithm/architecture docs when semantics or boundaries change.
- Update test report with real commands, devices, inputs, and results.
- Update usage instructions and screenshots when user-visible behavior changes.
