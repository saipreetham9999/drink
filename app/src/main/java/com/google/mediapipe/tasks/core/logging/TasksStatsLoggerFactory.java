package com.google.mediapipe.tasks.core.logging;

import android.content.Context;

/** Drink privacy adaptation: replaces the published remote factory with the upstream no-op logger. */
public final class TasksStatsLoggerFactory {
    private TasksStatsLoggerFactory() {}

    public static TasksStatsLogger create(Context context, String taskName, String runningMode) {
        return TasksStatsDummyLogger.create(context, taskName, runningMode);
    }
}
