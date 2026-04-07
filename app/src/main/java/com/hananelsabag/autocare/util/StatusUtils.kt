package com.hananelsabag.autocare.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class StatusLevel { UNKNOWN, GREEN, YELLOW, RED, EXPIRED }

fun getStatusLevel(expiryMs: Long?): StatusLevel {
    expiryMs ?: return StatusLevel.UNKNOWN
    val days = expiryMs.daysFromNow()
    return when {
        days < 0  -> StatusLevel.EXPIRED
        days <= 7  -> StatusLevel.RED
        days <= 30 -> StatusLevel.YELLOW
        else       -> StatusLevel.GREEN
    }
}

private val israelDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun Long.toFormattedDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(israelDateFormatter)

fun Long.daysFromNow(): Long {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(LocalDate.now(), date)
}

fun LocalDate.toEpochMilli(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
