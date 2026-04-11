package com.flightsystem.service

import com.flightsystem.model.*
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class PassengerService {
    // add 1 or more passengers to an existing booking 
    fun addPassengersToBooking(
        bookingId: Int,
        passengers: List<PassengerInput>
    ): List<Passenger> {
        return transaction {
            require(passengers.isNotEmpty()) {
                "At least 1 passenger must be provided"
            }
            // only add passengers if the booking exists 
            val bookingRow = Bookings.selectAll().where {
                Bookings.bookingId eq bookingId
            }.singleOrNull()
            if (bookingRow == null) {
                throw IllegalArgumentException("Booking does not exist")
            }

            // insert each passenger row and return the created passenger objects 
            val createdPassengers = passengers.map { passengerInput ->
                val newPassengerId = Passengers.insertAndGetId {
                    it[Passengers.bookingId] = bookingId
                    it[Passengers.name] = passengerInput.name
                    it[Passengers.email] = passengerInput.email
                    it[Passengers.passportNumber] = passengerInput.passportNumber
                }.value

                Passenger(
                    passengerId = newPassengerId,
                    bookingId = bookingId,
                    name = passengerInput.name,
                    email = passengerInput.email,
                    passportNumber = passengerInput.passportNumber
                )
            }
            createdPassengers
        }
    }

    // return all passengers linked to a specific booking 
    fun getPassengersByBooking(bookingId: Int): List<Passenger> {
        return transaction {
            Passengers.selectAll().where {
                Passengers.bookingId eq bookingId
            }.map { row ->
                Passenger(
                    passengerId = row[Passengers.passengerId],
                    bookingId = row[Passengers.bookingId],
                    name = row[Passengers.name],
                    email = row[Passengers.email],
                    passportNumber = row[Passengers.passportNumber]
                )
            }
        }
    }
}