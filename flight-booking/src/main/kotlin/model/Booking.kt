package com.flightsystem.model

import com.flightsystem.model.Airports.code
import org.jetbrains.exposed.sql.Table

data class Booking(
    val bookingId: Int,
    val userId: Int,
    val flightId: String,
    val paymentId: Int
)

object Bookings: Table() {
    val bookingId = integer("bookingId").autoIncrement()
    val user = reference("userId", Users.userId)
    val flightId = reference("flight", Flights.flightId)
    val paymentId = reference("paymentId", Payments.paymentId)

    override val primaryKey = PrimaryKey(bookingId)
}


