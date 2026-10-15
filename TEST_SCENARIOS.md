# Final functional scenarios — Drink

Run this checklist at the end of development. All scenarios below are **Not run** unless an execution entry explicitly records a result. Automated development checks are recorded in TASKS.md. Update this file whenever behavior changes.

## Sleep and setup
- SLEEP-01: Fresh install opens sleeping-hours setup before the water plan. Choose 22:00 sleep and 08:00 wake, save, restart; hours persist locally.
- SLEEP-02: Equal sleep/wake times cannot be saved. Test daytime sleep (09:00–17:00) and overnight sleep.
- PLAN-01: Set 2,000 mL and 10 attempts for today. Preview and Home show 200 mL per attempt, 0/10 attempts, 0/2,000 mL.
- PLAN-02: Choose 7 days. The same daily target/attempt count applies today through the sixth following date. Day eight asks for a new plan.
- PLAN-03: Before recording, edit target/attempts. After the first attempt or extra entry, editing is unavailable and storage rejects plan changes.
- PLAN-04: Reject blank, zero, negative, decimal, overflow, target above 100,000, attempts above 32, attempts exceeding target, or schedules too crowded for 15-minute spacing.
- PLAN-05: Start late in the day. Past reminder times are skipped; manual attempts remain available.
- PLAN-06: Revise an unstarted weekly day. Today/7-day choice replaces the upcoming plan; previous recorded dates retain their targets.

## Recording and history
- RECORD-01: Record ten 200 mL attempts. Each adds exactly one entry and one attempt; final total is 2,000 and 100%. Planned recording stops at ten.
- RECORD-02: Target 1,001 with three attempts records 334, 334, 333 mL, totaling exactly 1,001.
- RECORD-03: Add extra 100 mL before/after planned completion. It adds intake, keeps planned attempt count unchanged, locks the day, and permits completion above 100%.
- RECORD-04: Reject invalid extra amounts. Canceling the dialog adds nothing. Rapid taps while saving do not duplicate a single accepted action.
- RECORD-05: Restart/rotate/background/resume; totals, plan and attempt count persist.
- RECORD-06: Cross midnight while Home is open; new day refreshes, starts at zero, applies active weekly plan or asks for a plan. A stale previous-day action is rejected.
- HIST-01: Open History after recording on at least two previous dates. Dates appear newest first, only dates with records; current date is excluded.
- HIST-02: Open a date; total, original goal, percentage and timed entries match recorded intake. No edit/delete actions appear.
- HIST-03: Later target changes never alter past goals/records. Android Back returns from detail to dates, then Home.
- HIST-04: Timezone change preserves each entry's stored local recording date; displayed times use current timezone.

## Reminders
- REM-01: Saving the first plan requests Android notification permission when needed. Allow schedules reminders; deny leaves recording usable and offers enabling reminders later.
- REM-02: Ten calculated slots lie in waking hours, with at least 15-minute spacing. Notifications display the next unrecorded attempt amount.
- REM-03: Receiving/dismissing/tapping a notification never records water. Tap opens Drink.
- REM-04: Turn reminders off, verify pending alarm/notification cancellation; turn on and verify future slots resume.
- REM-05: Record attempts early; corresponding initial slots are skipped. Complete all planned attempts; today's reminders stop, next weekly date remains scheduled.
- REM-06: Change sleeping hours; future alarms recalculate, intake and locked goal remain unchanged. Reject hours too short for active plans.
- REM-07: No delivery during overnight/daytime sleep, even if Android delayed the alarm. No catch-up burst after resume/late setup.
- REM-08: Test reboot, app update, timezone/clock changes, force-stop then reopen. Only one pending alarm; duplicate/stale broadcasts do not notify twice.
- REM-09: Test notification permission/channel blocked and restored. Inexact alarms may be delayed by Android; verify behavior in Doze on a physical device.
- REM-10: Test daylight-saving start/end; reminder slots stay outside sleep and remain ordered. Weekly expiry leaves no subsequent reminders.

## Upgrade, privacy and regression
- REG-01: Upgrade schema 1/2 installation. Existing amounts/timestamps/dates remain. Unknown old goals stay unavailable. Legacy current-day records retain their goal and are not relabeled as attempts; compatibility plan uses ten attempts (or fewer when target below ten), expires today.
- REG-02: Airplane mode supports setup, recording, history and local reminders.
- REG-03: No account/backend/network/camera permission or settings screen added; private local storage, backup/transfer exclusions remain.
- REG-04: Verify readable layout, keyboard/time-picker usability, dark mode, rotation, large text, scroll and Android Back on emulator and older physical phone.
- REG-05: Verify storage failures show retry and do not show an unsaved entry as recorded. Check database growth with many entries and phone responsiveness.

## Retired expectations
Previous scenarios allowing goal changes after recording or manual notification intervals/quiet-hour toggles are superseded by PLAN-03 and REM-02/06. Historical verification records remain in TASKS.md.

## Execution log
| Date/device/build | Scenario IDs | Result/issue |
| --- | --- | --- |
| Pending final functional run | All | Not run |

## Setup UI revision — pending manual verification
- UI-01: White background and black controls/text even in device dark mode. Goal and attempts appear before sleeping hours on one screen.
- UI-02: Edit 2,000 mL / 10 attempts to 1,000 / 5: adjacent amount updates immediately to 200 mL; invalid/blank entries disable Start.
- UI-03: Type sleep 10:00 PM and wake 8:00 AM without a clock picker. Toggle AM/PM and verify preview changes; midnight is 12:00 AM and noon 12:00 PM.
- UI-04: Reminder list shows every time with AM/PM and mL. Editing either sleep field recalculates it; malformed/equal times disable Start.
- UI-05: Today/7 days selection remains visible; passed times clearly marked. Start saves the reviewed plan before requesting notification permission.
- UI-06: Keyboard, large text, rotation, scrolling and Cancel preserve usable controls and draft values. Failed save allows retry.
- UI-07: Existing locked plans and records survive update. Existing History remains unchanged; history enhancements are deferred.
Previous sleep-first/clock-picker expectations are superseded by UI-01 through UI-07. All new manual scenarios: Not run.
UI capture check: actual setup form and scrolled schedule/Start button visually inspected on Medium Phone API 36.1; fixed malformed separator glyph. This is screenshot verification only, not completion of UI-01 through UI-07 or final functional testing.

## Plan saving logic - pending functional verification
- SAVE-01: Start a valid today/7-day plan; reopen the app and verify goal, attempts, sleep/wake AM/PM and expiry persist together.
- SAVE-02: Failed save leaves both previous plan and sleeping hours unchanged; retry succeeds without intake or duplicate records.
- SAVE-03: Reject invalid target/attempts/duration, equal sleep/wake, insufficient waking time and stale previous-day setup before any write.
- SAVE-04: Preview and save use the same validation for selected duration, including a daylight-saving change within a weekly plan.
- SAVE-05: A locked plan rejects replacement without changing saved sleeping hours, historical goals or upcoming template.
- SAVE-06: Upgrade schema 3 to 4 without losing intake/plan; legacy sleeping hours and reminder preference stay readable. Once saved, canonical database hours take precedence.
- SAVE-07: Notification permission is requested only after successful save; denial leaves the saved plan and manual recording usable. Final functional status: Not run.

Step 1 development checks: 16 unit tests and 10 direct emulator instrumentation tests passed. Coverage includes save validation, selected-duration DST, stale date, canonical sleeping-hour persistence/legacy fallback, locked saves, forced database rollback/retry and schema upgrades. These automated checks do not mark the full manual SAVE-01 through SAVE-07 sequence complete.

## Attempt calculations and recording - pending final manual verification
- ATTEMPT-01: For 2,000 mL / 10, preview and Record show 200 mL. Each accepted tap updates total, remaining mL, remaining attempts and completion after saving; ten attempts total exactly 2,000 mL.
- ATTEMPT-02: For 1,001 mL / 3, preview shows 333-334 mL and recordings are 334, 334, 333. The finished Record button is disabled.
- ATTEMPT-03: Repeated taps while a save is pending create one entry. A stale count or changed plan is rejected without adding an unshown serving; refreshed state permits a new intentional recording.
- ATTEMPT-04: Force a storage failure: no intake/count/lock appears. Retry creates one entry; successful first recording locks the plan.
- ATTEMPT-05: Extra water before or after completion changes total but never planned attempt count; extras lock the plan. Remaining mL never becomes negative and completion can exceed 100 percent.
- ATTEMPT-06: Reject invalid extra amounts and stale previous-day actions without changing records. Reopen app and verify total/count/next serving persist. Full manual statuses: Not run.

Attempt recording development verification: 23 unit tests and 15 direct emulator tests passed. Verified 2,000/10 and uneven allocations, one record for repeated controller calls during saving, stale-snapshot rejection, database failure/retry, limits/locking/extras, remaining values and reopening persistence. Full visual/manual ATTEMPT scenarios remain Not run.

## Daily rollover and expiry - pending final manual run
- DAY-01: Leave Home open across local midnight; within the existing 30-second refresh, intake/attempts reset to zero and an active weekly plan remains available. Background/resume also refreshes the date.
- DAY-02: A weekly plan covers its start date through the sixth following date, including DST/calendar changes. Skipped days and reopening never extend expiry; day eight asks for a new plan.
- DAY-03: A today-only plan expires on the next local date. Starting a new plan leaves past intake and goals unchanged.
- DAY-04: Attempt, extra-water or setup actions from a stale prior-day screen fail without recording/changing the plan. Home recovers to the current date and reminder reconciliation is notified even on failure.
- DAY-05: Changing timezone can change Today; prior entries retain their stored local dates. Returning to an already-recorded date shows its existing totals instead of resetting or duplicating them.
- DAY-06: Day reads create no intake, goals or future snapshots. First recording snapshots its actual day. Check ordinary, 23-hour and 25-hour midnight boundaries. Manual statuses: Not run.

Rollover development checks: 23 unit tests and 22 direct emulator instrumentation tests passed, including seven new database/controller rollover tests with simulated time and explicit zones. Verified expiry, midnight/DST boundaries, travel, unchanged previous records, read-only day loads and stale-action recovery callback. Final visual/manual DAY scenarios remain Not run; no actual notification-delivery claim.

## Reminder delivery refinement - pending final functional run
- DELIVERY-01: Preview times and scheduled slots match at minute precision. Late setup skips passed slots; only one future alarm is kept.
- DELIVERY-02: A due alert shows the next unrecorded serving amount. Receiving/dismissing/tapping does not record water. Recording that attempt clears its obsolete notification.
- DELIVERY-03: Delay delivery into sleep, a different date, a changed plan/schedule or completed attempt: no alert. Expired plans cancel; completed weekly days retain only future-day alarms.
- DELIVERY-04: Duplicate/stale broadcasts and reopening/backwards clock changes do not repeat a recently delivered slot. Late delivery respects a 15-minute cooldown without catch-up bursts.
- DELIVERY-05: Turning reminders off cancels alarm and notification even when timezone/DST makes the old schedule invalid. Permission/channel blocking stops scheduling and leaves manual recording usable. Android notification settings remain reachable after denial.
- DELIVERY-06: Reboot/update/timezone/clock-change recovery recalculates future slots. Force-stop requires reopening. Verify real reboot and Doze on a physical device; inexact Android alarms may arrive late.
- DELIVERY-07: Verify notification cancellation when goal/attempt state changes. Failed notification posting does not advance the delivery cooldown or create intake. All final manual statuses: Not run.

Reminder development checks: 35 unit tests and 28 direct emulator tests passed, plus one separate denied-permission check. Verified shared minute slots, current amount, sleep/stale/duplicate/cooldown guards, completion/disable, persistent ledger, actual notification posting/clearing, blocked channel, pending-alarm cancellation and an immediate Android test-alarm broadcast through the shared engine without intake. Notification permission restored to its original denied setting. Test receiver/time injection does not establish production receiver recovery after real reboot or Doze behavior; final manual DELIVERY scenarios remain Not run.

## History verification - pending final full manual run
- HISTORY-01: Previous recorded dates appear newest first; today, future dates and dates with plans but no intake are excluded.
- HISTORY-02: Open a date: original goal, total, completion including over-goal extras, and deterministic timed entries match stored data. Legacy days without saved goals show unavailable rather than estimating.
- HISTORY-03: History has no edit/delete/input controls. Opening dates and details never writes records or changes snapshots; later plans and reopening preserve past goals/totals.
- HISTORY-04: Back returns detail to date list, then Home/setup. History remains reachable when a plan expires or sleeping-hours setup is pending. Invalid current/future/missing selections are ignored or restored to the list.
- HISTORY-05: White/black presentation and explicit AM/PM match the app. Timezone changes do not relocate stored dates; a selection that becomes Today returns to eligible previous dates. Times are labelled as current phone timezone.
- HISTORY-06: Empty history has a clear message; storage failure shows retry without a false empty-success message. Final manual statuses remain Not run.

History development verification: 35 unit tests passed; 5 focused database/controller/UI history tests and 23 regression emulator tests passed. Actual populated list/detail screenshots visually inspected from isolated fixture data in app/build/history-ui-check; verified 1,101/1,001 mL, 109 percent, original goals, unknown legacy goal, AM/PM entries and read-only UI/navigation. No owner records fabricated. This targeted run does not complete the full final manual HISTORY-01 through HISTORY-06 sequence.

## Phase 5 camera setup - final manual statuses: Not run
- CAM-01: Open Camera setup from Home or plan setup. Explanation and stopped status appear; no permission prompt or camera indicator until Start.
- CAM-02: Start requests camera permission. Allow shows a live front-camera preview (rear fallback when no front camera), active status and Stop. No intake changes.
- CAM-03: Deny once or permanently: helpful message and app-settings recovery; Back returns to manual tracking. Grant in settings returns stopped until Start.
- CAM-04: Stop releases the camera and removes the preview. Repeat Start/Stop without stuck streams, crashes or accumulating observers.
- CAM-05: Back, Android Back, Home/background, screen lock and recreation release the camera. Return requires Start; a late permission/provider callback must not reopen a cancelled session.
- CAM-06: Missing camera, privacy toggle, another camera app or initialization failure gives a retryable unavailable state. Manual hydration/history/reminders remain usable.
- CAM-07: Verify offline; no photos, video, audio, analyzed frames or hydration records are saved by preview. Manifest has no network, audio or media-storage permission.
- CAM-08: On a physical target phone near 4 GB RAM measure startup latency, memory, UI responsiveness and battery/temperature during sustained preview; document device/results before landmark analysis.

Camera development verification: final build, 35 unit tests and lint passed (0 errors, 51 nonfatal warnings). Direct instrumentation passed three camera checks across runs: real permission denial and allow dialogs, actual live STREAMING/IDLE transitions, repeated Start/Stop, background return and recreation. Another 28 existing hydration/history/reminder-preference tests passed. Preview overflow was found in screenshot inspection, fixed with clipping and reverified. Original camera-denied state restored and notification denial preserved. Merged manifest excludes network/audio/media storage; preview has no capture/analyzer or hydration writes. These targeted emulator checks do not complete CAM-01 through CAM-08 or establish physical-phone performance.

## Dev navigation - final manual statuses: Not run
- DEV-01: Home and plan setup show Dev at the top right beside Drink; the old direct Camera setup link is absent there.
- DEV-02: Dev opens a dedicated testing page. Camera setup is available there; opening Dev alone does not request permission or activate the camera.
- DEV-03: Camera Back/Android Back returns to Dev and releases any active session. Dev Back/Android Back returns to the previous Home/setup state without changing intake or plan drafts.
- DEV-04: Recreation restores the Dev/camera destination with the camera stopped; existing History navigation remains usable.
Owner feedback: camera setup works on their phone. General page appearance will be refined separately; this does not complete all camera performance/manual scenarios.
Dev navigation development checks: offline app/test APK build and lint passed (0 errors, 51 nonfatal warnings). Direct emulator checks passed for Dev -> Camera setup -> permission denial -> Dev -> Home/setup, and existing populated History navigation. A stale accessibility-node wait in the initial navigation test was corrected to await distinct Dev-page text; the focused retry passed. No intake changes or permission grants; final manual DEV scenarios remain Not run.

## Dev notification test - final manual statuses: Not run
- DEV-NOTIFY-01: Dev -> Test notification requests notification permission if needed. Allow posts an immediate Drink test notification, with sample text and no water recorded. Check the notification panel; heads-up/sound depends on Android/channel/DND settings.
- DEV-NOTIFY-02: Deny or permanently block permission: no sample posts, recovery message and Notification settings link appear. Allow in settings, return and tap Test notification again.
- DEV-NOTIFY-03: Disable the water-reminder channel: sample does not post and UI reports blocking. Restore the channel and retry.
- DEV-NOTIFY-04: Repeat taps update one sample; actual hydration reminders are not replaced. Tapping the sample opens Drink, records no intake, and leaves reminder enabled/schedule/cooldown state alone.
- DEV-NOTIFY-05: Reopening Dev does not send a sample automatically. Test works without a configured water plan, including when regular reminders are off. This is an immediate notification test, not verification of timed/background/Doze reminders.
Dev notification development checks: offline app/test APK build and lint passed (0 errors, 51 nonfatal warnings). Three direct emulator tests passed: actual sample posting after the real Allow dialog, same water-reminder channel, repeat-tap replacement and opening without intake changes; blocked-channel failure; real Deny with settings recovery and no post. Sample/test channel cleaned up; original notification-denied state restored, camera remained denied. These checks do not verify timed alarms or complete the final manual DEV-NOTIFY scenarios.

## Planned Phase 8 custom reminder audio - draft, Not run
- AUDIO-01: Select a ringtone or import a personal voice clip; keep it on-device and retain the choice after reopening.
- AUDIO-02: Verify agreed playback duration (requested around 30 seconds), early Stop, dismissal and repeated reminders without overlapping audio. Final rules pending owner preference.
- AUDIO-03: Missing/deleted/unreadable audio, denied access and invalid files preserve usable standard reminders with clear recovery.
- AUDIO-04: Test playback from Dev, permission/channel blocking, silent/DND, background operation and battery use against the selected Android implementation; do not assume bypass of phone sound controls.

## Planned Phase 9 Tablets tab - draft, Not run
- TABLETS-01: After Tablets meaning and requirements are confirmed, verify Water/Tablets navigation, retained input and separate records without altering existing water plans/history.
- TABLETS-02: Add entry validation, persistence, scheduling, confirmation/history and migration cases only after the owner defines the feature. No medicine-specific behavior has been approved yet.
Roadmap-only update: release/polish is now Phase 10. No runtime behavior changed and no build/device checks run for this update.

Roadmap clarification: owner confirmed Tablets means medicine reminders, and custom reminder audio must not exceed 45 seconds. This supersedes the earlier tentative 30-second wording and Tablets ambiguity in the draft scenarios. AUDIO-02 must verify playback ends by 45 seconds, including longer imported clips, early stopping, repeat reminders and background playback. Medicine fields/schedules/recording rules and audio duration selection remain to be defined. Documentation only; scenarios Not run.

## Phase 6 step 1 - notification camera sessions, final statuses Not run
- SESSION-01: Camera detection is off by default on Home/setup; toggling persists across reopening and requests no camera permission or session by itself. Manual intake/reminders still work.
- SESSION-02: Tap a water reminder with detection on: visible Water reminder camera opens, requests camera permission if needed, then starts preview with a four-minute countdown. Posting the alert alone never opens the camera.
- SESSION-03: With detection off, reminder tap opens Home without camera permission/preview. Change the switch after an alert was posted: tapping honors its current value. Dev sample notifications always open ordinary Drink.
- SESSION-04: Stop and Back release preview. Backgrounding/locking/recreation stops it and does not resume automatically. Late initialization/permission callbacks cannot reopen a cancelled session.
- SESSION-05: Session initialization and active preview together cannot exceed the four-minute elapsed-time budget; countdown reaches zero and camera stops. Phone wall-clock changes do not extend the budget. Only a new explicit Start begins a new budget.
- SESSION-06: Denied/permanently denied camera permission or missing/busy camera offers recovery, with manual tracking available. Closing/recreating during permission request cannot auto-start later.
- SESSION-07: No videos/images/intake are saved; prototype states gesture detection is not implemented yet. Verify reminder scheduling/ledger and existing history are unaffected. Test cold/warm app notification taps on a physical phone, including locked screen (unlock before foreground camera).

Phase 6 step 1 development checks: build/app and test APKs, 39 unit tests and lint passed (0 errors, 52 nonfatal warnings, 1 boxing hint). Seven direct emulator checks passed across runs: opt-in persistence/default, posted reminder's actual PendingIntent routing with switch changed after posting, visible streaming, background/recreation stopping, injected elapsed-time deadline releasing a real stream, denied camera permission/recreation, existing manual camera and populated History. Countdown/Stop screenshot inspected. These checks use PendingIntent.send from a foreground test harness and an injected clock; physical notification-panel/cold/locked taps, a real four-minute run, performance and final SESSION scenarios remain Not run. Original opt-in and denied camera/notification states restored; no owner intake changed.

## Dev testing hub - final manual statuses Not run
- HUB-01: Dev groups Camera, Notifications, and Hydration/reminder tests; scroll to all entries. Existing camera and sample-notification actions still work.
- HUB-02: Camera detection in Dev shares Home's persisted opt-in. Merely opening Dev/toggling does not start camera. Send Test camera reminder, then tap it: switch on opens the capped visible camera session; off opens normal Drink. Samples use distinct IDs and do not replace real reminders or alter their schedule/ledger.
- HUB-03: Test plans and intake opens a clearly labelled sandbox. Save daily/weekly plans, inspect amount/sleep/reminder previews, edit before first drink, record attempts/extras and verify locking and totals.
- HUB-04: Next test day resets that test day's intake; +7 test days permits weekly-expiry checks. The actual phone clock and production plans/records remain unchanged.
- HUB-05: Test history shows only sandbox records, with the same read-only date/detail screens. Record test water then advance a day to populate history. Reopening retains test records/test date; actual history remains unchanged.
- HUB-06: Back and recreation preserve sensible testing navigation and release controllers/camera resources. No test entry for unimplemented detection, custom audio or medicines. Full hardware/Doze/real-duration checks remain separate.

Dev hub development checks: offline app/test APK build, 39 unit tests and lint passed (0 errors, 53 nonfatal warnings, 1 boxing hint). Six direct emulator checks passed across runs: sandbox save/attempt/extra/next-day/history with production-total preservation; shared detection switch plus separate standard/camera samples opening the real session; existing manual camera; populated history; blocked channel; notification denial recovery. New harness scrolling/waits/actionable-parent handling corrected after initial failures. Samples/test fixtures cleaned up and original denied permissions/opt-in restored. Test records use an isolated named database; no owner intake cleared/fabricated. Full manual HUB scenarios and physical timing/performance checks remain Not run.

## Phase 6 complete prototype - final manual statuses Not run
- DETECT-01: Dev -> Test drinking detection -> Start runs offline bundled landmarks; face/shoulders/hands visible. Verify overlay rotation, mirroring, portrait/landscape, front/rear fallback and one-person positioning.
- DETECT-02: Hand away, raise drink to mouth, hold briefly, lower hand: a Possible drink dialog may appear. Static hand, fast touches, low-confidence/lost landmarks and out-of-order/gapped frames must not complete a candidate. False positives remain possible; never claim swallowing or measured mL.
- DETECT-03: One motion gives one pending suggestion; repeated frames/holds and actions within cooldown do not spam. Reject saves nothing. Candidate dialogs stay usable with camera stop/expiry/background behavior.
- DETECT-04: Confirm planned serving adds one attempt exactly once. Corrected amount is explicitly extra water without consuming an attempt. No plan disables recording; invalid amounts cannot confirm. Duplicate taps, retries and reopening cannot duplicate a token.
- DETECT-05: Change plan/count/date between suggestion and confirmation: no stale intake write. Inject write failure and retry; no partial record or consumed token. Upgrade schema 4 and earlier preserving records, plans and sleep hours.
- DETECT-06: Stop, leave app, revoke camera, expiry/recreation or unavailable model releases camera/analysis resources. No automatic restart. Model unavailable still leaves manual hydration usable. Four-minute budget includes initialization.
- DETECT-07: Dev -> Test drink confirmation explicitly simulates a suggestion in the sandbox; use it to test reject, confirm, correction, validation and history. Dev camera-reminder test uses sandbox too; production reminder confirmations use actual intake.
- DETECT-08: Verify no Internet/network-state/audio/media-storage permissions and no remote logger/transport classes. Bundled model hash/license remain pinned; no raw frames/video/landmark logs persisted. First-run offline inference works.
- DETECT-09: Record actual device/OS/RAM, camera orientation, analyzed frames/FPS, model startup, inference avg/max, app memory, temperature/battery over repeated four-minute sessions. Collect deliberate-drinking and non-drinking outcomes with consent for evaluation; do not claim accuracy from synthetic rule tests or a pose fixture. Final physical acceptance remains pending.

Camera storage and lightweight delivery (2026-10-14; manual checks Not run)
- LITE-01: Use repeated detection sessions, Stop, background, four-minute expiry and app restart. No video/photo/selected-frame files should be created in app private/cache/external directories or the gallery. Only confirmed hydration entries may be added. Rejected/unconfirmed candidates create no intake.
- LITE-02: Repeat sessions on consecutive dates. No accumulated camera media should exist; no daily media-cleanup alarm/job or storage permission is needed. Existing hydration history remains available.
- LITE-03: Inspect generated APKs: each contains only its named native ABI; no universal APK is listed in output-metadata.json. Install the matching ARM build on a phone and the matching x86_64 build on the emulator. Verify manual tracking and optional offline detection. Measure APK bytes, installed storage and sustained RAM separately.
