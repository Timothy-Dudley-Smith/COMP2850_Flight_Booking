package com.flightsystem.model
import kotlinx.serialization.Serializable

@Serializable
enum class SeatClass {
    ECONOMY,
    PREMIUM_ECONOMY,
    BUSINESS
}