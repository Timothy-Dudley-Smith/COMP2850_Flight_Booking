package com.flightsystem.model

data class PaymentRequest(
    val cardholderName: String,
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val billingAddress: String
)