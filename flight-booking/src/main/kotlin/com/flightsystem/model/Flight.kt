package com.flightsystem.model

data class Flight(
    val flightNumber: String,
    val departureAirport: Airport,
    val arrivalAirport: Airport,
    val price: Double,
    val seats: MutableList<Seat>,
    val layovers: List<Layover> = emptyList()
)