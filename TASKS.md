shall we start# Drink — Implementation Tasks

## Current scope

Phase 4 is active: sleep-first setup, daily/seven-day target and attempts, and offline reminders calculated across waking hours. Preserve extra intake and read-only history; lock the daily plan after any recorded drink. No separate settings screen.

Current request: revise Phase 4 into sleep-first setup, daily/seven-day target and attempts, per-attempt recording, and automatic waking-hour reminder slots. Lock the day's plan after its first recorded drink. Preserve read-only history and optional extra water. Update TEST_SCENARIOS.md; final functional testing remains deferred.

Testing plan: `TEST_SCENARIOS.md` is the editable final functional checklist. Full functional execution is deferred until the final build; build/relevant automated checks continue during implementation. Revise scenarios with approved function changes. Do not mark final scenarios passed based only on earlier development checks.

## Phase 0 — Environment and instructions

- [x] Inspect existing project files and configuration.
- [x] Create AGENTS.md, DESIGN.md, and TASKS.md using the handoff and confirmed Drink name.
- [x] Configure Git ignores for generated files and local settings.
- [ ] Verify Android development environment and agent setup as needed for execution.

## Phase 1 — Build and launch starter app

- [x] Inspect project structure and declared Gradle/SDK configuration.
- [ ] Verify effective Kotlin tooling, Gradle JVM, installed SDK, and library compatibility.
- [x] Configure Compose and create a minimal launchable starter Activity.
- [x] Build debug application and resolve configuration errors (owner's Android Studio Run).
- [x] Run on an emulator or physical Android device (Medium Phone API 36.1).
- [x] Verify the initial screen launches without crashing (owner confirmed after cold boot).
- [ ] Document verified build configuration and execution results.

Acceptance: debug build succeeds; app launches without crashing; configuration is documented; no unnecessary dependencies are added.

## Phase 2 — Hydration MVP

- [x] Create Home screen, configurable daily goal, manual intake, and immediate progress updates (compiled; manual check pending).
- [x] Persist intake locally and handle day boundaries (implemented; restart/rollover manual check pending).
- [x] Add basic tests for hydration calculations (executed successfully).
- [ ] Owner verifies Phase 2 UI, persistence after restart, and local day rollover through Android Studio.

Acceptance: intake can be added, progress updates immediately, data survives restart, and daily totals are correct.

## Phase 3 — History and settings

- [x] Display previous recorded dates and read-only daily intake details (compiled; Home/history/back navigation verified on emulator).
- [x] Save each day's goal so completion stays accurate after future goal changes (storage test passed).
- [x] Verify existing database migration, additional intake beyond the goal, and history navigation (automated database tests and empty-history emulator navigation passed).
- [ ] Visually check a populated history date and its details on a device; emulator currently has no previous-day intake.

Acceptance: tap a previous date to see its intake and daily completion; records cannot be edited/deleted; Home supports goal edits before recording and extra water beyond the goal. No separate settings screen.

## Phase 4 — Reminders

- [x] Add notification permission flow and configurable offline reminder implementation (compiled; complete functional run pending).
- [x] Implement sleep-aware attempt slots and approximate scheduling (unit checks passed; actual background/Doze behavior pending).
- [ ] Verify notifications and disable controls on a physical device.
- [x] Update TEST_SCENARIOS.md with concrete Phase 4 scenarios and revised permission expectations.

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

2026-10-08: owner confirmed the starter screen works after the emulator restart. Phase 2 implementation adds total intake, editable daily goal, an amount field and Add Water, SQLite persistence on a background executor, and local-day aggregation. Calculation tests cover validation, progress limits, and daylight-saving day boundaries. Automated verification is pending; Phase 2 manual acceptance is pending.

2026-10-08: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` succeeded using Android Studio's bundled Java 21. Initial sandbox attempt failed on the Gradle wrapper cache lock; approved retry completed successfully. No emulator tests were run by the agent. Phase 2 manual acceptance remains pending.

2026-10-08: owner authorized agent emulator testing. APK install succeeded. Screen inspection and ADB inputs verified Add Water changes 0 to 250 mL, saving a 1,500 mL goal, persistence of both after force-stop/relaunch, and Add Water disabled for zero input. `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest --console=plain` succeeded. Both instrumentation tests passed, including an isolated database test verifying midnight inclusion/exclusion and persistence after database reopening. No new product features were added. Actual visible midnight rollover and physical-device performance remain unverified.

Test limitation: the Gradle connected-test runner uninstalled the target app afterward, clearing emulator app data and the backup stored inside its database directory. The observed starting UI was 0 mL with a 2,000 mL goal. Agent reinstallation restores defaults, not prior database contents. Future emulator-test backups must be stored outside the app before connected tests.

Phase 3 verification: `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain` succeeded; four hydration unit tests passed (plus the template unit test). Installed app/test APKs with `adb install -r`, then `adb shell am instrument -w com.drink.app.test/androidx.test.runner.AndroidJUnitRunner` returned OK (4 tests). Tests cover version 1 migration, read-only history totals/entries, daily goal preservation, over-goal intake, and database reopening. Direct instrumentation kept the target app installed and preserved its data. On-emulator UI inspection verified Home percentage, opening empty History, and returning via Android Back. A populated date's visual layout remains unverified; no actual user history was fabricated for testing. No settings screen or record edit/delete controls were added.

Structure refactor verification: reorganized implemented code into hydration model/domain/data/presentation and Home/History UI packages. `HydrationController` owns named actions and background loading; `MainActivity` hosts lifecycle and saved navigation. Existing tests moved to matching domain/data folders. Database schema/version and dependencies unchanged. `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain` passed. Direct `adb shell am instrument -w com.drink.app.test/androidx.test.runner.AndroidJUnitRunner` passed all 4 emulator tests; 4 hydration rule tests plus the template unit test passed. Installed with `adb install -r` without clearing intake. Initial UI launch timed out on the slow emulator; subsequent fresh UI inspection confirmed Home, opening History, and returning to Home using Android Back. No reminders or new product features were added.

Test planning update: created `TEST_SCENARIOS.md` with a two-day end-to-end sequence, hydration/history edge cases, upgrade/persistence/privacy/performance checks, future-feature drafts, and an execution/results log. Documentation-only change; no tests executed and no app behavior changed in this update.

Phase 4 implementation record: dedicated reminder model/domain/data/scheduling/notification/presentation packages and Home reminder controls added. Defaults are off, 120 minutes, quiet hours 22:00–08:00; intervals 15–1,440 minutes. Added POST_NOTIFICATIONS and RECEIVE_BOOT_COMPLETED; no network, exact-alarm, camera permissions or new dependencies. Reminder configuration is stored separately from the hydration database and excluded from backup/transfer. Android permission handling, blocked-channel recovery, single-alarm scheduling, cancellation, duplicate/quiet-hour checks, and reboot/update/time changes are implemented. Reminders never record intake.

Phase 4 development checks: `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug --console=plain` succeeded on the final code. Ten unit tests passed (five reminder, four hydration, one template). Direct `adb shell am instrument -w com.drink.app.test/androidx.test.runner.AndroidJUnitRunner` passed five tests, including isolated reminder preference persistence. No user intake was cleared. Lint has no errors; remaining warnings concern version notices, legacy unused resources, and KTX style suggestions. No dependency upgrades were made. Actual notification delivery, permission UX, alarm cancellation on-device, boot/Doze behavior, and physical-device checks remain unverified for the final functional run.

Test-case update record: replaced reminder drafts with REM-01 through REM-15, revised FUNC-19 permissions/backup expectations, and added a standing AGENTS.md instruction to update TEST_SCENARIOS.md for each feature change. Final functional statuses remain Not run as requested.

Next task: review the Phase 4 implementation; execute the full functional checklist at the end of development. Camera prototype (Phase 5) requires a new request. Keep scenarios synchronized with any further feature changes.

Sleep-first revision: implemented schema 3 daily/seven-day plans, exact whole-mL attempt allocation, append-only extras, plan locking, persisted sleeping hours, automatic waking-hour alarms, and revised Home/setup screens. Updated DESIGN.md and replaced final functional scenarios in TEST_SCENARIOS.md. Existing legacy intervals are superseded; no new dependencies or permissions. Build/test APK/unit/lint command succeeded; 10 unit tests passed, lint reports 0 errors and 46 nonfatal warnings (unused older resources/version/KTX notices). Rebuilt test APK after adding schema 2 upgrade coverage. Full functional/notification/physical-device testing remains pending.


Revised Phase 4 emulator verification: installed both APKs with adb install -r. Initial instrumentation process was killed for startup ANR on the slow emulator; explicit-class retry of adb shell am instrument -w passed all 6 tests (four hydration database, reminder preferences, template). Coverage includes schema 1/2 migrations, preserved past goals, exact attempt totals, plan locking after attempt/extra, weekly rollover/expiry, and reopening persistence. MainActivity launch succeeded; fresh UI hierarchy confirms sleeping-hours setup and both time controls. A Pixel Launcher ANR dialog initially obscured the screen; dismissing it exposed Drink. No user intake was cleared. Actual reminders/full manual sequence remain Not run in TEST_SCENARIOS.md.
Recommended next task: manually exercise the approved setup and attempt flow, then run the final functional checklist when development is complete.
