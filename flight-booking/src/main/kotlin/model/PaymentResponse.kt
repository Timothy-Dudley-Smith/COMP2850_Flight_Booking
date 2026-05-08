package com.flightsystem.model
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val success: Boolean,
    val message: String,
    val paymentId: String?,
    val bookingId: Int?,
    val returnBookingId: Int? = null,
    // now including loyalty info
    val pointsEarned: Int? = null,
    val pointsUsed: Int? = null,
    val updatedPointsTotal: Int? = null,
    val finalAmountPaid: Double? = null,
)
