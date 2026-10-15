# Drink — Product and Technical Design

## Vision

Drink helps users track daily water intake and build hydration habits. Optional future on-device camera processing can suggest likely drinking actions. Manual tracking remains fully useful without AI. Drink is not a medical device.

The product name is Drink, replacing Water in the original handoff. The initial scope remains water intake; tracking other beverages is an open product decision.

## Principles

- Offline-first with no required server, account, cloud database, or recurring subscription.
- On-device data and camera processing by default, with no raw video storage by default.
- Lightweight, battery-aware implementation for older Android phones with approximately 4 GB RAM.
- Accessible, readable Material 3 UI with transparent uncertainty and user control.
- Incremental implementation with independently testable responsibilities.

## Planned features

### Manual hydration

Approved setup UI: one white screen asks for daily target and attempts first, followed by editable sleep/wake times with AM/PM and a calculated reminder preview. Apply the plan to today or seven consecutive local dates starting today. Before the first recorded drink, the day's plan can be revised; any recorded intake locks that day's goal/attempt count. A weekly template supplies later days automatically, expiring after its seventh date. History stays read-only, with no separate settings screen.

For 2,000 mL and 10 attempts, Record attempt adds 200 mL. Whole-mL remainders are distributed across attempts so the completed plan adds exactly the target. Attempts are limited to 1–32, at least 1 mL each, with at least 15 minutes between calculated reminder slots. Slots are evenly spaced inside the day's waking periods, excluding sleep (including daytime sleep); missed slots are skipped without a catch-up burst. Notifications only suggest the next attempt's amount; intake requires explicit recording. A small Add extra water action preserves the approved ability to record extra intake without changing the locked goal or planned attempt count.

Phase 3 history lists only previous dates with recorded intake, newest first. Tapping a date shows total mL, that day's goal/completion percentage, and timed entries. A day keeps its last configured goal; later days' goal edits cannot change it. Completion may exceed 100% when extra water is added. New entries keep their local recording date so travel cannot move them to another history day. Existing Phase 2 entries migrate using the current timezone; their past goals were not recorded, so history displays an unavailable goal instead of estimating one.

The earlier Phase 2 custom-amount Home has been superseded by the approved attempt plan. Defaults of 2,000 mL and ten attempts are editable user choices, not medical recommendations.

The MVP uses Android's built-in SQLite through SQLiteOpenHelper, avoiding new dependencies. Schema version 3 stores intake timestamps, amounts, local recording dates, an attempt flag, daily goal/plan snapshots, and a dated one-day/seven-day template in the app's private database. Database operations run on a single background executor. Intake belongs to its local recording date; Home uses the current local date. Entry times display in the phone's current timezone. Home refreshes on resume and every 30 seconds while visible. Previous events are retained for history; uninstalling clears local data. Android cloud backup is disabled and the database is excluded from cloud backup and device transfer rules.

### Reminders

Configurable local notifications work offline, can be disabled, respect quiet hours including overnight ranges, and avoid excessive notifications. Choose scheduling APIs according to required precision and Android restrictions when this phase begins.

The sleep-first revision replaces manual interval/quiet-hour settings with calculated slots from the daily plan and sleeping hours. Plan setup requests notification permission only after saving the plan; denial leaves manual recording useful. Reminders can be toggled on/off from Home. Sleep times can be corrected separately; this reschedules reminders without changing locked intake plans. Scheduling still uses one inexact AlarmManager alarm at a time, with no exact-alarm permission or new dependencies. Android may delay delivery; sleep is rechecked at delivery, and no reminder is posted for an expired or completed plan. Future-only slots are used after late setup/resume. Reboot/update/time changes reconcile the schedule; force-stop requires reopening.

Reminder configuration is stored locally, separately from hydration records. Dedicated reminder model/domain/data/scheduling/notification/presentation packages and `ui/reminders` separate each responsibility. Turning reminders off cancels the alarm and displayed reminder. A single notification is reused; tapping it opens Drink, with no automatic intake recording.

### Optional drinking detection

Start with an explicitly enabled camera session and visible start/stop controls. Analyze movement over time rather than relying on a single photograph. Hand-to-mouth movements may also be eating, scratching, or other actions; uncertainty must remain visible.

Pipeline: CameraX frames → sampling/preprocessing → pose or hand landmarks → temporal analysis → gesture candidate → confidence and deduplication → user confirmation or approved auto-log policy → hydration history.

A gesture cannot verify swallowing, identify the liquid reliably, or measure exact volume. Low-confidence candidates must not automatically add intake. Users can confirm, reject, and correct events. Estimated amounts must be presented as estimates or configured serving sizes.

Build camera preview first, then landmarks, then a rule-based temporal detector with cooldown, and only then evaluate a trained classifier using licensed, consented data and drinking/non-drinking examples.

Initial experimentation targets are approximately 640 × 480 analysis frames and 5–10 analyzed frames per second. These are starting points, not performance guarantees. Measure latency, memory, responsiveness, and battery behavior on actual hardware; reuse buffers and avoid unnecessary bitmap allocations.

## Technology and architecture

Confirmed direction: Kotlin, Jetpack Compose, Gradle, Android SDK, and local notifications.

Proposed components, added only as needed:

- Room for intake events and history.
- DataStore for goals and preferences.
- ViewModel, StateFlow, and coroutines for UI state and background work.
- CameraX for preview and analysis.
- MediaPipe for landmarks, subject to compatibility and device measurements.
- LiteRT/TensorFlow Lite for a small classifier if evaluation justifies it.
- WorkManager or AlarmManager for reminders after scheduling requirements are settled.

Keep presentation, hydration rules, local persistence, reminders, and detection separate. Detection emits candidates; a separate domain component decides whether intake is recorded. Introduce structure only when features require it.

Existing hydration code is organized into `hydration/model` (record and screen data), `hydration/domain/HydrationRules` (validation/calculations), `hydration/data/HydrationStore` (SQLite), and `hydration/presentation/HydrationController` (named actions, navigation state, background loading). Screens live in `ui/home` and `ui/history`; `ui/DrinkApp` connects them to actions. `MainActivity` hosts lifecycle/navigation restoration and platform notification permission requests. Tests mirror the domain/data packages. Phase 4 reminders use their own packages and controller.

## Screen direction

- Home: Drink name, today's intake, goal, progress, Add Water, reminder status, and history/settings entry points.
- History: previous dates, read-only daily totals, completion percentage, and individual entries.
- Goal changes and additional water remain on Home; no separate settings screen in Phase 3.
- Camera (later): preview, start/stop, status, and candidate events with useful uncertainty information.

Build screens incrementally. Phase 1 needs only a launchable starter screen.

## Inspected project configuration

Inspection on 2026-10-08 found:

- Project name: Drink; module: app; namespace/application ID: com.drink.app.
- Gradle wrapper: 9.2.1; Android Gradle Plugin: 9.0.1.
- Compile SDK: 36.1; target SDK: 36; minimum SDK: 26.
- Gradle daemon toolchain configured for Java 21; Java source/target compatibility: 11.
- Kotlin version is not explicitly declared in the version catalog; effective Kotlin tooling remains to be verified.
- Compose is not configured. Existing UI dependencies are AppCompat and Material Views.
- No application Activity or launcher entry exists in the inspected source/manifest.
- local.properties points to the local Android SDK and must remain untracked.

These describe the original skeleton. Phase 1 now adds MainActivity and its launcher entry, enables Compose, and uses Material 3 for a welcome screen with system light/dark appearance and safe drawing insets. The XML host theme uses NoActionBar so Compose owns the screen.

The Compose compiler plugin is pinned to 2.2.10 to match AGP 9.0.1's built-in Kotlin tooling; no separate Kotlin Android plugin is added. Compose BOM 2025.08.00 and Activity Compose 1.10.1 provide the starter UI dependencies. AppCompat and Material Views template dependencies have been removed; Core KTX and test dependencies remain.

`gradlew.bat --version` ran successfully using Android Studio's bundled Java 21.0.9 and reported Gradle 9.2.1. Its reported Kotlin 2.2.20 is Gradle's embedded Kotlin, distinct from the app compiler. SDK platform android-36.1 is installed and emulator Medium_Phone_API_36.1 is available. The product owner will launch and test the app through Android Studio. Device launch has not been verified.

Compatibility references: [AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes), [Compose compiler migration](https://kotlinlang.org/docs/compose-compiler-migration-guide.html), [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping), and [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity).

## Open decisions

- Whether future versions track beverages besides water.
- Initial goal and serving-size defaults; goals remain user-configurable.
- UI style details and target test device.
- Reminder precision and handling of timezone changes.
- Export behavior: no export is implemented in Phase 2. App database backup and transfer are excluded to keep intake on-device.

Migration preserves old entries as extra/manual records. If today already has legacy records, its existing goal stays locked in a compatibility plan of ten attempts (limited by target mL), expiring today. Sleep setup is still required; old interval preferences never opt the user into the revised schedule.


Approved setup UI revision: one white screen with black text; goal first, attempts beside live mL-per-attempt, today/7-day choice, typed sleep/wake numbers with explicit AM/PM controls (no clock picker), and live reminder-time/amount list before Start plan. Passed slots are labelled. Setup drafts stay unsaved until Start; reminder hours save successfully before plan save and permission request. History enhancements are deferred; existing history and records are preserved.


Plan-saving logic revision: SavePlan is the explicit domain action. PlanRules is shared by setup preview and saving; it checks the chosen 1/7-day duration rather than always checking seven days. Schema 4 adds canonical sleep_hours in SQLite so Start plan commits the target, attempts, dated template, daily snapshot and sleeping hours in one transaction. Any failed or locked save preserves the previous plan and hours. ReminderStore reads canonical SQLite hours, falling back to legacy preferences until hours are saved; reminder enabled state and alarm timestamps remain in preferences. Permission/scheduling happen after successful saving. No event bus, backend, dependencies, UI redesign or detection added.

Attempt recording refinement: preview amount ranges use the same allocation rule as persisted attempts. A named RecordAttempt action takes the displayed plan/count and recording timestamp. Storage checks the plan and count again inside its transaction, rejecting stale snapshots without inserting intake. Busy UI actions remain disabled during writes; separate accepted taps after refresh can record separate drinks. Extra water never consumes a planned attempt; remaining mL is clamped at zero and completion may exceed 100 percent. No schema or reminder-policy change in this step.

Daily rollover verification: Home derives the active local calendar date from the current time/zone on every load. Day reads do not create plan snapshots; recorded entries snapshot their day transactionally. Weekly plans cover seven inclusive dates and do not extend when days are skipped. Stale actions may recover to the new day, and that date change must also reconcile reminders even when the attempted write failed. Controller time is injectable for tests; production uses current device time and timezone. No device-clock mutation, schema change, or reminder-delivery expansion in this feature.

Reminder-delivery refinement: a testable ReminderEngine coordinates shared slot rules, persisted delivery state, plan reads and Android platform calls. Slots align to displayed whole minutes. Delivery rechecks current plan/count, sleep, permission, scheduled slot and a 15-minute cooldown. A small bounded recent-slot ledger survives reopening/clock changes to avoid duplicate alerts; no intake is recorded. Obsolete notifications clear when their plan date/count no longer matches. Disabling reminders always cancels, including when changed timezone/DST makes the old schedule invalid. Existing inexact AlarmManager policy and permissions remain; actual Doze/OEM delivery timing requires physical-device testing.

History verification refinement: history remains read-only and shows only recorded dates before the current local date, newest first. Detail selection is validated against that list and cleared when timezone/clock changes make it ineligible. History uses the shared white/black theme and shared explicit AM/PM time formatter; entry dates remain their stored recording dates while times display in the current phone timezone. Failure does not falsely show the empty-history success message. Tests use isolated databases and populated test-only UI; no history editing/deletion, schema, backend or dependencies added.
History stays reachable from plan setup after expiry, so viewing old records does not require starting another plan. Its navigation does not depend on reminder permission or sleeping-hour setup.
