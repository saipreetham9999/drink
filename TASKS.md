shall we start# Drink — Implementation Tasks

## Current scope

Phase 1 is underway. The Compose starter screen and launcher configuration are implemented. The product owner will run the emulator using Android Studio buttons and verify the screen manually. Do not implement Phase 2 or later features yet.

## Phase 0 — Environment and instructions

- [x] Inspect existing project files and configuration.
- [x] Create AGENTS.md, DESIGN.md, and TASKS.md using the handoff and confirmed Drink name.
- [x] Configure Git ignores for generated files and local settings.
- [ ] Verify Android development environment and agent setup as needed for execution.

## Phase 1 — Build and launch starter app

- [x] Inspect project structure and declared Gradle/SDK configuration.
- [ ] Verify effective Kotlin tooling, Gradle JVM, installed SDK, and library compatibility.
- [x] Configure Compose and create a minimal launchable starter Activity.
- [ ] Build debug application and resolve configuration errors.
- [ ] Run on an emulator or physical Android device.
- [ ] Verify the initial screen launches without crashing.
- [ ] Document verified build configuration and execution results.

Acceptance: debug build succeeds; app launches without crashing; configuration is documented; no unnecessary dependencies are added.

## Phase 2 — Hydration MVP

- [ ] Create Home screen, configurable daily goal, manual intake, and immediate progress updates.
- [ ] Persist intake locally and handle day boundaries.
- [ ] Add basic tests for hydration calculations.

Acceptance: intake can be added, progress updates immediately, data survives restart, and daily totals are correct.

## Phase 3 — History and settings

- [ ] Store individual intake events and display daily history.
- [ ] Support correction/deletion and configurable goal/serving size.

Acceptance: entries are inspectable, changes persist, and deletion updates totals.

## Phase 4 — Reminders

- [ ] Add required notification permission flow and configurable offline reminders.
- [ ] Respect quiet hours and Android execution restrictions.
- [ ] Verify notifications and disable controls on a physical device.

## Phase 5 — Camera prototype

- [ ] Add CameraX, permission explanation, preview, and explicit start/stop controls.
- [ ] Measure device performance and verify frames remain on-device.

## Phase 6 — Drinking gesture prototype

- [ ] Integrate local landmarks and short temporal movement analysis.
- [ ] Implement baseline candidates, cooldown, and deduplication.
- [ ] Allow confirmation/rejection and record performance measurements.

Acceptance: some deliberate drinking-like gestures produce candidates, false positives can be rejected, and no swallowing verification is claimed.

## Phase 7 — AI improvements

- [ ] Establish a licensed, consented evaluation dataset.
- [ ] Compare a small trained classifier with the rule-based baseline.
- [ ] Document precision, recall, false positives, and performance across lighting, angles, and occlusion.
- [ ] Optimize inference and battery usage while maintaining target-device responsiveness.

## Phase 8 — Polish and release preparation

- [ ] Improve accessibility and permission-denial handling.
- [ ] Verify offline behavior, battery usage, and automated/manual checks.
- [ ] Prepare release build and documentation.

## Verification record

2026-10-08: inspected repository files, configuration, and Git status. Documentation and ignore rules added. Gradle build, tests, emulator launch, and physical-device launch have not been executed. Phase 1 remains incomplete.

2026-10-08: product owner ran the app through Android Studio on Medium Phone API 36.1. MainActivity started and process 16633 connected, but the owner reported a blank screen. Read-only `adb logcat -d --pid=16633 --format brief` inspection found no fatal exception in the available log, a roughly 6.9-second first frame, skipped frames, and emulator EGL configuration warnings. These do not establish the cause. Screen acceptance remains unverified; manual testing stays with the product owner.

Next task: manually cold boot the emulator and rerun the app; if the screen remains blank, collect a screenshot and fresh Logcat output to diagnose rendering. Phase 1 remains incomplete.
