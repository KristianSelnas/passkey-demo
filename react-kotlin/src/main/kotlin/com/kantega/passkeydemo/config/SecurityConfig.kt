package com.kantega.passkeydemo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Security model:
 * - Registration and login (the WebAuthn ceremonies) are public.
 * - A successful passkey login returns a JWT (see JwtService).
 * - All other endpoints require "Authorization: Bearer <token>", validated by Spring Security's
 *   OAuth2 resource server. Missing or invalid tokens give 401.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/register/**", "/api/login/**", "/error").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt { } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }

        return http.build()
    }

    /**
     * The H2 console is only available in the dev profile. It runs in frames and posts forms,
     * so it needs relaxed frame options and no CSRF protection.
     */
    @Bean
    @Profile("dev")
    @Order(0) // Checked before the main filter chain
    fun h2ConsoleFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/h2-console/**")
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }
            .csrf { it.disable() }

        return http.build()
    }

    /** HMAC key for signing and validating JWTs. jwt.secret is base64-encoded (at least 256 bits). */
    @Bean
    fun jwtSecretKey(@Value("\${jwt.secret}") secret: String): SecretKey =
        SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256")

    @Bean
    fun jwtEncoder(jwtSecretKey: SecretKey): JwtEncoder =
        NimbusJwtEncoder.withSecretKey(jwtSecretKey).build()

    @Bean
    fun jwtDecoder(jwtSecretKey: SecretKey): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey).build()
}
