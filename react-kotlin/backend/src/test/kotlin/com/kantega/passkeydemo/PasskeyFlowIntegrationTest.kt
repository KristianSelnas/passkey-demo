package com.kantega.passkeydemo

import com.webauthn4j.data.AttestationConveyancePreference
import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialDescriptor
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialRequestOptions
import com.webauthn4j.data.PublicKeyCredentialRpEntity
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.PublicKeyCredentialUserEntity
import com.webauthn4j.data.ResidentKeyRequirement
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.test.authenticator.webauthn.NoneAttestationAuthenticator
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor
import com.webauthn4j.test.client.ClientPlatform
import com.webauthn4j.util.Base64UrlUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

/**
 * Runs the registration and login flows end to end against the real controllers.
 *
 * The browser and the authenticator are emulated by webauthn4j-test. The responses are sent in
 * the same JSON form as @simplewebauthn/browser produces.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasskeyFlowIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    // Emulated browser on the frontend origin, with an emulated authenticator ("passkey provider")
    private val browser = ClientPlatform(
        Origin("http://localhost:3000"),
        WebAuthnAuthenticatorAdaptor(NoneAttestationAuthenticator())
    )

    @Test
    fun `register and log in with a passkey`() {
        register("ola@example.com", "Ola")

        val token = login("ola@example.com").path("token").asString()

        // The dashboard endpoint accepts the JWT and lists the passkey
        val passkeys = mockMvc.get("/api/passkeys") { header("Authorization", "Bearer $token") }
            .andExpect { status { isOk() } }
            .json()
        assertEquals("Ola", passkeys.path("name").asString())
        assertEquals(1, passkeys.path("passkeys").size())
    }

    @Test
    fun `passkeys endpoint requires a valid token`() {
        mockMvc.get("/api/passkeys").andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/passkeys") { header("Authorization", "Bearer not-a-valid-token") }
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `signature counter is stored after login`() {
        register("kari@example.com", "Kari")

        val token = login("kari@example.com").path("token").asString()
        val counterAfterFirstLogin = passkeyCounter(token)
        login("kari@example.com")
        val counterAfterSecondLogin = passkeyCounter(token)

        assertTrue(counterAfterSecondLogin > counterAfterFirstLogin)
    }

    @Test
    fun `an existing user cannot register a new passkey`() {
        register("per@example.com", "Per")

        postJson("/api/register/start", mapOf("username" to "per@example.com", "name" to "Angriper"))
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `registration requires a name and a valid email address`() {
        postJson("/api/register/start", mapOf("username" to "ikke-en-epost", "name" to "Ola"))
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value(startsWith("username:")) }
            }
        postJson("/api/register/start", mapOf("username" to "ola@example.com", "name" to "  "))
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value(startsWith("name:")) }
            }
    }

    @Test
    fun `a login challenge can only be used once`() {
        register("anne@example.com", "Anne")

        val options = postJson("/api/login/start", mapOf("username" to "anne@example.com")).json()
        val finishRequest = mapOf(
            "username" to "anne@example.com",
            "assertionResponse" to authenticate(options)
        )

        postJson("/api/login/finish", finishRequest).andExpect { status { isOk() } }
        postJson("/api/login/finish", finishRequest).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `a response signed for another challenge is rejected`() {
        register("nils@example.com", "Nils")

        val oldOptions = postJson("/api/login/start", mapOf("username" to "nils@example.com")).json()
        val oldResponse = authenticate(oldOptions)

        // A new login replaces the challenge, so the old response no longer matches
        postJson("/api/login/start", mapOf("username" to "nils@example.com"))
        postJson("/api/login/finish", mapOf("username" to "nils@example.com", "assertionResponse" to oldResponse))
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("Passkey verification failed") }
            }
    }

    // ----- Helpers that play the role of the frontend -----

    private fun register(username: String, name: String) {
        val options = postJson("/api/register/start", mapOf("username" to username, "name" to name))
            .andExpect { status { isOk() } }
            .json()

        // What navigator.credentials.create() does in the browser
        val credential = browser.create(
            PublicKeyCredentialCreationOptions(
                PublicKeyCredentialRpEntity(options.path("rp").path("id").asString(), options.path("rp").path("name").asString()),
                PublicKeyCredentialUserEntity(
                    Base64UrlUtil.decode(options.path("user").path("id").asString()),
                    options.path("user").path("name").asString(),
                    options.path("user").path("displayName").asString()
                ),
                DefaultChallenge(Base64UrlUtil.decode(options.path("challenge").asString())),
                listOf(PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)),
                options.path("timeout").asLong(),
                emptyList(),
                AuthenticatorSelectionCriteria(null, ResidentKeyRequirement.PREFERRED, UserVerificationRequirement.REQUIRED),
                AttestationConveyancePreference.NONE,
                null
            )
        )

        // RegistrationResponseJSON, as returned by startRegistration() in @simplewebauthn/browser
        val response = credential.response!!
        val attestationResponse = mapOf(
            "id" to credential.id,
            "rawId" to credential.id,
            "type" to "public-key",
            "response" to mapOf(
                "clientDataJSON" to Base64UrlUtil.encodeToString(response.clientDataJSON),
                "attestationObject" to Base64UrlUtil.encodeToString(response.attestationObject),
                "transports" to listOf("internal"),
                "publicKeyAlgorithm" to COSEAlgorithmIdentifier.ES256.value
            ),
            "clientExtensionResults" to emptyMap<String, Any>(),
            "authenticatorAttachment" to "platform"
        )

        postJson("/api/register/finish", mapOf("username" to username, "attestationResponse" to attestationResponse))
            .andExpect { status { isOk() } }
    }

    private fun login(username: String): JsonNode {
        val options = postJson("/api/login/start", mapOf("username" to username))
            .andExpect { status { isOk() } }
            .json()

        return postJson("/api/login/finish", mapOf("username" to username, "assertionResponse" to authenticate(options)))
            .andExpect { status { isOk() } }
            .json()
    }

    /** What navigator.credentials.get() does, returned as AuthenticationResponseJSON. */
    private fun authenticate(options: JsonNode): Map<String, Any?> {
        val credential = browser.get(
            PublicKeyCredentialRequestOptions(
                DefaultChallenge(Base64UrlUtil.decode(options.path("challenge").asString())),
                options.path("timeout").asLong(),
                options.path("rpId").asString(),
                options.path("allowCredentials").values().map {
                    PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, Base64UrlUtil.decode(it.path("id").asString()), null)
                },
                UserVerificationRequirement.REQUIRED,
                null
            )
        )

        val response = credential.response!!
        return mapOf(
            "id" to credential.id,
            "rawId" to credential.id,
            "type" to "public-key",
            "response" to mapOf(
                "clientDataJSON" to Base64UrlUtil.encodeToString(response.clientDataJSON),
                "authenticatorData" to Base64UrlUtil.encodeToString(response.authenticatorData),
                "signature" to Base64UrlUtil.encodeToString(response.signature),
                "userHandle" to response.userHandle?.let { Base64UrlUtil.encodeToString(it) }
            ),
            "clientExtensionResults" to emptyMap<String, Any>(),
            "authenticatorAttachment" to "platform"
        )
    }

    private fun passkeyCounter(token: String): Long =
        mockMvc.get("/api/passkeys") { header("Authorization", "Bearer $token") }
            .json()
            .path("passkeys").path(0).path("counter").asLong()

    private fun postJson(path: String, body: Any): ResultActionsDsl =
        mockMvc.post(path) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }

    private fun ResultActionsDsl.json(): JsonNode =
        objectMapper.readTree(andReturn().response.contentAsString)
}
