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

Dev notification test: Test notification posts an immediate, explicitly labelled sample through the existing water-reminder channel with a separate tagged ID. It requests notification permission when necessary and provides a settings link if denied or blocked. Repeated taps replace that sample; tapping the sample opens Drink without recording intake. It does not enable reminders, schedule alarms or change the delivery ledger/cooldown. This checks permission/channel/display, not alarm timing or Doze delivery.

Configurable local notifications work offline, can be disabled, respect quiet hours including overnight ranges, and avoid excessive notifications. Choose scheduling APIs according to required precision and Android restrictions when this phase begins.

The sleep-first revision replaces manual interval/quiet-hour settings with calculated slots from the daily plan and sleeping hours. Plan setup requests notification permission only after saving the plan; denial leaves manual recording useful. Reminders can be toggled on/off from Home. Sleep times can be corrected separately; this reschedules reminders without changing locked intake plans. Scheduling still uses one inexact AlarmManager alarm at a time, with no exact-alarm permission or new dependencies. Android may delay delivery; sleep is rechecked at delivery, and no reminder is posted for an expired or completed plan. Future-only slots are used after late setup/resume. Reboot/update/time changes reconcile the schedule; force-stop requires reopening.

Reminder configuration is stored locally, separately from hydration records. Dedicated reminder model/domain/data/scheduling/notification/presentation packages and `ui/reminders` separate each responsibility. Turning reminders off cancels the alarm and displayed reminder. A single notification is reused; tapping it opens Drink, with no automatic intake recording.

### Future custom reminder audio and Tablets tab

Planned Phase 8 adds selectable reminder sounds and personal audio imports, including an existing recording of a child's voice asking the user to drink. Owner confirmed playback must not exceed 45 seconds. Duration selection and early Stop behavior still need agreement. Audio stays local. Investigate the Android playback approach and verify that playback stops within the cap; retain useful reminders when custom audio cannot play. Dev will provide a sample playback check. In-app voice recording is not specified by this request.

Planned Phase 9 changes navigation to separate Water and Tablets tabs. Owner confirmed Tablets means medicine reminders. Define medicine fields, scheduling and recording rules before implementation, preserving Water records and keeping the two features' logic separate. Broader black/white page design will be reviewed with that layout. Release preparation moves to Phase 10. Existing camera/detection phases stay on the roadmap.

### Optional drinking detection

Owner storage requirement (2026-10-14): camera detection saves no videos, photos, selected frames or landmark history. Frames are analyzed in reusable memory buffers and released when the session stops; only an explicitly confirmed hydration entry is persisted. No daily media-cleanup job is needed because detection creates no media files. Frame capture for future training would require a separate explicit decision. Keep existing hydration history and preferences.

APK delivery uses separate armeabi-v7a, arm64-v8a, x86 and x86_64 builds with no universal APK. A phone receives only its own processor's native libraries, while Android Studio can use the appropriate emulator build. This reduces package storage without removing offline detection or older 32-bit ARM support. APK size is distinct from installed storage and runtime RAM; measure those separately.

Phase 6 prototype revision: bundle MediaPipe Pose Landmarker Lite version 1 and tasks-vision 0.10.32 with CPU inference. CameraX RGBA analysis requests 640x480, keeps only the latest frame, samples at most 5 FPS and reuses image buffers/bitmaps on a single worker. Model input/output stay in memory. Native inference and model loading happen off the UI thread; failures leave manual intake available. Fit-center, orientation-correct, mirrored overlays show confidently tracked mouth/shoulder/arm points. UI exposes analyzed frames and inference latency for local performance checks. See MODEL_NOTES.md for model hash/license and the reproducible runtime privacy adaptation: remote logging classes/transport dependencies are omitted, a no-op logger is used, and merged network permissions are removed.

Gesture rules require visible mouth/shoulders/wrist landmarks at least 0.7 confidence, a sufficiently scaled body, hand-away arming, approach/hold near the mouth for at least 800 ms/four samples, and hand withdrawal for 200 ms. Confidence is landmark visibility/presence, not a calibrated probability of drinking. Missing/low-confidence/out-of-order/stale frames cannot complete a gesture; candidates have a 15-second cooldown and one pending confirmation. This cannot distinguish all eating/scratching gestures; every candidate remains unverified until the user confirms.

Confirmation offers the current planned serving as an estimate, with an editable amount and Reject. No write occurs on detection/rejection. Confirming the planned amount consumes one planned attempt; a different amount (or completed attempts) records extra water. No plan prevents confirmation. Schema 5 adds a nullable candidate_token to intake and a unique index; a domain action checks date/amount, and an atomic transaction rechecks the displayed plan/count before calling the existing recording actions. The token makes repeat confirmations idempotent across retries/reopening. Late/stale snapshots fail without changing intake. Existing manual entries have null tokens and preserve their history/goals. Successful real confirmations reconcile reminders; Dev confirmations do not.

Dev adds Test drinking detection (live landmarks/candidates) and Test drink confirmation (explicitly simulated suggestions) using dev_hydration.db and its virtual date. Dev camera-reminder notifications use a distinct private action to ensure their confirmations also use that sandbox. Production reminders use real local dates/data. Both preserve the opt-in recheck, four-minute limit and background/recreation stopping. Test suggestions are not evidence of recognition accuracy. Real-person drinking/non-drinking accuracy and sustained target-phone latency/memory/battery measurements remain required; Phase 7 tunes/evaluates the baseline rather than claiming verified swallowing or measured volume.

Dev testing hub: expose manual tests for implemented features. Camera setup exercises preview/Start/Stop/the four-minute limit; the same Camera detection switch as Home is available in Dev and changes the real opt-in. Test camera reminder posts a labelled sample with separate tag/ID 102 and the same camera Activity/action as water reminders, honoring opt-in at tap time. Standard sample remains ID 101; neither replaces real reminders or changes their schedule/ledger. An isolated Hydration sandbox reuses PlanSetup, DrinkHome, DrinkHistory, HydrationController and hydration rules against dev_hydration.db, with its own saved test date. Users can try plans/attempt allocation/sleep-slot previews, recording, extras, locking, history, next-day reset and seven-day expiry. Advancing a test day never changes the phone clock. Test records persist separately; no actual reminders are scheduled by sandbox saves. New sandbox forms use default sleeping hours on a new visit. Final physical/timed/background checks remain necessary; no buttons claim to implement future gesture/audio/medicine features. New implemented features should add/update Dev testing access.

Approved Phase 6 step 1: a persisted Camera detection switch on Home/setup defaults off. A water-reminder PendingIntent directly opens a private CameraSessionActivity, which rechecks current opt-in: off opens normal Home; on starts a visible camera session after required camera permission. This opt-in plus explicit notification tap authorizes that session. The Dev sample notification still opens normal Drink. Session initialization starts a monotonic four-minute budget, with countdown and Stop. Stop, Back, pause/background or disposal releases preview; return/recreation requires explicit Start again. Delayed initialization cannot reopen a stopped/expired camera. The same cap applies to manual Dev preview sessions. An explicit new Start begins a new capped session. No hidden/background camera, raw video saving, landmarks, gesture detection, or hydration writes in this step; UI labels the current session as preview-only. Keep future candidate confirmation separate from camera processing.

Development navigation: a top-right Dev button on Home/plan setup opens a dedicated Dev page containing Camera setup. Back from camera returns to Dev, then Back returns to Home/setup. Opening Dev does not request camera permission or start a session. Future testing tools can live here; broader page design changes are deferred.

Phase 5 preview: a Camera setup link opens a separate monochrome screen without starting the camera. Start requests CAMERA permission only when needed and binds a CameraX Preview use case, preferring the front camera with rear fallback. Stop, Back, leaving the foreground and screen disposal unbind that use case; returning or recreation requires Start again. Camera hardware is optional, denial/unavailability leaves hydration usable, and app settings provide permission recovery. This phase has no capture, frame analyzer, microphone, storage, networking or hydration writes. CameraX camera2/lifecycle/view 1.5.3 are the only new direct dependencies. Physical-device memory, responsiveness and battery measurement remain required before detection.

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
