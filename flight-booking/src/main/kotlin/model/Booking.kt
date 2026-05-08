package com.flightsystem.model

import com.flightsystem.model.Users.userId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Booking(
    val bookingId: Int,
    val userId: Int,
    val flightId: String,
    val returnFlightId: String? = null,
    val totalPrice: Double,
    val date: String,
    val time: String,
    val cabin: String? = null,
    val addOns: String? = null,
)

@Serializable
data class BookingDetails(
    val booking: Booking,
    val seats: List<String>,
)

object Bookings : Table() {
    val bookingId = integer("bookingId").autoIncrement()
    val userId = reference("userId", Users.userId)
    val flightId = reference("flight", Flights.flightId)
    val date = varchar("date", VARCHAR_LENGTH)
    val time = varchar("time", VARCHAR_LENGTH)
    val cabin = varchar("cabin", VARCHAR_LENGTH).nullable()
    val addOns = varchar("addOns", 1000).nullable()
    val returnFlightId = varchar("returnFlightId", 128).nullable()

    override val primaryKey = PrimaryKey(bookingId)

    init {
        uniqueIndex(bookingId, flightId)
        // Asked codex to check my tables, it said to add this so BookingSeat can have a unique composite key
    }
}
