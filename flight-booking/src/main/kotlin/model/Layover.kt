package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Layover(
    val flightId: String,
    val stopNumber: Int,
    val airport: String,
    val durationMinutes: Int
)

object Layovers : Table() {
    val flightId = reference("flight_id", Flights.flightId)
    val stopNumber = integer("stop_number")
    val airport = reference("airport", Airports.code)
    val duration = integer("duration")

    override val primaryKey = PrimaryKey(flightId, stopNumber)
}