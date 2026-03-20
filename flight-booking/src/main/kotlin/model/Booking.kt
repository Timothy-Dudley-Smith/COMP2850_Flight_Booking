package com.flightsystem.model

import com.flightsystem.model.Airports.code
import com.flightsystem.model.Users.userId
import org.jetbrains.exposed.sql.Table

data class Booking(
    val bookingId: Int,
    val userId: Int,
    val flightId: String,
)

object Bookings: Table() {
    val bookingId = integer("bookingId").autoIncrement()
    val user = reference("userId", Users.userId)
    val flightId = reference("flight", Flights.flightId)

    override val primaryKey = PrimaryKey(bookingId)

    init {
        uniqueIndex(bookingId, flightId)  //Asked codex to check my tables, it said to add this so BookingSeat can have a unique composite key
    }
}


