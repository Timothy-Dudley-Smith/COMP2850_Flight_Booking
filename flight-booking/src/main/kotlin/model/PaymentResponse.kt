package com.flightsystem.model

data class PaymentResponse(
    val success: Boolean,
    val message: String,
    val paymentId: String?,
    val bookingId: Int?
)