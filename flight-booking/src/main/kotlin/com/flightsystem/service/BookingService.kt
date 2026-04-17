package com.flightsystem.service 

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import com.flightsystem.model.*

class BookingService {

    fun calculateTotalPrice(flight: Flight, seats: List<Seat>): Double {
        return flight.price * seats.size
    }


    // create booking, links seats through BookingSeats and marks seats unavailable
    fun createBooking(
        userId: Int,
        flightId: String,
        seatNumbers: List<String>
    ): Booking {
        return transaction  {
            require(seatNumbers.isNotEmpty()) {
                "At least one must be selected"
            }
            val seatsFromDb = Seats.selectAll().where {
                (Seats.flightId eq flightId) and
                (Seats.seatNumber inList seatNumbers)
            }.toList()
            
            // check seats exist
            if (seatsFromDb.size != seatNumbers.size) {
                throw IllegalArgumentException("one or more selected seats don't exist")
            }

            // now check if they are available
            val unavailableSeat = seatsFromDb.find { !it[Seats.isAvailable]}
            if (unavailableSeat != null) {
                throw IllegalArgumentException("one or more seats are not available")
            }
            
            // insert new row in bookings table
            val inserted = Bookings.insert {
                it[Bookings.userId] = userId
                it[Bookings.flightId] = flightId
            }

            val newBookingId = inserted[Bookings.bookingId]

            // link booking to each selected seat
            for (seatNumber in seatNumbers) {
                BookingSeats.insert {
                    it[BookingSeats.bookingId] = newBookingId
                    it[BookingSeats.flightId] = flightId
                    it[BookingSeats.seatNumber] = seatNumber
                }
            }

            Seats.update({
                (Seats.flightId eq flightId) and
                (Seats.seatNumber inList seatNumbers)
            }) {
                it[isAvailable] = false
            }

            Booking(
                bookingId = newBookingId,
                userId = userId,
                flightId = flightId,
                totalPrice = 10.0
            )
        }
    }

    fun cancelBooking(bookingId: Int): Boolean {
        return transaction {
            // load linked seat rows from BookingSeats
            val bookedSeats = BookingSeats.selectAll().where {
                BookingSeats.bookingId eq bookingId
            }.toList()

            // if no linked rows exist - false
            if (bookedSeats.isEmpty()) {
                return@transaction false
            }

            // mark those seats avail again
            val flightId = bookedSeats.first()[BookingSeats.flightId]
            val seatNumbers = bookedSeats.map { it[BookingSeats.seatNumber] }
            // then update seats
            Seats.update({
                (Seats.flightId eq flightId) and (Seats.seatNumber inList seatNumbers)
            }) {
                it[isAvailable] = true
            }

            // delete from BookingSeats
            BookingSeats.deleteWhere {
                BookingSeats.bookingId eq bookingId
            }
            // delete from Bookings
            Bookings.deleteWhere {
                Bookings.bookingId eq bookingId
            }
            
            return@transaction true
        }
    }

    // get booking details 
    fun getBookingDetails(bookingId: Int): BookingDetails? {
        return transaction {
            // load booking row
            val bookingRow = Bookings.selectAll().where {
                Bookings.bookingId eq bookingId
            }.singleOrNull()
            // if not exist then null
            if (bookingRow == null) {
                return@transaction null
            }
            //convert DB row to booking object
            val booking = Booking(
                bookingId = bookingRow[Bookings.bookingId],
                userId = bookingRow[Bookings.userId],
                flightId = bookingRow[Bookings.flightId],
                totalPrice = 10.0
            )
            // load linked seats
            val bookedSeats = BookingSeats.selectAll().where {
                BookingSeats.bookingId eq bookingId
            }.toList()

            val seatNumbers = bookedSeats.map { it[BookingSeats.seatNumber] }

            // return BookingDetails
            BookingDetails(
                booking = booking,
                seats = seatNumbers
            )
        }
    }

    // return a list of all seats that are still available 
    fun getAvailableSeats(flightId: String): List<Seat> {
        return transaction {
            Seats.selectAll().where {
                (Seats.flightId eq flightId) and (Seats.isAvailable eq true)
            }.map {
                Seat(
                    seatId = it[Seats.seatId],
                    flightId = it[Seats.flightId],
                    seatNumber = it[Seats.seatNumber],
                    isAvailable = it[Seats.isAvailable],
                    seatClass = it[Seats.seatClass]
                )
            }           
        }
    }


    // return all bookings made by a specific user
    fun getBookingsByUser(userId: Int): List<Booking> {
        return transaction {
            // query the bookings table for rows matching that user
            val bookingRows = Bookings.selectAll().where {
                Bookings.userId eq userId
            }.toList()
            // map db rows into booking objects
            bookingRows.map { row ->
                Booking(
                    bookingId = row[Bookings.bookingId],
                    userId = row[Bookings.userId],
                    flightId = row[Bookings.flightId],
                    totalPrice = 10.0
                )
            }
        }
    }

    // temporarily hold seat so it can't be booked by others
    fun holdSeat(flightId: String, seatNumber: String): Boolean {
        return transaction {
            val seatRow = Seats.selectAll().where {
                (Seats.flightId eq flightId) and (Seats.seatNumber eq seatNumber)
            }.singleOrNull()
            if (seatRow == null) {
                return@transaction false
            }
            if (!seatRow[Seats.isAvailable]) {
                return@transaction false
            }
            Seats.update({
                (Seats.flightId eq flightId) and (Seats.seatNumber eq seatNumber)
            }) {
                it[isAvailable] = false
            }
            return@transaction true
        }  
    }

    // release a held seat 
    fun releaseSeat(flightId: String, seatNumber: String) {
        transaction {
            Seats.update({
                (Seats.flightId eq flightId) and (Seats.seatNumber eq seatNumber)
            }) {
                it[isAvailable] = true
            }
        }
    }


    // update booking
    fun updateBookingSeats(
        bookingId: Int,
        newSeatNumbers: List<String>
    ): Boolean {
        return transaction {
            // check for empty newSeatNumbers
            if (newSeatNumbers.isEmpty()) {
                return@transaction false
            }
            // get current seat links
            val currentSeats = BookingSeats.selectAll().where {
                BookingSeats.bookingId eq bookingId
            }.toList()
            if (currentSeats.isEmpty()) {
                return@transaction false
            }
            // get current data
            val flightId = currentSeats.first()[BookingSeats.flightId]
            val oldSeatNumbers = currentSeats.map { it[BookingSeats.seatNumber]}
            // load new seats from db
            val newSeatsFromDb = Seats.selectAll().where {
                (Seats.flightId eq flightId) and (Seats.seatNumber inList newSeatNumbers)
            }.toList()
            // validate new seats exist
            if (newSeatsFromDb.size != newSeatNumbers.size) {
                return@transaction false
            }
            // validate availability 
            val unavailableNewSeat = newSeatsFromDb.find {
                !it[Seats.isAvailable] && it[Seats.seatNumber] !in oldSeatNumbers
            }
            if (unavailableNewSeat != null) {
                return@transaction false
            }
            // release old seats
            Seats.update({
                (Seats.flightId eq flightId) and (Seats.seatNumber inList oldSeatNumbers)
            }) {
                it[isAvailable] = true
            }
            //delete old links
            BookingSeats.deleteWhere {
                BookingSeats.bookingId eq bookingId
            }
            // insert new links
            for (seatNumber in newSeatNumbers) {
                BookingSeats.insert {
                    it[BookingSeats.bookingId] = bookingId
                    it[BookingSeats.flightId] = flightId
                    it[BookingSeats.seatNumber] = seatNumber
                }
            }
            // mark new seats unavailable
            Seats.update({
                (Seats.flightId eq flightId) and (Seats.seatNumber inList newSeatNumbers)
            }) {
                it[isAvailable] = false
            }
            return@transaction true
        }
    }

    // calc total price based on seat class
    /* temp comment this out - trying something 
    fun calculatePriceByClass(flight: Flight, seats: List<Seat>): Double {
        var totalPrice = 0.0

        for (seat in seats) {
            val seatPrice = when (seat.seatClass) {
                SeatClass.ECONOMY -> flight.price
                SeatClass.BUSINESS -> flight.price * 1.5
                SeatClass.FIRST -> flight.price * 2
            }
            totalPrice += seatPrice
        }
        return totalPrice
    }
    */

    // update the seats assigned to an existing booking
    /* temp 
    fun updateBookingSeats(
        booking: Booking,
        newSeats: List<Seat>
    ): Boolean {
        //check if new seats are available
        if (!validateSeats(newSeats)) {
            return false
        }

        // release old seats
        for (seat in booking.seatsBooked) {
            seat.isAvailable = true
        }

        // reserve new seats
        for (seat in newSeats) {
            seat.isAvailable = false
        }
        //update the booking with the new seats
        booking.seatsBooked.clear()
        booking.seatsBooked.addAll(newSeats)
        return true
    }
    */
}