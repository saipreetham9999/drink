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
