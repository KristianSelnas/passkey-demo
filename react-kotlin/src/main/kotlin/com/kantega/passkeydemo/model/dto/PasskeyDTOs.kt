package com.kantega.passkeydemo.model.dto

data class PasskeyInfo(
    val id: String,
    val counter: Long,
    val transports: List<String>?
)

data class PasskeysResponse(
    val passkeys: List<PasskeyInfo>,
    val name: String?
)
