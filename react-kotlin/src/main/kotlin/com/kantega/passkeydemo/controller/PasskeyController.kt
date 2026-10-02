package com.kantega.passkeydemo.controller

import com.kantega.passkeydemo.model.dto.PasskeyInfo
import com.kantega.passkeydemo.model.dto.PasskeysResponse
import com.kantega.passkeydemo.service.UserService
import com.webauthn4j.util.Base64UrlUtil
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

/**
 * Lists the logged-in user's passkeys, as shown on the dashboard.
 *
 * Unlike registration and login, this endpoint requires a valid JWT
 * (`Authorization: Bearer <token>`), see SecurityConfig.
 */
@RestController
@RequestMapping("/api/passkeys")
class PasskeyController(
    private val userService: UserService
) {

    /**
     * Returns the user's name and a summary of each passkey: a shortened credential id,
     * the signature counter and the transports.
     *
     * @return 200 with the passkeys, 401 without a valid JWT,
     *   or 404 if the user in the token no longer exists (the database is in-memory)
     */
    @GetMapping
    fun getPasskeys(principal: Principal): ResponseEntity<Any> {
        // Spring Security has validated the JWT. The principal name is its subject: the username
        val username = principal.name

        val user = userService.findByUsername(username)
            ?: return ResponseEntity.status(404).body(mapOf("error" to "User not found"))

        val passkeys = user.credentials.map { credential ->
            // The full credential id is long, so only the first 16 characters are shown
            val credentialIdBase64 = Base64UrlUtil.encodeToString(credential.credentialId)
            val truncatedId = if (credentialIdBase64.length > 16) {
                credentialIdBase64.substring(0, 16) + "..."
            } else {
                credentialIdBase64
            }

            PasskeyInfo(
                id = truncatedId,
                counter = credential.counter,
                transports = credential.transports.toList()
            )
        }

        return ResponseEntity.ok(
            PasskeysResponse(
                passkeys = passkeys,
                name = user.name
            )
        )
    }
}
