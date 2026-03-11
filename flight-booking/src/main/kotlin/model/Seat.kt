package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Seat(
    val flightId: String,
    val seatNumber: String,
    var isAvailable: Boolean = true
)

object Seats : Table() {
    val flightId = reference("flight_id", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)
    val isAvailable = bool("isAvailable")

    override val primaryKey = PrimaryKey(flightId, seatNumber)
}