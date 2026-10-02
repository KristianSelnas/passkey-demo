package com.kantega.passkeydemo.controller

import com.webauthn4j.util.exception.WebAuthnException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Turns failed WebAuthn verifications and invalid request bodies into 400 responses
 * with the same {"error": ...} shape as the rest of the API.
 *
 * The details (bad challenge, wrong origin, invalid signature, ...) are logged on the server,
 * but not sent to the client. Other unexpected errors fall through to Spring Boot's default
 * error handling (500 without the exception message).
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(WebAuthnException::class)
    fun handleWebAuthnException(e: WebAuthnException): ResponseEntity<Map<String, String>> {
        log.warn("Passkey verification failed: {}", e.toString())
        return ResponseEntity.badRequest().body(mapOf("error" to "Passkey verification failed"))
    }

    /** Thrown when a @Valid request body breaks a constraint, e.g. @NotBlank or @Email. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val message = e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(mapOf("error" to message))
    }
}
