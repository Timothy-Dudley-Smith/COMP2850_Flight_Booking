package com.flightsystem.model

import com.flightsystem.model.Airports.code
import com.flightsystem.model.Flights.arrivalAirport
import com.flightsystem.model.Flights.departureAirport
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.awt.print.Book
import kotlinx.serialization.Serializable

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
        foreignKey(bookingId to Bookings.bookingId, flightId to Bookings.flightId)
    }
}