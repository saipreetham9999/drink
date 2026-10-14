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
