package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Layover(
    val flightId: String,
    val stopNumber: Int,
    val airport: Airport,
    val durationMinutes: Int
)

object Layovers : Table() {
    val flightId = reference("flight_id", Flights.flightId)
    val stopNumber = integer("stop_number")
    val airport = varchar("airport", VARCHAR_LENGTH)
    val duration = integer("duration")

    override val primaryKey = PrimaryKey(flightId, stopNumber)
}