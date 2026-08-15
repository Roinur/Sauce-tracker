package com.roinur.saucetracker.core.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal object UserCalendar {
    fun today(
        zoneId: ZoneId = ZoneId.systemDefault(),
        clock: Clock = Clock.systemUTC()
    ): LocalDate = LocalDate.now(clock.withZone(zoneId))

    fun dayForInstant(
        instant: Instant,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate = instant.atZone(zoneId).toLocalDate()

    fun dayForUtcTimestamp(
        timestamp: String?,
        formatter: DateTimeFormatter,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate? {
        val value = timestamp?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching {
            LocalDateTime.parse(value, formatter)
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(zoneId)
                .toLocalDate()
        }.getOrNull()
    }
}
