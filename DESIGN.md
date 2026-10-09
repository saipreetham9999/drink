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

Users can set a daily target in milliliters, configure a typical glass amount, record water intake, view today's progress and previous days, and correct or delete entries. Daily totals must handle local day boundaries correctly.

### Reminders

Configurable local notifications work offline, can be disabled, respect quiet hours including overnight ranges, and avoid excessive notifications. Choose scheduling APIs according to required precision and Android restrictions when this phase begins.

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

## Screen direction

- Home: Drink name, today's intake, goal, progress, Add Water, reminder status, and history/settings entry points.
- History: daily totals, individual entries, correction and deletion.
- Settings: goal, serving size, reminder interval, quiet hours, notification controls, and future detection controls.
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

The Compose compiler plugin is pinned to 2.2.10 to match AGP 9.0.1's built-in Kotlin tooling; no separate Kotlin Android plugin is added. Compose BOM 2025.08.00 and Activity Compose 1.10.1 provide the starter UI dependencies. Existing template dependencies remain in place.

`gradlew.bat --version` ran successfully using Android Studio's bundled Java 21.0.9 and reported Gradle 9.2.1. Its reported Kotlin 2.2.20 is Gradle's embedded Kotlin, distinct from the app compiler. SDK platform android-36.1 is installed and emulator Medium_Phone_API_36.1 is available. The product owner will launch and test the app through Android Studio. Device launch has not been verified.

Compatibility references: [AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes), [Compose compiler migration](https://kotlinlang.org/docs/compose-compiler-migration-guide.html), [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping), and [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity).

## Open decisions

- Whether future versions track beverages besides water.
- Initial goal and serving-size defaults; goals remain user-configurable.
- UI style details and target test device.
- Reminder precision and handling of timezone changes.
- Backup/export behavior: the current manifest enables Android backup; review this against the privacy requirements before local hydration persistence is introduced.
