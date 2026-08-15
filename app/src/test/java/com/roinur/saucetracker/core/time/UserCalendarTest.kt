package com.roinur.saucetracker.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UserCalendarTest {
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val stockholm = ZoneId.of("Europe/Stockholm")

    @Test
    fun todayUsesTheUsersCalendarDayInsteadOfUtc() {
        val clock = Clock.fixed(Instant.parse("2026-08-14T22:30:00Z"), ZoneId.of("UTC"))

        assertEquals("2026-08-15", UserCalendar.today(stockholm, clock).toString())
    }

    @Test
    fun utcTimestampIsAssignedToTheUsersLocalDay() {
        val day = UserCalendar.dayForUtcTimestamp(
            timestamp = "2026-08-14 22:30:00",
            formatter = timestampFormat,
            zoneId = stockholm
        )

        assertEquals("2026-08-15", day.toString())
    }

    @Test
    fun instantIsAssignedToTheUsersLocalDay() {
        val day = UserCalendar.dayForInstant(
            instant = Instant.parse("2026-12-31T23:30:00Z"),
            zoneId = stockholm
        )

        assertEquals("2027-01-01", day.toString())
    }
}
