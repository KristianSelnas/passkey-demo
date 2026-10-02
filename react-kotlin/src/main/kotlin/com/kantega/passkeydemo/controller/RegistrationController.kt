package com.kantega.passkeydemo.controller

import com.kantega.passkeydemo.model.dto.RegistrationFinishRequest
import com.kantega.passkeydemo.model.dto.RegistrationFinishResponse
import com.kantega.passkeydemo.model.dto.RegistrationStartRequest
import com.kantega.passkeydemo.service.UserService
import com.kantega.passkeydemo.service.WebAuthnService
import com.kantega.passkeydemo.util.ChallengeStore
import com.kantega.passkeydemo.util.ChallengeStore.PendingRegistration
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Registration: creates a new user and their first passkey.
 *
 * A WebAuthn registration (the "create" ceremony) takes two round trips to the server:
 * 1. `POST /api/register/start` – the server returns options with a fresh random challenge.
 *    The browser passes them to `navigator.credentials.create()` (via `startRegistration()`
 *    in @simplewebauthn/browser), and the authenticator creates a new key pair after the user
 *    has verified with biometrics or PIN.
 * 2. `POST /api/register/finish` – the browser sends back the new public key and the signed
 *    challenge (the attestation response). The server verifies it and stores the public key.
 *
 * The private key never leaves the authenticator. The server only ever sees the public key.
 */
@RestController
@RequestMapping("/api/register")
class RegistrationController(
    private val userService: UserService,
    private val webAuthnService: WebAuthnService,
    private val challengeStore: ChallengeStore
) {

    /**
     * Step 1: Generates the registration options for a new user.
     *
     * The challenge, name and user handle are kept in the [ChallengeStore] until [finishRegistration].
     *
     * @return 200 with the options for `navigator.credentials.create()`,
     *   409 if the username is already registered, or 400 if the request is invalid
     */
    @PostMapping("/start")
    fun startRegistration(@Valid @RequestBody request: RegistrationStartRequest): ResponseEntity<Any> {
        // Only new users can register here. Otherwise, anyone could add their own passkey
        // to an existing account and log in as that user.
        if (userService.existsByUsername(request.username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "User already registered"))
        }

        // The user is not created until the registration is verified in /finish.
        // Until then, the challenge and the user's details are kept in the ChallengeStore.
        val userHandle = webAuthnService.generateUserHandle()
        val options = webAuthnService.generateRegistrationOptions(request.username, request.name, userHandle)

        challengeStore.saveRegistration(
            request.username,
            PendingRegistration(challenge = options.challenge, name = request.name, userHandle = userHandle)
        )

        return ResponseEntity.ok(options)
    }

    /**
     * Step 2: Verifies the browser's response and creates the user together with the passkey.
     *
     * @return 200 with `verified = true` if the passkey was stored,
     *   or 400 if registration was not started, the challenge has expired or verification failed
     */
    @PostMapping("/finish")
    fun finishRegistration(@Valid @RequestBody request: RegistrationFinishRequest): ResponseEntity<Any> {
        // take() removes the challenge, so it can only be used once, even if verification fails
        val pending = challengeStore.takeRegistration(request.username)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Registration not started or expired"))

        // Throws if the response is invalid (handled by ApiExceptionHandler)
        val registration = webAuthnService.verifyRegistrationResponse(
            request.attestationResponse.toString(), // The browser's response as a JSON string
            pending.challenge
        )

        userService.createUserWithCredential(
            username = request.username,
            name = pending.name,
            userHandle = pending.userHandle,
            registration = registration
        )

        return ResponseEntity.ok(RegistrationFinishResponse(verified = true))
    }
}
