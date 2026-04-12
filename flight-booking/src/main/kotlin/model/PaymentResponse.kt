package com.flightsystem.model
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val success: Boolean,
    val message: String,
    val paymentId: String?,
    val bookingId: Int?
)