package com.kantega.passkeydemo.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Issues the JWT that the frontend uses after a successful passkey login.
 *
 * The token contains the username as subject and is signed with HS256. Validation of incoming
 * tokens is handled by Spring Security's OAuth2 resource server (see SecurityConfig).
 */
@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    @Value("\${jwt.expiration}") private val expiration: Duration
) {

    fun generateToken(username: String): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(username)
            .issuedAt(now)
            .expiresAt(now.plus(expiration))
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
