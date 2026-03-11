package com.flightsystem.model

data class Seat(
    val seatNumber: String,
    var isAvailable: Boolean = true
)   