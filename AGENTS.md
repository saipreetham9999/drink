# Drink — Coding Agent Instructions

## Project and source of truth

Drink is an Android hydration companion built with Kotlin and Jetpack Compose. It must be offline-first, privacy-focused, lightweight, and usable on older phones with approximately 4 GB RAM. It is not a medical device.

Read this file, DESIGN.md, and TASKS.md before starting work. Inspect existing code and configuration before changing anything. These files and the product owner's instructions are the shared project context; do not assume access to a separate ChatGPT conversation.

## Scope and decisions

- Work on one phase or feature at a time. Identify the active phase before implementing.
- Complete Phase 1 build and launch verification before implementing hydration tracking, reminders, or camera detection.
- State assumptions that materially affect the design. Discuss and document changes to core requirements before implementation.
- Preserve working functionality. Do not delete existing code without a clear reason.
- Do not spawn additional agents unless the product owner explicitly requests delegation.

## Implementation rules

- Make small, reviewable changes using idiomatic Kotlin and Compose.
- Prefer simple solutions and existing compatible dependencies. Add dependencies only when the current feature needs them.
- Introduce packages as features are implemented; do not create empty architecture scaffolding.
- Keep UI, local data, hydration rules, reminders, and detection responsibilities separate.
- Never let the AI detection layer directly modify hydration records. Route candidates through confirmation or an explicitly approved recording policy.
- Keep the app useful when AI is disabled or unavailable.
- Do not add a backend, accounts, cloud database, subscriptions, paid APIs, analytics, or advertising SDKs. Analytics and advertising require explicit approval.
- Avoid unnecessary permissions and expensive processing. Do not block the main UI thread.

## Privacy and detection

- Keep hydration records and camera processing on-device. Do not upload camera frames or store raw video by default.
- Request camera access only when needed, explain its purpose, and visibly indicate active detection.
- Use explicit user-controlled detection sessions. Do not implement hidden recording or silently enabled background camera access.
- Describe detections as likely drinking events, never verified swallowing or precise measured consumption.
- Low-confidence detections must not automatically increase intake. Support confirmation, rejection, correction, and deduplication.
- Build camera and landmark prototypes before training a classifier. Measure performance on target hardware.

## Verification and reporting

- Run checks appropriate to the change; never claim a build, test, or device check passed unless executed successfully.
- Document build configuration and material limitations.
- Update TASKS.md with verified progress; distinguish planned, completed, and blocked work.
- Update TEST_SCENARIOS.md whenever feature behavior changes. Keep final functional scenarios editable and distinguish planned scenarios from executed checks.
- Keep Dev testing entries in sync with implemented features. Reuse production logic/screens, isolate test hydration data, and identify checks that still require physical-device or elapsed-time testing. Do not add placeholder tests for unimplemented features.
- Report changes, affected files, commands executed, results, remaining issues, and the recommended next task.
- Do not commit, publish, or deploy unless requested.
