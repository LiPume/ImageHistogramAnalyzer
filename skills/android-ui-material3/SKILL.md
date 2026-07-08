---
name: android-ui-material3
description: Audit and improve the ImageHistogramAnalyzer Android UI using the project's Jetpack Compose and Material 3 design system. Use when changing home/analyzer layouts, theme colors or typography, cards, buttons, page states, lightweight result visualizations, accessibility semantics, previews, or Compose UI tests in this repository.
---

# Android UI Material 3

Use this skill only for the ImageHistogramAnalyzer single-module course app. Preserve histogram behavior and performance timing while making the interface clearer, consistent, accessible, and testable.

## Required context

1. Read `AGENTS.md`, `docs/迭代计划与TODO.md`, and `docs/ui-design-system.md`.
2. Select one `UI-*` TODO and inspect `git status --short --branch`.
3. Inspect the target composable, its state owner, strings, theme, and existing tests before editing.
4. Also use `.github/skills/android-test-gate/SKILL.md` for code/resource changes and `.github/skills/project-doc-sync/SKILL.md` before completion.

## Material 3 rules

- Resolve color, type, and shape through `MaterialTheme`; preserve the selected B “Sky & Citrus” fixed light scheme.
- Use the project 8dp spacing scale and tonal surface containers. Avoid page-local colors, arbitrary radii, excess shadows, and decorative gradients.
- Match navigation to scope: this two-level flow needs a home destination, an analyzer destination, and a clear top-bar/system back path—not a new navigation framework.
- Labels describe the immediate action. A system picker action is “选择图片”.
- Represent initial, loading, success, and error states explicitly.

## Compose rules

- State flows down and events flow up. Keep screen/content components stateless where practical.
- Keep image decoding, histogram math, quality analysis, and performance measurement outside composition and Canvas.
- Use `rememberSaveable` for small local UI destination state. Do not move temporary UI state into the ViewModel without a business need.
- Use `remember`, `derivedStateOf`, effects, stability annotations, and Lazy item keys only when their documented condition applies.
- Apply the caller `modifier` to the root. Check modifier order for touch area, clipping, background, and padding.
- Extract only repeated or independently testable UI. Do not build a generic component framework for one screen.

## UI checklist

- [ ] A user can tell what the app does before selecting a photo.
- [ ] The primary action label matches the next system operation.
- [ ] The analyzer has a visible return-home action and supports system back.
- [ ] Cards follow a clear title → summary → evidence → detail order.
- [ ] Exact histogram, quality, and performance values remain visible.
- [ ] Any new visualization answers a specific question and needs no third-party chart library.
- [ ] The fixed light theme and 1.5× font scaling remain readable, including when the device uses dark mode.

## Performance checklist

- [ ] No bitmap traversal, list-wide transformation, or heavy formatting occurs in a composable body.
- [ ] UI changes do not alter the Native v3 core-timing boundary.
- [ ] Images have bounded display size and fixed-result sections remain scrollable.
- [ ] No speculative cache, `derivedStateOf`, key, or `@Stable` annotation is added without evidence.
- [ ] Real performance conclusions distinguish debug/emulator checks from release/real-device measurements.

## Accessibility checklist

- [ ] Interactive targets are at least 48dp.
- [ ] Meaningful images and actions have action-oriented descriptions; decorative graphics are null.
- [ ] Section titles expose heading semantics.
- [ ] Custom distribution/progress graphics expose one concise semantic summary and retain text values.
- [ ] Status is not conveyed by color alone; contrast works throughout the fixed light theme.

## Verification

Run the narrowest affected test first, then the project gate:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

When an emulator/device is connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Update the selected TODO and affected docs using verified results. Report changed files, visible behavior, accessibility checks, exact commands/results, remaining risks, and real-device checks. Never report an unrun command as passed.
