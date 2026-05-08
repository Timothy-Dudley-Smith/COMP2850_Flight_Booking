package com.flightsystem.flightservice

import com.flightsystem.model.Booking
import com.flightsystem.model.BookingDetails
import com.flightsystem.model.BookingSeats
import com.flightsystem.model.Bookings
import com.flightsystem.model.Flight
import com.flightsystem.model.Flights
import com.flightsystem.model.Seat
import com.flightsystem.model.Seats
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.LocalTime

class BookingService {
    fun calculateTotalPrice(
        flight: Flight,
        seats: List<Seat>,
    ): Double = flight.price * seats.size

    // create booking, links seats through BookingSeats and marks seats unavailable
    fun createBooking(
        userId: Int,
        flightId: String,
        seatNumbers: List<String>,
    ): Booking =
        transaction {
            require(seatNumbers.isNotEmpty()) {
                "At least one must be selected"
            }
            val seatsFromDb =
                Seats
                    .selectAll()
                    .where {
                        (Seats.flightId eq flightId) and
                            (Seats.seatNumber inList seatNumbers)
                    }.toList()

            // check seats exist
            if (seatsFromDb.size != seatNumbers.size) {
                throw IllegalArgumentException("one or more selected seats don't exist")
            }

            // now check if they are available
            val unavailableSeat = seatsFromDb.find { !it[Seats.isAvailable] }
            if (unavailableSeat != null) {
                throw IllegalArgumentException("one or more seats are not available")
            }
            val bookingDate = LocalDate.now().toString()
            val bookingTime = LocalTime.now().toString()
            // insert new row in bookings table
            val inserted =
                Bookings.insert {
                    it[Bookings.userId] = userId
                    it[Bookings.flightId] = flightId
                    it[Bookings.date] = bookingDate
                    it[Bookings.time] = bookingTime
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
                date = bookingDate,
                time = bookingTime,
                cabin = null,
                addOns = null,
                totalPrice = 10.0,
            )
        }

    fun cancelBooking(bookingId: Int): Boolean {
        return transaction {
            // load linked seat rows from BookingSeats
            val bookedSeats =
                BookingSeats
                    .selectAll()
                    .where {
                        BookingSeats.bookingId eq bookingId
                    }.toList()

            // if no linked rows exist - false
            if (bookedSeats.isEmpty()) {
                return@transaction false
            }

            // mark those seats avail again
            val seatsByFlight = bookedSeats.groupBy { it[BookingSeats.flightId] }
            for ((fId, fSeats) in seatsByFlight) {
                val nums = fSeats.map { it[BookingSeats.seatNumber] }
                Seats.update({ (Seats.flightId eq fId) and (Seats.seatNumber inList nums) }) {
                    it[isAvailable] = true
                }
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
            val bookingRow =
                Bookings
                    .selectAll()
                    .where {
                        Bookings.bookingId eq bookingId
                    }.singleOrNull()
            // if not exist then null
            if (bookingRow == null) {
                return@transaction null
            }
            // convert DB row to booking object
            val booking =
                Booking(
                    bookingId = bookingRow[Bookings.bookingId],
                    userId = bookingRow[Bookings.userId],
                    flightId = bookingRow[Bookings.flightId],
                    totalPrice = 10.0,
                    date = bookingRow[Bookings.date],
                    time = bookingRow[Bookings.time],
                    cabin = bookingRow[Bookings.cabin],
                    addOns = bookingRow[Bookings.addOns],
                )
            // load linked seats
            val bookedSeats =
                BookingSeats
                    .selectAll()
                    .where {
                        BookingSeats.bookingId eq bookingId
                    }.toList()

            val seatNumbers = bookedSeats.map { it[BookingSeats.seatNumber] }

            // return BookingDetails
            BookingDetails(
                booking = booking,
                seats = seatNumbers,
            )
        }
    }

    // return a list of all seats that are still available
    fun getAvailableSeats(flightId: String): List<Seat> =
        transaction {
            Seats
                .selectAll()
                .where {
                    (Seats.flightId eq flightId) and (Seats.isAvailable eq true)
                }.map {
                    Seat(
                        seatId = it[Seats.seatId],
                        flightId = it[Seats.flightId],
                        seatNumber = it[Seats.seatNumber],
                        isAvailable = it[Seats.isAvailable],
                        seatClass = it[Seats.seatClass],
                    )
                }
        }

    // return all bookings made by a specific user
    fun getBookingsByUser(userId: Int): List<Booking> =
        transaction {
            // query the bookings table for rows matching that user
            val bookingRows =
                Bookings
                    .selectAll()
                    .where {
                        Bookings.userId eq userId
                    }.toList()
            // map db rows into booking objects
            bookingRows.map { row ->
                Booking(
                    bookingId = row[Bookings.bookingId],
                    userId = row[Bookings.userId],
                    flightId = row[Bookings.flightId],
                    totalPrice = 10.0,
                    date = row[Bookings.date],
                    time = row[Bookings.time],
                    cabin = row[Bookings.cabin],
                    addOns = row[Bookings.addOns],
                )
            }
        }

    // temporarily hold seat so it can't be booked by others
    fun holdSeat(
        flightId: String,
        seatNumber: String,
    ): Boolean {
        return transaction {
            val seatRow =
                Seats
                    .selectAll()
                    .where {
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
    fun releaseSeat(
        flightId: String,
        seatNumber: String,
    ) {
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
        newSeatNumbers: List<String>,
    ): Boolean {
        return transaction {
            // check for empty newSeatNumbers
            if (newSeatNumbers.isEmpty()) {
                return@transaction false
            }
            // get current seat links
            val currentSeats =
                BookingSeats
                    .selectAll()
                    .where {
                        BookingSeats.bookingId eq bookingId
                    }.toList()
            if (currentSeats.isEmpty()) {
                return@transaction false
            }
            // get current data
            val flightId = currentSeats.first()[BookingSeats.flightId]
            val oldSeatNumbers = currentSeats.map { it[BookingSeats.seatNumber] }
            // load new seats from db
            val newSeatsFromDb =
                Seats
                    .selectAll()
                    .where {
                        (Seats.flightId eq flightId) and (Seats.seatNumber inList newSeatNumbers)
                    }.toList()
            // validate new seats exist
            if (newSeatsFromDb.size != newSeatNumbers.size) {
                return@transaction false
            }
            // validate availability
            val unavailableNewSeat =
                newSeatsFromDb.find {
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
            // delete old links
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

    fun getBookingDetailsByUser(userId: Int): List<Map<String, String>> {
        val result =
            transaction {
                // grab all bookings for this user
                val bookingRows =
                    Bookings
                        .selectAll()
                        .where { Bookings.userId eq userId }

                val resultList = mutableListOf<Map<String, String>>()

                // loop thru each booking and get the flight info seperately
                for (bookingRow in bookingRows) {
                    val flightId = bookingRow[Bookings.flightId]

                    // find the flight
                    val flightRow =
                        Flights
                            .selectAll()
                            .where { Flights.flightId eq flightId }
                            .single()

                    // map and add 2 list
                    resultList.add(
                        mapOf(
                            "bookingId" to bookingRow[Bookings.bookingId].toString(),
                            "flightId" to flightId,
                            "departureAirport" to flightRow[Flights.departureAirport],
                            "arrivalAirport" to flightRow[Flights.arrivalAirport],
                            "date" to flightRow[Flights.date],
                            "departureTime" to flightRow[Flights.departureTime],
                            "arrivalTime" to flightRow[Flights.arrivalTime],
                            "returnFlightId" to (bookingRow[Bookings.returnFlightId] ?: ""),
                        ),
                    )
                }

                resultList
            }

        return result
    }

    //  function gets bookings from the database and returns list
    fun getAllBookings(): List<BookingDetails> =
        transaction {
            // get every booking for the bookings table
            Bookings
                .selectAll()
                // if not null turn it into a BookingDetails object.
                .mapNotNull { row ->
                    // get booking id
                    val bookingId = row[Bookings.bookingId]
                    //  find all rows where the bookingId column matches
                    val seats =
                        BookingSeats
                            .selectAll()
                            .where { BookingSeats.bookingId eq bookingId }
                            //  only want the seat number itself
                            .map { seatRow -> seatRow[BookingSeats.seatNumber] }
                    // combine all info into object
                    val booking =
                        Booking(
                            bookingId = bookingId,
                            userId = row[Bookings.userId],
                            flightId = row[Bookings.flightId],
                            totalPrice = 10.0,
                            date = row[Bookings.date],
                            time = row[Bookings.time],
                            cabin = row[Bookings.cabin],
                            addOns = row[Bookings.addOns],
                        )

                    BookingDetails(
                        booking = booking,
                        seats = seats,
                    )
                }
        }
}
