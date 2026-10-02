package com.kantega.passkeydemo.util

import com.kantega.passkeydemo.util.ChallengeStore.PendingAuthentication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ChallengeStoreTest {

    /** A clock the test can move forward. */
    private class TestClock(var now: Instant = Instant.parse("2026-01-01T12:00:00Z")) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }

    private val clock = TestClock()
    private val store = ChallengeStore(clock)

    @Test
    fun `a challenge can be taken once`() {
        store.saveAuthentication("ola@example.com", PendingAuthentication("challenge"))

        assertEquals("challenge", store.takeAuthentication("ola@example.com")?.challenge)
        assertNull(store.takeAuthentication("ola@example.com"))
    }

    @Test
    fun `a challenge is valid until the ceremony timeout`() {
        store.saveAuthentication("ola@example.com", PendingAuthentication("challenge"))

        clock.now = clock.now.plus(Duration.ofSeconds(60))

        assertEquals("challenge", store.takeAuthentication("ola@example.com")?.challenge)
    }

    @Test
    fun `an expired challenge is rejected`() {
        store.saveAuthentication("ola@example.com", PendingAuthentication("challenge"))

        clock.now = clock.now.plus(Duration.ofSeconds(61))

        assertNull(store.takeAuthentication("ola@example.com"))
    }

    @Test
    fun `a login challenge cannot be used to finish a registration`() {
        store.saveAuthentication("ola@example.com", PendingAuthentication("challenge"))

        assertNull(store.takeRegistration("ola@example.com"))
    }
}
