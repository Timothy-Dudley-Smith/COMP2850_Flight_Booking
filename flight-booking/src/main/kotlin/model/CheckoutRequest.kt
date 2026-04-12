package com.flightsystem.model
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest (
    val holdId: Int,
    val cardholderName: String,
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val billingAddress: String,
    val pointsToRedeem: Int = 0

)