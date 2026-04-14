package com.flightsystem.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table


// represents 1 passenger linked to a booking
@Serializable
data class Passenger(
    val passengerId: Int,
    val bookingId: Int,
    val title: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val dateOfBirth: String,
    val passportNumber: String
)

// input model used when creating passengers before passengerId exists
@Serializable
data class PassengerInput(
    val title: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val dateOfBirth: String,
    val passportNumber: String
)

// request model for saving multiple passengers for one booking
@Serializable
data class SavePassengersRequest(
    val bookingId: Int,
    val passengers: List<PassengerInput>
)

// db for storing passemnger records 
object Passengers : Table() {
    // primary key
    val passengerId = integer("passengerId").autoIncrement()
    // link each passenger to a booking
    val bookingId = reference("bookingId", Bookings.bookingId)

    val title = varchar("title", VARCHAR_LENGTH)
    val firstName = varchar("firstName", VARCHAR_LENGTH)
    val lastName = varchar("lastName", VARCHAR_LENGTH)
    val gender = varchar("gender", VARCHAR_LENGTH)
    val dateOfBirth = varchar("dateOfBirth", VARCHAR_LENGTH)
    val passportNumber = varchar("passportNumber", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(passengerId)

    // index to speed up queries 
    init {
        index(false, bookingId)
    }
}

