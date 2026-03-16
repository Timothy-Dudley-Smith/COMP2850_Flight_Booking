 package com.flightsystem.model

data class Seat(
    val seatNumber: String,
    val seatClass: SeatClass,
    var isAvailable: Boolean = true
)