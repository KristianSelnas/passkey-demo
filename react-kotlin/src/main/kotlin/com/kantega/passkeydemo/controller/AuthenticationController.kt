package com.kantega.passkeydemo.controller

import com.kantega.passkeydemo.model.dto.LoginFinishRequest
import com.kantega.passkeydemo.model.dto.LoginFinishResponse
import com.kantega.passkeydemo.model.dto.LoginStartRequest
import com.kantega.passkeydemo.security.JwtService
import com.kantega.passkeydemo.service.UserService
import com.kantega.passkeydemo.service.WebAuthnService
import com.kantega.passkeydemo.util.ChallengeStore
import com.kantega.passkeydemo.util.ChallengeStore.PendingAuthentication
import com.webauthn4j.util.Base64UrlUtil
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Login with a passkey that was registered earlier.
 *
 * A WebAuthn authentication takes two round trips to the server:
 * 1. `POST /api/login/start` – the server returns options with a fresh random challenge and the
 *    ids of the user's passkeys. The browser passes them to `navigator.credentials.get()` (via
 *    `startAuthentication()` in @simplewebauthn/browser), and the authenticator signs the
 *    challenge with the private key after the user has verified with biometrics or PIN.
 * 2. `POST /api/login/finish` – the browser sends back the signature (the assertion response).
 *    The server verifies it with the public key stored at registration and issues a JWT.
 *
 * Because the challenge is new for every login, an old signature can not be replayed.
 */
@RestController
@RequestMapping("/api/login")
class AuthenticationController(
    private val userService: UserService,
    private val webAuthnService: WebAuthnService,
    private val challengeStore: ChallengeStore,
    private val jwtService: JwtService
) {

    /**
     * Step 1: Generates the authentication options for an existing user.
     *
     * The challenge is kept in the [ChallengeStore] until [finishLogin].
     *
     * @return 200 with the options for `navigator.credentials.get()`,
     *   404 if the user does not exist, or 400 if the request is invalid
     */
    @PostMapping("/start")
    fun startLogin(@Valid @RequestBody request: LoginStartRequest): ResponseEntity<Any> {
        val user = userService.findByUsername(request.username)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "User not found"))

        val options = webAuthnService.generateAuthenticationOptions(user)

        challengeStore.saveAuthentication(request.username, PendingAuthentication(challenge = options.challenge))

        return ResponseEntity.ok(options)
    }

    /**
     * Step 2: Verifies the signed challenge and logs the user in.
     *
     * Before the signature is checked, the passkey the browser used is looked up among the
     * user's stored passkeys, since its public key is needed for the verification.
     *
     * @return 200 with the user's details and a JWT for the authenticated session,
     *   or 400 if login was not started, the challenge has expired, the passkey does not belong
     *   to the user or verification failed
     */
    @PostMapping("/finish")
    fun finishLogin(@Valid @RequestBody request: LoginFinishRequest): ResponseEntity<Any> {
        // take() removes the challenge, so it can only be used once, even if verification fails
        val pending = challengeStore.takeAuthentication(request.username)
        val user = userService.findByUsername(request.username)

        if (pending == null || user == null) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Login not started or expired"))
        }

        // Find the user's passkey that the browser used
        val credentialId = request.assertionResponse.path("id").asString()
        val credential = user.credentials.find {
            Base64UrlUtil.encodeToString(it.credentialId) == credentialId
        } ?: return ResponseEntity.badRequest().body(mapOf("error" to "Credential not found"))

        // Discoverable passkeys also return the user handle. If present, it must belong to the
        // same user, otherwise the passkey was registered for someone else.
        val userHandle = request.assertionResponse.path("response").path("userHandle").stringValue(null)
        if (userHandle != null && userHandle != Base64UrlUtil.encodeToString(user.userHandle)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Credential not found"))
        }

        // Throws if the signature or any other check fails (handled by ApiExceptionHandler)
        val authentication = webAuthnService.verifyAuthenticationResponse(
            responseJson = request.assertionResponse.toString(), // The browser's response as a JSON string
            expectedChallenge = pending.challenge,
            credential
        )

        userService.updateAfterLogin(credential, authentication)

        // Generate JWT token for authenticated session
        val token = jwtService.generateToken(user.username)

        return ResponseEntity.ok(
            LoginFinishResponse(
                verified = true,
                username = user.username,
                name = user.name,
                token = token
            )
        )
    }
}
