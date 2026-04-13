package com.flightsystem.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val success: Boolean,
    val userId: Int?,
    val email: String?,
    val error: String? = null
)