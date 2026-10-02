package com.kantega.passkeydemo.repository

import com.kantega.passkeydemo.model.entity.Credential
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CredentialRepository : JpaRepository<Credential, UUID>
