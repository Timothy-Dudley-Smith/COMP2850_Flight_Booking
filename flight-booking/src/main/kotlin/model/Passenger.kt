package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

// represents 1 passenger linked to a booking
data class Passenger(
    val passengerId: Int,
    val bookingId: Int,
    val name: String,
    val email: String,
    val passportNumber: String
)

// input model used when creating passengers before passengerId exists
data class PassengerInput(
    val name: String,
    val email: String,
    val passportNumber: String
)

// db for storing passemnger records 
object Passengers : Table() {
    // primary key
    val passengerId = integer("passengerId").autoIncrement()
    // link each passenger to a booking
    val bookingId = reference("bookingId", Bookings.bookingId)

    val name = varchar("name", VARCHAR_LENGTH)
    val email = varchar("email", VARCHAR_LENGTH)
    val passportNumber = varchar("passportNumber", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(passengerId)

    // index to speed up queries 
    init {
        index(false, bookingId)
    }
}

