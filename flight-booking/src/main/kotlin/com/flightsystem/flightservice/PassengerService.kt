package com.flightsystem.flightservice

import com.flightsystem.model.Bookings
import com.flightsystem.model.Passenger
import com.flightsystem.model.PassengerInput
import com.flightsystem.model.Passengers
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class PassengerService {
    // add 1 or more passengers to an existing booking
    fun addPassengersToBooking(
        bookingId: Int,
        passengers: List<PassengerInput>,
    ): List<Passenger> =
        transaction {
            require(passengers.isNotEmpty()) {
                "At least 1 passenger must be provided"
            }
            // only add passengers if the booking exists
            val bookingRow =
                Bookings
                    .selectAll()
                    .where {
                        Bookings.bookingId eq bookingId
                    }.singleOrNull()
            if (bookingRow == null) {
                throw IllegalArgumentException("Booking does not exist")
            }

            // insert each passenger row and return the created passenger objects
            val createdPassengers =
                passengers.map { passengerInput ->
                    val inserted =
                        Passengers.insert {
                            it[Passengers.bookingId] = bookingId
                            it[Passengers.title] = passengerInput.title
                            it[Passengers.firstName] = passengerInput.firstName
                            it[Passengers.lastName] = passengerInput.lastName
                            it[Passengers.gender] = passengerInput.gender
                            it[Passengers.dateOfBirth] = passengerInput.dateOfBirth
                            it[Passengers.passportNumber] = passengerInput.passportNumber
                        }

                    val newPassengerId = inserted[Passengers.passengerId]

                    Passenger(
                        passengerId = newPassengerId,
                        bookingId = bookingId,
                        title = passengerInput.title,
                        firstName = passengerInput.firstName,
                        lastName = passengerInput.lastName,
                        gender = passengerInput.gender,
                        dateOfBirth = passengerInput.dateOfBirth,
                        passportNumber = passengerInput.passportNumber,
                    )
                }
            createdPassengers
        }

    // return all passengers linked to a specific booking
    fun getPassengersByBooking(bookingId: Int): List<Passenger> =
        transaction {
            Passengers
                .selectAll()
                .where {
                    Passengers.bookingId eq bookingId
                }.map { row ->
                    Passenger(
                        passengerId = row[Passengers.passengerId],
                        bookingId = row[Passengers.bookingId],
                        title = row[Passengers.title],
                        firstName = row[Passengers.firstName],
                        lastName = row[Passengers.lastName],
                        gender = row[Passengers.gender],
                        dateOfBirth = row[Passengers.dateOfBirth],
                        passportNumber = row[Passengers.passportNumber],
                    )
                }
        }

    fun updatePassenger(
        passengerId: Int,
        input: PassengerInput,
    ) {
        transaction {
            // find passenger by id and update their info
            Passengers.update({ Passengers.passengerId eq passengerId }) {
                it[title] = input.title
                it[firstName] = input.firstName
                it[lastName] = input.lastName
                it[gender] = input.gender
                it[dateOfBirth] = input.dateOfBirth
                it[passportNumber] = input.passportNumber
            }
        }
    }

    fun deletePassengersByBooking(bookingId: Int) {
        transaction {
            Passengers.deleteWhere { Passengers.bookingId eq bookingId }
        }
    }
}
