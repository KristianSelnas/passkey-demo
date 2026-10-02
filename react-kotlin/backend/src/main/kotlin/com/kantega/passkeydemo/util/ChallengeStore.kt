package com.kantega.passkeydemo.util

import com.kantega.passkeydemo.service.WebAuthnService
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory storage for the state that must survive between the start and finish step
 * of a WebAuthn ceremony.
 *
 * Challenges are single-use: take() removes the entry, so the same challenge can never be
 * verified twice, not even after a failed attempt. Entries older than the ceremony timeout
 * are rejected.
 */
@Component
class ChallengeStore(private val clock: Clock) {

    /** State kept between /register/start and /register/finish. The user does not exist yet. */
    data class PendingRegistration(
        val challenge: String,
        val name: String,
        val userHandle: ByteArray
    )

    /** State kept between /login/start and /login/finish. */
    data class PendingAuthentication(
        val challenge: String
    )

    /** What is stored: the pending state and when it was saved. */
    private data class Stored<T>(val pending: T, val createdAt: Instant)

    private val maxAge = Duration.ofMillis(WebAuthnService.TIMEOUT_MS)

    // Separate maps so a login challenge can never be used to finish a registration, or vice versa
    private val registrations = ConcurrentHashMap<String, Stored<PendingRegistration>>()
    private val authentications = ConcurrentHashMap<String, Stored<PendingAuthentication>>()

    fun saveRegistration(username: String, pending: PendingRegistration) {
        registrations[username] = Stored(pending, clock.instant())
    }

    fun takeRegistration(username: String): PendingRegistration? =
        registrations.remove(username)?.takeIf { !isExpired(it.createdAt) }?.pending

    fun saveAuthentication(username: String, pending: PendingAuthentication) {
        authentications[username] = Stored(pending, clock.instant())
    }

    fun takeAuthentication(username: String): PendingAuthentication? =
        authentications.remove(username)?.takeIf { !isExpired(it.createdAt) }?.pending

    private fun isExpired(createdAt: Instant) = createdAt.plus(maxAge).isBefore(clock.instant())
}
