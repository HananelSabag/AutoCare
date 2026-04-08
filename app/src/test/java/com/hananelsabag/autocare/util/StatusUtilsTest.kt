package com.hananelsabag.autocare.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatusUtilsTest {

    // ── getStatusLevel ───────────────────────────────────────────────────────

    @Test
    fun `getStatusLevel_nullExpiry_returnsUnknown`() {
        assertEquals(StatusLevel.UNKNOWN, getStatusLevel(null))
    }

    @Test
    fun `getStatusLevel_expiredYesterday_returnsExpired`() {
        val yesterday = LocalDate.now().minusDays(1).toEpochMilli()
        assertEquals(StatusLevel.EXPIRED, getStatusLevel(yesterday))
    }

    @Test
    fun `getStatusLevel_expiresIn3Days_returnsRed`() {
        val threeDaysFromNow = LocalDate.now().plusDays(3).toEpochMilli()
        assertEquals(StatusLevel.RED, getStatusLevel(threeDaysFromNow))
    }

    @Test
    fun `getStatusLevel_expiresIn7Days_returnsRed`() {
        val sevenDaysFromNow = LocalDate.now().plusDays(7).toEpochMilli()
        assertEquals(StatusLevel.RED, getStatusLevel(sevenDaysFromNow))
    }

    @Test
    fun `getStatusLevel_expiresIn20Days_returnsYellow`() {
        val twentyDaysFromNow = LocalDate.now().plusDays(20).toEpochMilli()
        assertEquals(StatusLevel.YELLOW, getStatusLevel(twentyDaysFromNow))
    }

    @Test
    fun `getStatusLevel_expiresIn30Days_returnsYellow`() {
        val thirtyDaysFromNow = LocalDate.now().plusDays(30).toEpochMilli()
        assertEquals(StatusLevel.YELLOW, getStatusLevel(thirtyDaysFromNow))
    }

    @Test
    fun `getStatusLevel_expiresIn31Days_returnsGreen`() {
        val thirtyOneDaysFromNow = LocalDate.now().plusDays(31).toEpochMilli()
        assertEquals(StatusLevel.GREEN, getStatusLevel(thirtyOneDaysFromNow))
    }

    @Test
    fun `getStatusLevel_expiresIn60Days_returnsGreen`() {
        val sixtyDaysFromNow = LocalDate.now().plusDays(60).toEpochMilli()
        assertEquals(StatusLevel.GREEN, getStatusLevel(sixtyDaysFromNow))
    }

    @Test
    fun `getStatusLevel_expiresExactlyToday_returnsRed`() {
        // today: daysFromNow() == 0, which is <= 7 → RED
        val today = LocalDate.now().toEpochMilli()
        assertEquals(StatusLevel.RED, getStatusLevel(today))
    }
}
