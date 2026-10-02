package com.kantega.passkeydemo.service

import com.kantega.passkeydemo.model.entity.Credential
import com.kantega.passkeydemo.model.entity.User
import com.kantega.passkeydemo.repository.CredentialRepository
import com.kantega.passkeydemo.repository.UserRepository
import com.kantega.passkeydemo.service.WebAuthnService.VerifiedAuthentication
import com.kantega.passkeydemo.service.WebAuthnService.VerifiedRegistration
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val credentialRepository: CredentialRepository
) {

    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    /**
     * Creates the user together with its first passkey.
     * Only called after the registration response has been verified.
     */
    fun createUserWithCredential(
        username: String,
        name: String,
        userHandle: ByteArray,
        registration: VerifiedRegistration
    ): User {
        val user = User(username = username, name = name, userHandle = userHandle)
        user.credentials.add(
            Credential(
                credentialId = registration.credentialId,
                publicKey = registration.publicKey,
                counter = registration.counter,
                transports = registration.transports,
                backupEligible = registration.backupEligible,
                backupState = registration.backupState,
                user = user
            )
        )
        return userRepository.save(user) // The credential is saved through cascade
    }

    /**
     * Stores what can change between logins: the signature counter (so the next login can detect
     * cloned authenticators) and whether the passkey is currently backed up.
     */
    fun updateAfterLogin(credential: Credential, authentication: VerifiedAuthentication) {
        credential.counter = authentication.newCounter
        credential.backupState = authentication.backupState
        credentialRepository.save(credential)
    }
}
