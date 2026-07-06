---
name: project-doc-sync
description: Keep ImageHistogramAnalyzer documentation synchronized with verified code and technical decisions. Use after changes to behavior, architecture, Gradle, algorithms, tests, performance measurements, user flows, scope, risks, milestones, or TODO status, and whenever the user asks to maintain, audit, or update project documentation.
---

# Project Documentation Sync

Keep documents factual, concise, and aligned with the current repository state.

## Determine affected documents

- Always update `docs/迭代计划与TODO.md` for material task progress, blockers, or completion.
- Update architecture/planning documents when package boundaries, dependencies, Gradle, SDK, data flow, or interfaces change.
- Update algorithm/design documents when formula semantics, rounding, alpha handling, normalization, quality thresholds, or timing scope change.
- Update test documentation when test cases, fixtures, devices, commands, results, failures, or performance evidence change.
- Update usage instructions and screenshots only when the user-visible flow changes.
- Update `README.md`, `AGENTS.md`, collaboration rules, or skill files only for repository-wide workflow changes.

Do not edit documents unrelated to the current change merely to make them look fresh.

## Synchronize from evidence

1. Inspect the actual diff, current implementation, tests, and command output.
2. Find the TODO task ID and affected claims with `rg`.
3. Change status only when its definition of done is met.
4. Record concrete facts: supported flow, interfaces, commands, devices, image sizes, timings, and known limitations.
5. Preserve unresolved items as unchecked TODOs or explicit risks.

Never invent screenshots, test passes, device results, performance numbers, owners, dates, or completion status.

## Keep documents maintainable

- Write project documents in clear Chinese; keep code identifiers and commands exact.
- Use one canonical definition for the grayscale formula, rounding, 256 bins, 0..100 normalization, and 300ms timing scope.
- Prefer links to canonical documents over duplicating long sections.
- Keep task IDs stable. Add new IDs instead of silently repurposing old ones.
- Mark superseded decisions explicitly when history matters.
- Keep tables for comparable records; use checklists for actionable work.

## Verify consistency

Before finishing:

- Search for stale SDK versions, old package paths, conflicting statuses, and contradictory timing definitions.
- Confirm README, AGENTS, skills, TODO, implementation, tests, and user-visible wording agree.
- Run `git diff --check` and inspect Markdown links/paths.
- Summarize which documents changed and which evidence justified each update.
