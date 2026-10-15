package com.drink.app.reminders.scheduling

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drink.app.hydration.data.HydrationStore
import com.drink.app.hydration.model.PlanRequest
import com.drink.app.reminders.data.ReminderStore
import com.drink.app.reminders.domain.*
import com.drink.app.reminders.model.ReminderConfig
import com.drink.app.reminders.model.SleepHours
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ReminderAndroidTest {
    private class Fixture(blocked: Boolean = false) : AutoCloseable {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "native-reminder-test"
        val channel = "drink_test_reminders"
        val id = 9100
        val manager = context.getSystemService(NotificationManager::class.java)
        val state: ReminderStore
        val db: HydrationStore
        val native: AndroidReminderPlatform
        var now = ZonedDateTime.parse("2026-10-14T08:00:00-05:00[America/Chicago]")
        var scheduled = 0L
        val plans: ReminderPlans
        val platform: ReminderPlatform
        init {
            context.deleteSharedPreferences(name)
            context.deleteDatabase("$name.db")
            // A blocked channel has its own ID: deleting/recreating a channel retains its old importance.
            val selectedChannel = if (blocked) "${channel}_blocked" else channel
            manager.createNotificationChannel(NotificationChannel(selectedChannel, "Drink test", if (blocked)
                NotificationManager.IMPORTANCE_NONE else NotificationManager.IMPORTANCE_DEFAULT))
            state = ReminderStore(context, name)
            db = HydrationStore(context, "$name.db")
            db.savePlan(PlanRequest(2000, 10, 7), SleepHours(LocalTime.of(22, 0), LocalTime.of(8, 0)), now.toLocalDate())
            state.save(ReminderConfig(enabled = true, sleepConfigured = true))
            native = AndroidReminderPlatform(context, id, selectedChannel, id)
            plans = object : ReminderPlans {
                override fun plan(date: LocalDate) = db.readPlan(date)
                override fun completed(date: LocalDate) = db.recordedAttempts(date)
            }
            // Virtual clock decisions never arm a real alarm at a synthetic timestamp.
            platform = object : ReminderPlatform by native {
                override fun schedule(trigger: Long) { scheduled = trigger }
                override fun cancelAlarm() { scheduled = 0; native.cancelAlarm() }
            }
        }
        fun engine(state: ReminderStore = this.state) = ReminderEngine(state, plans, platform) { now }
        fun due() { now = Instant.ofEpochMilli(state.nextAt).atZone(now.zone) }
        fun active() = manager.activeNotifications.firstOrNull { it.id == id }
        fun awaitVisible(visible: Boolean) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (System.nanoTime() < deadline) {
                if ((active() != null) == visible) return
                Thread.sleep(20)
            }
            assertEquals("Unexpected notification visibility", visible, active() != null)
        }
        override fun close() {
            native.cancelAlarm()
            native.clearNotificationUnless(null, 0, null)
            manager.deleteNotificationChannel(channel)
            manager.deleteNotificationChannel("${channel}_blocked")
            db.close()
            context.deleteDatabase("$name.db")
            context.deleteSharedPreferences(name)
        }
    }

    @Test fun actualPermissionStateIsRespectedAndManualRecordingWorks() {
        Fixture().use { f ->
            val allowed = f.native.allowed()
            val engine = f.engine()
            engine.reconcile()
            if (!allowed) {
                assertEquals(0L, f.state.nextAt)
                assertEquals(0L, f.scheduled)
                assertNull(f.active())
            } else {
                assertTrue(f.state.nextAt > f.now.toInstant().toEpochMilli())
                f.due(); engine.deliver(f.state.nextAt); f.awaitVisible(true)
            }
            assertEquals(0L, f.db.readToday(f.now.toLocalDate()).totalMl)
            f.db.recordAttempt(f.now.toLocalDate(), f.now.toInstant().toEpochMilli(), zone = f.now.zone)
            assertEquals(200L, f.db.readToday(f.now.toLocalDate()).totalMl)
        }
    }

    @Test fun realNotificationClearsAfterRecordingAndDisabling() {
        Fixture().use { f ->
            assertTrue("Grant notification permission before running this test", f.native.allowed())
            val engine = f.engine()
            engine.reconcile(); f.due(); engine.deliver(f.state.nextAt); f.awaitVisible(true)
            val notification = f.active()!!.notification
            assertEquals("Time for 200 mL. Open Drink to record it.", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertNotNull(notification.contentIntent)
            assertEquals(f.context.packageName, notification.contentIntent.creatorPackage)
            assertEquals(0L, f.db.readToday(f.now.toLocalDate()).totalMl)
            f.db.recordAttempt(f.now.toLocalDate(), f.now.toInstant().toEpochMilli(), zone = f.now.zone)
            engine.reconcile(); f.awaitVisible(false)
            f.due(); engine.deliver(f.state.nextAt); f.awaitVisible(true)
            f.state.save(f.state.read().copy(enabled = false))
            engine.reconcile(); f.awaitVisible(false)
            assertEquals(0L, f.state.nextAt)
            assertEquals(0L, f.scheduled)
            assertEquals(1, f.db.readToday(f.now.toLocalDate()).recordedAttempts)
        }
    }

    @Test fun persistedScheduleRestoresAndAndroidPendingAlarmCancels() {
        Fixture().use { f ->
            assertTrue(f.native.allowed())
            f.engine().reconcile()
            val due = f.state.nextAt
            val reopened = ReminderStore(f.context, f.name)
            f.engine(reopened).reconcile()
            assertEquals(due, reopened.nextAt)
            f.native.schedule(System.currentTimeMillis() + 3_600_000)
            val intent = Intent(f.context, ReminderReceiver::class.java).setAction(ReminderScheduler.ACTION)
            assertNotNull(PendingIntent.getBroadcast(f.context, f.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
            f.native.cancelAlarm()
            assertNull(PendingIntent.getBroadcast(f.context, f.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        }
    }

    @Test fun blockedNotificationChannelPreventsScheduling() {
        Fixture(blocked = true).use { f ->
            assertFalse(f.native.allowed())
            f.engine().reconcile()
            assertEquals(0L, f.state.nextAt)
            assertNull(f.active())
        }
    }

    @Test fun androidAlarmBroadcastRunsDeliveryWithoutRecordingWater() {
        Fixture().use { f ->
            assertTrue(f.native.allowed())
            val engine = f.engine()
            engine.reconcile()
            val due = f.state.nextAt
            val done = CountDownLatch(1)
            val failure = AtomicReference<Throwable?>()
            val worker = Executors.newSingleThreadExecutor()
            val action = "${f.context.packageName}.TEST_REMINDER_ALARM"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val pending = goAsync()
                    worker.execute {
                        try { f.due(); engine.deliver(due) } catch (error: Throwable) { failure.set(error) }
                        finally { pending.finish(); done.countDown() }
                    }
                }
            }
            ContextCompat.registerReceiver(f.context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
            val pending = PendingIntent.getBroadcast(f.context, 9101, Intent(action).setPackage(f.context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarms = f.context.getSystemService(AlarmManager::class.java)
            try {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() - 1, pending)
                assertTrue("Android delayed the test alarm beyond 45 seconds", done.await(45, TimeUnit.SECONDS))
                failure.get()?.let { throw AssertionError("Alarm delivery failed", it) }
                f.awaitVisible(true)
                assertEquals(0L, f.db.readToday(f.now.toLocalDate()).totalMl)
                assertEquals(0, f.db.readToday(f.now.toLocalDate()).recordedAttempts)
            } finally {
                alarms.cancel(pending); pending.cancel()
                f.context.unregisterReceiver(receiver)
                worker.shutdownNow()
            }
        }
    }
}
