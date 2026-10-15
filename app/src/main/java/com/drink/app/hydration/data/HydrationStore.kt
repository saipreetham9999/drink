package com.drink.app.hydration.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.drink.app.hydration.domain.HydrationRules
import com.drink.app.hydration.domain.PlanRules
import com.drink.app.hydration.model.*
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.model.SleepHours
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Database access is confined to background executors. Entries remain append-only. */
class HydrationStore(context: Context, databaseName: String = "hydration.db") :
    SQLiteOpenHelper(context, databaseName, null, 4) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE intake (id INTEGER PRIMARY KEY, recorded_at INTEGER NOT NULL, amount_ml INTEGER NOT NULL CHECK(amount_ml > 0), local_date TEXT NOT NULL, is_attempt INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE INDEX intake_date ON intake(local_date)")
        db.execSQL("CREATE TABLE preferences (id INTEGER PRIMARY KEY CHECK(id = 1), goal_ml INTEGER NOT NULL CHECK(goal_ml > 0))")
        db.execSQL("INSERT INTO preferences VALUES (1, 2000)")
        db.execSQL("CREATE TABLE daily_goals (local_date TEXT PRIMARY KEY, goal_ml INTEGER NOT NULL CHECK(goal_ml > 0))")
        createPlans(db)
        createSleepHours(db)
    }

    private fun createPlans(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE daily_plans (local_date TEXT PRIMARY KEY, goal_ml INTEGER NOT NULL, attempts INTEGER NOT NULL, ends_on TEXT NOT NULL)")
        db.execSQL("CREATE TABLE plan_template (id INTEGER PRIMARY KEY CHECK(id = 1), starts_on TEXT NOT NULL, ends_on TEXT NOT NULL, goal_ml INTEGER NOT NULL, attempts INTEGER NOT NULL)")
    }

    private fun createSleepHours(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sleep_hours (id INTEGER PRIMARY KEY CHECK(id = 1), sleep_time TEXT NOT NULL, wake_time TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE intake ADD COLUMN local_date TEXT")
            db.rawQuery("SELECT id, recorded_at FROM intake", null).use { cursor ->
                while (cursor.moveToNext()) db.update("intake", ContentValues().apply {
                    put("local_date", localDate(cursor.getLong(1)).toString())
                }, "id = ?", arrayOf(cursor.getLong(0).toString()))
            }
            db.execSQL("CREATE INDEX intake_date ON intake(local_date)")
            db.execSQL("CREATE TABLE daily_goals (local_date TEXT PRIMARY KEY, goal_ml INTEGER NOT NULL CHECK(goal_ml > 0))")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE intake ADD COLUMN is_attempt INTEGER NOT NULL DEFAULT 0")
            createPlans(db)
            val today = LocalDate.now()
            // Preserve a goal already in use today. Legacy entries are never relabelled as attempts.
            if (counts(db, today).third) {
                val savedGoal = db.rawQuery("SELECT goal_ml FROM daily_goals WHERE local_date = ?", arrayOf(today.toString()))
                    .use { if (it.moveToFirst()) it.getInt(0) else legacyGoal(db) }
                putDay(db, DailyPlan(today, savedGoal, minOf(10, savedGoal), today))
            }
        }
        if (oldVersion < 4) createSleepHours(db)
    }

    fun readSleepHours(): SleepHours? = readableDatabase.rawQuery("SELECT sleep_time, wake_time FROM sleep_hours WHERE id = 1", null).use {
        if (it.moveToFirst()) SleepHours(java.time.LocalTime.parse(it.getString(0)), java.time.LocalTime.parse(it.getString(1))) else null
    }

    fun saveSleepHours(hours: SleepHours) {
        require(hours.sleepTime != hours.wakeTime)
        writableDatabase.execSQL("INSERT OR REPLACE INTO sleep_hours VALUES (1, ?, ?)",
            arrayOf(hours.sleepTime.toString(), hours.wakeTime.toString()))
    }

    fun savePlan(request: PlanRequest, hours: SleepHours, date: LocalDate) {
        require(PlanRules.valid(request, ReminderConfig(sleepConfigured = true,
            sleepTime = hours.sleepTime, wakeTime = hours.wakeTime), date))
        val db = writableDatabase
        db.beginTransaction()
        try {
            writePlan(db, request, date)
            saveSleepHours(hours)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun localDate(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    private fun legacyGoal(db: SQLiteDatabase): Int = db.rawQuery("SELECT goal_ml FROM preferences WHERE id = 1", null)
        .use { it.moveToFirst(); it.getInt(0) }

    private fun counts(db: SQLiteDatabase, date: LocalDate): Triple<Long, Int, Boolean> =
        db.rawQuery("SELECT COALESCE(SUM(amount_ml), 0), COALESCE(SUM(is_attempt), 0), COUNT(*) FROM intake WHERE local_date = ?",
            arrayOf(date.toString())).use { it.moveToFirst(); Triple(it.getLong(0), it.getInt(1), it.getInt(2) > 0) }

    fun readPlan(date: LocalDate): DailyPlan? {
        val db = readableDatabase
        db.rawQuery("SELECT goal_ml, attempts, ends_on FROM daily_plans WHERE local_date = ?", arrayOf(date.toString())).use {
            if (it.moveToFirst()) return DailyPlan(date, it.getInt(0), it.getInt(1), LocalDate.parse(it.getString(2)))
        }
        return db.rawQuery("SELECT goal_ml, attempts, ends_on FROM plan_template WHERE starts_on <= ? AND ends_on >= ?",
            arrayOf(date.toString(), date.toString())).use {
            if (it.moveToFirst()) DailyPlan(date, it.getInt(0), it.getInt(1), LocalDate.parse(it.getString(2))) else null
        }
    }

    private fun putDay(db: SQLiteDatabase, plan: DailyPlan) {
        db.insertWithOnConflict("daily_plans", null, ContentValues().apply {
            put("local_date", plan.date.toString()); put("goal_ml", plan.goalMl)
            put("attempts", plan.attempts); put("ends_on", plan.endsOn.toString())
        }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Could not save daily plan" } }
        db.insertWithOnConflict("daily_goals", null, ContentValues().apply {
            put("local_date", plan.date.toString()); put("goal_ml", plan.goalMl)
        }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Could not save daily goal" } }
    }

    fun readToday(date: LocalDate = LocalDate.now()): HomeState {
        val db = readableDatabase
        val plan = readPlan(date)
        val (total, attempts, recorded) = counts(db, date)
        return HomeState(date, total, plan?.goalMl ?: 0, plan, attempts, recorded)
    }

    fun configurePlan(request: PlanRequest, date: LocalDate = LocalDate.now()) {
        require(HydrationRules.validPlan(request.goalMl, request.attempts) && request.days in listOf(1, 7))
        val db = writableDatabase
        db.beginTransaction()
        try {
            writePlan(db, request, date)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun writePlan(db: SQLiteDatabase, request: PlanRequest, date: LocalDate) {
        check(!counts(db, date).third) { "Today's plan is locked after the first recorded drink" }
        val end = date.plusDays(request.days - 1L)
        // Replace the upcoming template, while preserving every recorded day's plan and goal.
        val untouchedFuture = "local_date > ? AND local_date NOT IN (SELECT local_date FROM intake)"
        db.delete("daily_plans", untouchedFuture, arrayOf(date.toString()))
        db.delete("daily_goals", untouchedFuture, arrayOf(date.toString()))
        db.insertWithOnConflict("plan_template", null, ContentValues().apply {
            put("id", 1); put("starts_on", date.toString()); put("ends_on", end.toString())
            put("goal_ml", request.goalMl); put("attempts", request.attempts)
        }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Could not save plan template" } }
        putDay(db, DailyPlan(date, request.goalMl, request.attempts, end))
    }

    fun recordedAttempts(date: LocalDate): Int = counts(readableDatabase, date).second

    fun recordAttempt(expectedDate: LocalDate, recordedAt: Long = System.currentTimeMillis(),
        expectedCompleted: Int? = null, expectedPlan: DailyPlan? = null, zone: ZoneId = ZoneId.systemDefault()) {
        val date = localDate(recordedAt, zone)
        check(date == expectedDate) { "The day changed. Set or review today's plan." }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val plan = checkNotNull(readPlan(date)) { "Set a daily plan first" }
            val completed = counts(db, date).second
            check(expectedPlan == null || expectedPlan == plan) { "The plan changed. Review it before recording." }
            check(expectedCompleted == null || expectedCompleted == completed) { "Intake changed. Review it before recording." }
            check(completed < plan.attempts) { "All planned attempts are recorded" }
            putDay(db, plan)
            insertEntry(db, date, recordedAt, HydrationRules.attemptAmount(plan.goalMl, plan.attempts, completed), true)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun addExtra(amountMl: Int, expectedDate: LocalDate, recordedAt: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()) {
        require(amountMl in 1..100000)
        val date = localDate(recordedAt, zone)
        check(date == expectedDate) { "The day changed. Review today's plan." }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val plan = checkNotNull(readPlan(date)) { "Set a daily plan first" }
            putDay(db, plan)
            insertEntry(db, date, recordedAt, amountMl, false)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun insertEntry(db: SQLiteDatabase, date: LocalDate, timestamp: Long, amount: Int, attempt: Boolean) {
        db.insertOrThrow("intake", null, ContentValues().apply {
            put("recorded_at", timestamp); put("amount_ml", amount); put("local_date", date.toString()); put("is_attempt", if (attempt) 1 else 0)
        })
    }

    fun readHistory(today: LocalDate = LocalDate.now()): List<HistoryDay> = readableDatabase.rawQuery(
        "SELECT i.local_date, SUM(i.amount_ml), g.goal_ml FROM intake i LEFT JOIN daily_goals g ON i.local_date = g.local_date WHERE i.local_date < ? GROUP BY i.local_date ORDER BY i.local_date DESC",
        arrayOf(today.toString()),
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(HistoryDay(LocalDate.parse(cursor.getString(0)), cursor.getLong(1),
        if (cursor.isNull(2)) null else cursor.getInt(2))) } }

    fun readDay(date: LocalDate): DayIntake {
        val db = readableDatabase
        val entries = db.rawQuery("SELECT id, recorded_at, amount_ml FROM intake WHERE local_date = ? ORDER BY recorded_at, id", arrayOf(date.toString()))
            .use { cursor -> buildList { while (cursor.moveToNext()) add(IntakeEntry(cursor.getLong(0), cursor.getLong(1), cursor.getInt(2))) } }
        val goal = db.rawQuery("SELECT goal_ml FROM daily_goals WHERE local_date = ?", arrayOf(date.toString())).use { if (it.moveToFirst()) it.getInt(0) else null }
        return DayIntake(HistoryDay(date, entries.sumOf { it.amountMl.toLong() }, goal), entries)
    }
}
