package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table
import kotlinx.serialization.Serializable

@Serializable
data class Seat(
    val flightId: String,
    val seatNumber: String,
    val isAvailable: Boolean = true
)

object Seats : Table() {
    val flightId = reference("flightId", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)
    val isAvailable = bool("isAvailable")

    override val primaryKey = PrimaryKey(flightId, seatNumber)
}