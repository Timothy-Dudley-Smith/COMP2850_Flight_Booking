package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Seat(
    val flightId: String,
    val seatNumber: String,
    var isAvailable: Boolean = true,
    var seatClass: SeatClass
)

object Seats : Table() {
    val flightId = reference("flightId", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)
    val isAvailable = bool("isAvailable")
    val seatClass = varchar("seatClass", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(flightId, seatNumber)
}