package com.flightsystem.model

// import com.flightsystem.model.Bookings.bookingId
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class Seat(
    val seatId: Int,
    val flightId: String,
    val seatNumber: String,
    val isAvailable: Boolean = true,
    val seatClass: SeatClass,
)

object Seats : Table() {
    val seatId = integer("seatId").autoIncrement()
    val flightId = reference("flightId", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)
    val isAvailable = bool("isAvailable")
    val seatClass = enumeration<SeatClass>("seatClass")

    override val primaryKey = PrimaryKey(flightId, seatNumber)
}
