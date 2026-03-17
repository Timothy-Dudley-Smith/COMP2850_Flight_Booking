package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Flight(
    val flightId: String,
    val date: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val departureTime: String,
    val arrivalTime: String,
    val seats: List<Seat>,
    val layovers: List<Layover> = emptyList()
)

object Flights : Table() {
    val flightId = varchar("flightId", VARCHAR_LENGTH)
    val date = varchar("date", VARCHAR_LENGTH)
    val departureAirport = reference("departureAirport", Airports.code)
    val arrivalAirport = reference("arrivalAirport", Airports.code)
    val departureTime = varchar("departureTime", VARCHAR_LENGTH)
    val arrivalTime = varchar("arrivalTime", VARCHAR_LENGTH)
    val price = double("price")

    override val primaryKey = PrimaryKey(flightId)
}