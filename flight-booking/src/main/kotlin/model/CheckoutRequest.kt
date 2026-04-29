package com.flightsystem.model
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest (
    val holdId: Int,
    val returnHoldId: Int? = null,
    val cardholderName: String,
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val billingAddress: String,
    val pointsToRedeem: Int = 0,
    val promoCode: String? = null,
    val cabin: String? = null,
    val addOns: String? = null

)