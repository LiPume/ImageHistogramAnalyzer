---
name: course-presentation
description: Create, revise, or verify the ImageHistogramAnalyzer classroom acceptance PowerPoint. Use for course reports, defense decks, acceptance presentations, speaker-facing summaries, slide screenshots, performance charts, or final PPTX handoff in this repository.
---

# Course Presentation

Create a concise, editable `.pptx` that explains the project to a teacher in 5–8 minutes and keeps every technical claim traceable to code, tests, documents, or real-device evidence.

## Start from verified project evidence

1. Read `docs/final/README.md`, `docs/final/05-算法与性能优化说明.md`, `docs/final/06-测试计划与测试用例.md`, and `docs/final/07-真机测试与性能验收记录.md`.
2. Inspect the current version, Git state, Gradle configuration, relevant Kotlin/C++ source, and available screenshots.
3. Use `[待真机填写]` for missing final measurements. Never invent device timings, screenshots, tags, APK hashes, or acceptance results.
4. Treat v1/v2/v3 timings from different source images as directional evidence, not strict same-image acceleration.

## Build the narrative before the slides

Express the communication job in one sentence, then use this arc:

`课程目标 → 可运行产品 → 两种算法 → 性能瓶颈 → Native 优化 → 正确性证据 → 真机验收 → 结论`

Give every slide one claim and a takeaway-style title. A 5–8 minute deck normally uses 9–11 slides. Keep formulas, architecture, performance, correctness, UI, and limitations in the main story instead of appending disconnected requirement pages.

Read [references/design-rules.md](references/design-rules.md) before choosing layouts or colors.

## Use the project visual system

- Use a 16:9 light deck with a Swiss-inspired grid, large sans-serif type, square geometry, hairline rules, and generous whitespace.
- Use one presentation accent: project sky blue. Mint and citrus may appear only when reproducing the App's quality/performance semantics or embedded screenshots.
- Use screenshots as evidence, not decoration. Preserve their content, crop away irrelevant status/navigation areas when practical, and never recreate measurement text.
- Prefer a meaningful native chart for v1→v3 timing changes. Keep exact values and an interpretation beside it.
- Use at least 50pt for the cover, 35pt for slide titles, 24pt for important callouts, and 16pt for body text.
- Avoid gradients, heavy shadows, ornamental animation, dense card dashboards, decorative stock photos, and generic “谢谢” endings.

## Author and verify the PPTX

1. Use the installed `presentations` skill and `@oai/artifact-tool` from a plain JavaScript ES module. Do not use `python-pptx`.
2. Keep scratch files outside the repository. Save only the final PPTX under `docs/final/` unless the user specifies another destination.
3. Render every slide, inspect a montage and full-size pages, and fix text wrapping, clipping, unintended overlap, inconsistent margins, and unreadable screenshots.
4. Run the presentation overflow test and inspect layout JSON warnings. Do not waive an overlap without visual confirmation.
5. Confirm all placeholders are intentional and limited to unavailable final-device data or course submission metadata.
6. Update `docs/final/README.md`, `docs/final/09-交付清单与答辩提纲.md`, and `docs/迭代计划与TODO.md` when a deck is created or materially revised.

## Final handoff

Report the PPTX path, slide count, evidence used, placeholders that remain, and the actual render/overflow checks performed. Keep the final answer focused on the delivered deck.
