---
name: project-git-checkpoint
description: Create safe Git checkpoints for ImageHistogramAnalyzer. Use after a verified TODO-sized change, before or after risky Gradle/Manifest refactors, at stage boundaries, during handoff, or whenever the user asks to inspect status, commit, push, synchronize, or prepare a recoverable project checkpoint.
---

# Project Git Checkpoint

Create small, understandable, recoverable checkpoints without mixing unrelated work.

## Choose the checkpoint moment

Suggest a checkpoint when one of these is true:

- One TODO task or independently testable slice is complete.
- Relevant tests and build checks pass.
- Work is about to touch a high-conflict or high-risk file.
- A milestone ends, ownership changes, or the work session is handed off.

Do not create commits merely because time passed. Do not checkpoint known-broken work on `main`.

## Inspect before staging

1. Run `git status --short --branch`, `git diff --stat`, and inspect relevant diffs.
2. Identify user-owned or unrelated changes and leave them untouched.
3. Confirm `references/open_source/`, `.gradle/`, `build/`, `.idea/`, `local.properties`, secrets, temporary images, and original local Word files remain ignored.
4. Confirm the matching TODO and documentation are synchronized.
5. Run the tests required by `$android-test-gate` or report why they cannot run.

## Stage safely

- Stage explicit files or coherent path groups; avoid blind `git add .` in a dirty worktree.
- Review `git diff --cached --name-status` and `git diff --cached` before committing.
- Unstage accidental files without discarding their contents.
- Never rewrite, amend, squash, force-push, reset, or delete user history unless explicitly requested.

## Commit clearly

Use one focused message:

- `feat:` user-visible capability
- `fix:` defect correction
- `test:` test-only change
- `perf:` measured performance change
- `docs:` documentation-only change
- `build:` build/dependency change
- `chore:` governance or maintenance

Reference a TODO ID in the body when useful. Do not claim tests or performance results that were not run.

## Push with authority

- Commit or push only when the user requested it or the current task already grants that authority.
- Otherwise, prepare the staged-file recommendation and proposed message, then report that the checkpoint is ready.
- Before pushing, confirm the branch and `origin`; use normal non-force push.
- After pushing, verify local HEAD tracks the expected remote branch and the worktree contains no accidental changes.

## Report

Return the commit hash/message, pushed branch, tests run, remaining uncommitted files, and next TODO ID. If no checkpoint was created, state the precise reason.
