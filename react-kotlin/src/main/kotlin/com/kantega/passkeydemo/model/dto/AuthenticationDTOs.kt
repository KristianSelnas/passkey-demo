package com.kantega.passkeydemo.model.dto

import jakarta.validation.constraints.NotBlank
import tools.jackson.databind.JsonNode

data class LoginStartRequest(
    @field:NotBlank val username: String
)

/**
 * Options for navigator.credentials.get(), in the JSON form that
 * startAuthentication() in @simplewebauthn/browser expects (PublicKeyCredentialRequestOptionsJSON).
 */
data class AuthenticationOptionsResponse(
    val challenge: String,
    val timeout: Long,
    val rpId: String,
    val allowCredentials: List<CredentialDescriptor>,
    val userVerification: String
)

/** Identifies one of the user's registered passkeys. */
data class CredentialDescriptor(
    val id: String,              // credential id, base64url
    val transports: List<String>,
    val type: String = "public-key"
)

/**
 * assertionResponse is the AuthenticationResponseJSON returned by startAuthentication().
 * It is passed unchanged to webauthn4j, which parses and verifies it.
 */
data class LoginFinishRequest(
    @field:NotBlank val username: String,
    val assertionResponse: JsonNode
)

data class LoginFinishResponse(
    val verified: Boolean,
    val username: String,
    val name: String?,
    val token: String
)
