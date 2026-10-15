# Phase 6 bundled pose model and runtime

- Model: Google MediaPipe Pose Landmarker Lite, float16 bundle version 1.
- File: `app/src/main/assets/pose_landmarker_lite.task`, 5,777,746 bytes.
- SHA-256: `59929e1d1ee95287735ddd833b19cf4ac46d29bc7afddbbf6753c459690d574a`.
- [Official download](https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task).
- [Model card](https://storage.googleapis.com/mediapipe-assets/Model%20Card%20BlazePose%20GHUM%203D.pdf) identifies the model license as Apache 2.0. Runtime: `com.google.mediapipe:tasks-vision:0.10.32`, also Apache 2.0.
- [Android integration guide](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/android).

The model is bundled for first-launch offline operation. It estimates pose landmarks, not drinking, liquid volume or swallowing. Drink's separate temporal rules generate unverified candidates requiring confirmation. Single-person positioning, occlusion, framing and lighting affect results.

Camera frames and landmarks are temporary memory only: no video, photos or selected training frames are saved. Stop releases the model and reusable image buffers. There is no daily video deletion task because no videos are created. The model asset itself is retained so detection works offline.

Native libraries are packaged per processor ABI rather than together in one universal APK. Share `app-arm64-v8a-debug.apk` with a compatible 64-bit ARM phone, or `app-armeabi-v7a-debug.apk` for a compatible 32-bit ARM phone. Android Studio selects the matching build for its connected device/emulator. These are debug builds; installed size and runtime memory require separate device measurements.

## Privacy adaptation to the published runtime

The upstream Maven core factory creates a remote usage-metrics logger. `preparePrivateMediaPipe` in `app/build.gradle.kts` reproducibly removes `TasksStatsLoggerFactory`, `TasksStatsProtoLogger` and `RemoteLoggingClient` classes from the pinned core AAR. The local Java factory returns upstream `TasksStatsDummyLogger`. The upstream core's transport dependencies are not included; its protobuf/flogger dependencies are retained. Licenses and all non-logging runtime entries remain intact. Generated adapted artifacts stay under ignored `app/build`.

Manifest merger rules remove Internet and network-state permissions as additional protection. No raw frames or landmark streams are persisted. UI frame/inference counters are session-local diagnostics. Verify the no-op factory, absence of remote logger/transport classes, merged permissions, model hash and native CPU inference when updating MediaPipe.

Source context: [upstream TaskRunner](https://github.com/google-ai-edge/mediapipe/blob/v0.10.32/mediapipe/tasks/java/com/google/mediapipe/tasks/core/TaskRunner.java), [upstream privacy notice](https://github.com/google-ai-edge/mediapipe#privacy-notice).
