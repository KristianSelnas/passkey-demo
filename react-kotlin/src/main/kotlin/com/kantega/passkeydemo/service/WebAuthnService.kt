package com.kantega.passkeydemo.service

import com.kantega.passkeydemo.model.dto.AuthenticationOptionsResponse
import com.kantega.passkeydemo.model.dto.AuthenticatorSelection
import com.kantega.passkeydemo.model.dto.CredentialDescriptor
import com.kantega.passkeydemo.model.dto.PublicKeyCredentialParam
import com.kantega.passkeydemo.model.dto.RegistrationOptionsResponse
import com.kantega.passkeydemo.model.dto.RelyingParty
import com.kantega.passkeydemo.model.dto.UserEntity
import com.kantega.passkeydemo.model.entity.Credential
import com.kantega.passkeydemo.model.entity.User
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.attestation.authenticator.AAGUID
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.authenticator.COSEKey
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.util.Base64UrlUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom

/**
 * Generates WebAuthn options and verifies the browser's responses using webauthn4j.
 *
 * Verification failures are thrown as webauthn4j exceptions (subclasses of WebAuthnException)
 * and turned into an HTTP 400 by ApiExceptionHandler.
 */
@Service
class WebAuthnService(
    @Value("\${webauthn.rp.name}") private val rpName: String,
    @Value("\${webauthn.rp.id}") private val rpId: String,
    @Value("\${webauthn.origin}") private val origin: String
) {

    companion object {
        // How long the browser waits for the user, and how long the challenge is valid on the server
        const val TIMEOUT_MS = 60_000L

        // Signature algorithms we accept for new passkeys, in order of preference
        private val SUPPORTED_ALGORITHMS = listOf(
            COSEAlgorithmIdentifier.ES256,
            COSEAlgorithmIdentifier.RS256
        )
    }

    private val secureRandom = SecureRandom()
    // "Non-strict" means attestation statements are not checked against trusted certificates.
    // That is fine here, since we ask for attestation "none". All other checks are still done.
    private val webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager()
    private val objectConverter = ObjectConverter()

    /**
     * The challenge is random bytes that the authenticator signs. Because it is new for every
     * ceremony and can only be used once, a signed response can not be replayed later.
     */
    fun generateChallenge(): Challenge {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return DefaultChallenge(bytes)
    }

    /**
     * The user handle identifies the user towards the authenticator (user.id in the options).
     * The spec recommends random bytes (max 64) that contain no personal information.
     */
    fun generateUserHandle(): ByteArray {
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    // ----- Registration -----

    /**
     * Creates the options the browser passes to `navigator.credentials.create()`.
     *
     * They tell the authenticator which site the passkey is for (the relying party), who the user is,
     * which signature algorithms we accept and that the user must be verified. All binary values
     * are base64url-encoded, so the options can be sent as JSON.
     *
     * @param username shown to the user when choosing a passkey, in our case the user's email
     * @param name the user's display name
     * @param userHandle stable id for the user, stored by the authenticator with the passkey (user.id)
     */
    fun generateRegistrationOptions(username: String, name: String, userHandle: ByteArray): RegistrationOptionsResponse {
        val challenge = generateChallenge()

        return RegistrationOptionsResponse(
            challenge = Base64UrlUtil.encodeToString(challenge.value),
            rp = RelyingParty(id = rpId, name = rpName),
            user = UserEntity(
                id = Base64UrlUtil.encodeToString(userHandle),
                name = username,
                displayName = name
            ),
            pubKeyCredParams = SUPPORTED_ALGORITHMS.map { PublicKeyCredentialParam(alg = it.value) },
            timeout = TIMEOUT_MS,
            authenticatorSelection = AuthenticatorSelection(
                // Ask for a discoverable credential (a "resident key"), which is stored on the
                // authenticator together with the user handle. This is what is usually called a passkey.
                // "preferred" means older security keys that can't store it can still register.
                residentKey = "preferred",
                // The passkey is the only factor, so the authenticator must verify the user
                // (biometrics or PIN), not just check that someone touched it
                userVerification = "required"
            ),
            // We don't need proof of which authenticator model was used
            attestation = "none"
        )
    }

    /**
     * Verifies the RegistrationResponseJSON from the browser and returns the new passkey.
     *
     * webauthn4j checks among other things that:
     * - the challenge is the one we issued, and the origin and rp id match this application
     * - the user was present and verified (UP and UV flags)
     * - the public key uses one of the algorithms we asked for
     */
    fun verifyRegistrationResponse(responseJson: String, expectedChallenge: String): VerifiedRegistration {
        val serverProperty = ServerProperty.builder()
            .origin(Origin(origin))
            .rpId(rpId)
            .challenge(DefaultChallenge(Base64UrlUtil.decode(expectedChallenge)))
            .build()
        val registrationParameters = RegistrationParameters(
            serverProperty,
            SUPPORTED_ALGORITHMS.map { PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, it) },
            true, // userVerificationRequired
            true  // userPresenceRequired
        )

        // Parse and verify in one step. Throws if any check fails.
        val registrationData = webAuthnManager.verifyRegistrationResponseJSON(responseJson, registrationParameters)

        val authenticatorData = registrationData.attestationObject!!.authenticatorData
        val attestedCredentialData = authenticatorData.attestedCredentialData!!

        return VerifiedRegistration(
            credentialId = attestedCredentialData.credentialId,
            // The public key is stored in COSE format (CBOR-encoded), as received from the authenticator
            publicKey = objectConverter.cborMapper.writeValueAsBytes(attestedCredentialData.coseKey),
            counter = authenticatorData.signCount,
            transports = registrationData.transports.orEmpty().map { it.value }.toSet(),
            backupEligible = authenticatorData.isFlagBE,
            backupState = authenticatorData.isFlagBS
        )
    }

    // ----- Authentication -----

    /**
     * Creates the options the browser passes to `navigator.credentials.get()`.
     *
     * They contain a new challenge for the authenticator to sign, and the ids of the passkeys
     * the user has registered, so the browser only offers those.
     */
    fun generateAuthenticationOptions(user: User): AuthenticationOptionsResponse {
        val challenge = generateChallenge()

        return AuthenticationOptionsResponse(
            challenge = Base64UrlUtil.encodeToString(challenge.value),
            timeout = TIMEOUT_MS,
            rpId = rpId,
            // Only the user's own passkeys can be used
            allowCredentials = user.credentials.map { credential ->
                CredentialDescriptor(
                    id = Base64UrlUtil.encodeToString(credential.credentialId),
                    transports = credential.transports.toList()
                )
            },
            userVerification = "required"
        )
    }

    /**
     * Verifies the AuthenticationResponseJSON from the browser against the stored passkey.
     *
     * webauthn4j checks among other things that:
     * - the challenge, origin and rp id match
     * - the signature is valid for the stored public key
     * - the user was present and verified (UP and UV flags)
     * - the signature counter has increased (if the authenticator uses one)
     */
    fun verifyAuthenticationResponse(
        responseJson: String,
        expectedChallenge: String,
        credential: Credential
    ): VerifiedAuthentication {
        val serverProperty = ServerProperty.builder()
            .origin(Origin(origin))
            .rpId(rpId)
            .challenge(DefaultChallenge(Base64UrlUtil.decode(expectedChallenge)))
            .build()

        // Rebuild the credential record that webauthn4j verifies against, from what we stored
        // at registration
        val coseKey = objectConverter.cborMapper.readValue(credential.publicKey, COSEKey::class.java)
        val credentialRecord = CredentialRecordImpl(
            NoneAttestationStatement(), // attestation statement: we asked for attestation "none"
            true, // uvInitialized: registration required user verification
            credential.backupEligible,
            credential.backupState,
            credential.counter,
            AttestedCredentialData(AAGUID.ZERO, credential.credentialId, coseKey),
            null, // authenticator extensions: not used
            null, // client data: not needed for authentication
            null, // client extensions: not used
            null  // transports: not needed for verification
        )

        val authenticationParameters = AuthenticationParameters(
            serverProperty,
            credentialRecord,
            null, // allowCredentials: the controller has already matched the credential to the user
            true, // userVerificationRequired
            true  // userPresenceRequired
        )

        // Parse and verify in one step. Throws if any check fails.
        val authenticationData = webAuthnManager.verifyAuthenticationResponseJSON(responseJson, authenticationParameters)

        val authenticatorData = authenticationData.authenticatorData!!
        return VerifiedAuthentication(
            newCounter = authenticatorData.signCount,
            backupState = authenticatorData.isFlagBS
        )
    }

    class VerifiedRegistration(
        val credentialId: ByteArray,
        val publicKey: ByteArray,
        val counter: Long,
        val transports: Set<String>,
        val backupEligible: Boolean,
        val backupState: Boolean
    )

    class VerifiedAuthentication(
        val newCounter: Long,
        val backupState: Boolean
    )
}
