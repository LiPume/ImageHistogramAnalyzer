# Classroom deck design rules

These project-specific rules adapt ideas from the local research copy of `guizang-ppt-skill` and the official Codex presentation workflow. They do not copy its HTML template or execute its scripts.

## Communication job

By the end of the report, the teacher should understand that the project implements both required histogram workflows, preserves exact results across Kotlin and Native paths, and uses measured engineering changes to bring 12.58MP core computation below the 300ms target in existing Xiaomi 14 evidence.

## Slide rhythm

- Open with the problem and measurable target, not an agenda.
- Alternate evidence shapes: statement, screenshot, process, architecture, chart, test matrix.
- Use a section pivot after three or four content slides.
- Close by resolving the 300ms question and clearly naming the remaining final-device placeholders.

## Visual grammar

- Canvas: 1280×720, light background, 72px left/right safe margin.
- Typography: Chinese sans-serif with Windows fallback; large text uses lighter weight, small labels use medium weight.
- Palette: navy text `#17365D`, sky blue accent `#3A7FCC`, pale blue `#EAF3FC`, neutral line `#CCD8E5`. Mint `#D9F2E8` and citrus `#FFF0C2` are semantic exceptions only.
- Shapes: square or nearly square, no decorative shadow, 1px dividers.
- Screenshots: portrait frames with contain/crop, no redrawing of UI or numbers, one screenshot purpose per slide.
- Charts: begin at zero, label units, show both algorithms consistently, state why the difference matters.

## Content limits

- One primary claim per slide.
- No paragraph longer than three lines at presentation size.
- No more than five bullets on one slide.
- Prefer exact nouns and verbs over slogans.
- Avoid exposing generation notes, prompt instructions, file paths, or internal QA language on visible slides.

## Required acceptance coverage

The deck should cover: scope, user flow, formula and normalization, two algorithms, architecture, v1→v3 optimization, current UI/quality analysis, automated tests, 17-ID final matrix, limitations, and final status.

Missing L1 final-matrix values must display `[待真机填写]`; existing v3/v4 screenshots may be labeled as prior single-run evidence.
