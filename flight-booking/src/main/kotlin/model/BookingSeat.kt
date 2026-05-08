package com.flightsystem.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class BookingSeat(
    val bookingId: Int,
    val flightId: String,
    val seatNumber: String,
)

object BookingSeats : Table() {
    val bookingId = reference("bookingId", Bookings.bookingId)
    val flightId = reference("flightId", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(flightId, bookingId, seatNumber)

    init {
        foreignKey(flightId, seatNumber, target = Seats.primaryKey)
    }
}
