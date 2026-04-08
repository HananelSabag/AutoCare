package com.hananelsabag.autocare.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ReminderCheckWorker.shouldFireToday].
 *
 * Escalation schedule:
 *   daysLeft == 60  (only if daysBeforeExpiry >= 60) → fire
 *   daysLeft == 30                                   → fire
 *   daysLeft in 8..29, (30 - daysLeft) % 7 == 0     → fire weekly (23, 16, 9)
 *   daysLeft in 0..7                                 → fire every run (~twice daily)
 *   daysLeft < 0                                     → never fire (already expired)
 *   daysLeft > daysBeforeExpiry                      → not yet in window
 */
class ReminderCheckWorkerTest {

    // ── Window gate ──────────────────────────────────────────────────────────

    @Test
    fun `shouldFire_at60days_withWindow60_returnsTrue`() {
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 60L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at60days_withWindow30_returnsFalse`() {
        // 60 > 30 → outside window
        assertFalse(ReminderCheckWorker.shouldFireToday(daysLeft = 60L, daysBeforeExpiry = 30))
    }

    // ── 30-day mark ──────────────────────────────────────────────────────────

    @Test
    fun `shouldFire_at30days_returnsTrue`() {
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 30L, daysBeforeExpiry = 60))
    }

    // ── Weekly cadence (30, 23, 16, 9) ──────────────────────────────────────

    @Test
    fun `shouldFire_at23days_returnsTrue`() {
        // (30 - 23) % 7 == 0
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 23L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at16days_returnsTrue`() {
        // (30 - 16) % 7 == 0
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 16L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at9days_returnsTrue`() {
        // (30 - 9) % 7 == 0
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 9L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at45days_returnsFalse`() {
        // 45 is inside the window (45 <= 60) but not a trigger point
        assertFalse(ReminderCheckWorker.shouldFireToday(daysLeft = 45L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at20days_returnsFalse`() {
        // In 8..29 range but (30 - 20) % 7 == 10 % 7 == 3 → not a weekly trigger
        assertFalse(ReminderCheckWorker.shouldFireToday(daysLeft = 20L, daysBeforeExpiry = 60))
    }

    // ── Last 7 days — fire every run ─────────────────────────────────────────

    @Test
    fun `shouldFire_at7days_returnsTrue`() {
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 7L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at5days_returnsTrue`() {
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 5L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at1day_returnsTrue`() {
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 1L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_at0days_returnsTrue`() {
        assertTrue(ReminderCheckWorker.shouldFireToday(daysLeft = 0L, daysBeforeExpiry = 60))
    }

    // ── Past expiry — never fire ─────────────────────────────────────────────

    @Test
    fun `shouldFire_pastExpiry_returnsFalse`() {
        assertFalse(ReminderCheckWorker.shouldFireToday(daysLeft = -1L, daysBeforeExpiry = 60))
    }

    @Test
    fun `shouldFire_farPastExpiry_returnsFalse`() {
        assertFalse(ReminderCheckWorker.shouldFireToday(daysLeft = -365L, daysBeforeExpiry = 60))
    }
}
