package com.flightsystem.model


data class Booking(
    val bookingId: String,
    val user: User,
    val flight: Flight,
    val seatsBooked: List<Seat>,
    val payment: Payment
)