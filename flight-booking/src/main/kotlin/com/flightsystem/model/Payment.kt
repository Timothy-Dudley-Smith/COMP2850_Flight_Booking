package com.flightsystem.model

data class Payment(
    val paymentId: String,
    val amount: Double,
    val status: String
)