package com.flightsystem.model

import com.flightsystem.model.Airports.code
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

data class BookingSeat(
    val bookingId: Int,
    val flightId: String,
    val seatNumber: String,
)

object BookingSeats : Table() {
    val bookingId = reference("bookingId", Bookings.bookingId)
    val flightId = reference("flightId", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(flightId, seatNumber)
}