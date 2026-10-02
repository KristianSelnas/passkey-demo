package com.kantega.passkeydemo.model.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import tools.jackson.databind.JsonNode

data class RegistrationStartRequest(
    @field:NotBlank @field:Email val username: String,
    @field:NotBlank val name: String
)

/**
 * Options for navigator.credentials.create(), in the JSON form that
 * startRegistration() in @simplewebauthn/browser expects (PublicKeyCredentialCreationOptionsJSON).
 * All binary values (challenge, user.id) are base64url-encoded strings.
 */
data class RegistrationOptionsResponse(
    val challenge: String,
    val rp: RelyingParty,
    val user: UserEntity,
    val pubKeyCredParams: List<PublicKeyCredentialParam>,
    val timeout: Long,
    val authenticatorSelection: AuthenticatorSelection,
    val attestation: String
)

/** The relying party is this application. The passkey is bound to the rp id (the domain). */
data class RelyingParty(
    val id: String,
    val name: String
)

data class UserEntity(
    val id: String,          // user handle, base64url
    val name: String,        // username shown in the passkey picker
    val displayName: String
)

data class PublicKeyCredentialParam(
    val alg: Long,           // COSE algorithm identifier, e.g. -7 for ES256
    val type: String = "public-key"
)

data class AuthenticatorSelection(
    val residentKey: String,
    val userVerification: String
)

/**
 * attestationResponse is the RegistrationResponseJSON returned by startRegistration().
 * It is passed unchanged to webauthn4j, which parses and verifies it.
 */
data class RegistrationFinishRequest(
    @field:NotBlank val username: String,
    val attestationResponse: JsonNode
)

data class RegistrationFinishResponse(
    val verified: Boolean
)
