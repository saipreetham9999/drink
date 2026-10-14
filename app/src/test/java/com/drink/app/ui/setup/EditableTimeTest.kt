package com.drink.app.ui.setup

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class EditableTimeTest {
    @Test fun noonAndMidnightKeepTheirPeriod() {
        assertEquals(LocalTime.MIDNIGHT, enteredTime("12:00", false))
        assertEquals(LocalTime.NOON, enteredTime("12:00", true))
        assertEquals(LocalTime.of(22, 15), enteredTime("10:15", true))
        assertEquals("12:00 AM", displayTime(LocalTime.MIDNIGHT))
        assertEquals("12:00 PM", displayTime(LocalTime.NOON))
    }
    @Test fun incompleteAndInvalidTimesAreRejected() {
        listOf("", "0:00", "13:00", "10:60", "10", "10:0", "1a:00").forEach {
            assertNull(enteredTime(it, false))
        }
    }
}
