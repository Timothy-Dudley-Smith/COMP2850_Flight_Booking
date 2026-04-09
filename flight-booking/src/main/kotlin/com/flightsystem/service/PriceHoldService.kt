package com.flightsystem.service

import com.flightsystem.model.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert 
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.deleteWhere


class PriceHoldService {
    fun createHold(
        userId: Int,
        flightId: String,
        seatNumbers: List<String>
    ): PriceHold {
        return transaction {
            require(seatNumbers.isNotEmpty()) {
                "At least 1 seat must be selected" 
            }
            val seatsFromDb = Seats.select {
                (Seats.flightId eq flightId) and
                (Seats.seatNumber inList seatNumbers)
            }.toList()
            if (seatsFromDb.size != seatNumbers.size) {
                throw IllegalArgumentException("One or more selected seats don't exist") //if DB returns fewer seats than requested, some seat nums are invalid 
            }
            val unavailableSeat = seatsFromDb.find { !it[Seats.isAvailable] }
            if (unavailableSeat != null) {
                throw IllegalArgumentException("One or more selected seats aren't available") // all selected seats have to be available before creating the hold
            }
            // load flight data from DB so total price is calcd from stored flight price
            val flightRow = Flights.select {
                Flights.flightId eq flightId 
            }.singleOrNull()
            if (flightRow == null) {
                throw IllegalArgumentException("Flight doesn't exist") 
            }
            val basePrice = flightRow[Flights.price]
            val totalPrice = basePrice * seatNumbers.size
            val expiryTime = LocalDateTime.now().plusMinutes(15).toString() // set hold to expire in 15 mins 
            val newHoldId = PriceHolds.insertAndGetId {
                it[PriceHolds.userId] = userId
                it[PriceHolds.flightId] = flightId
                it[PriceHolds.expiryTime] = expiryTime
                it[PriceHolds.totalPrice] = totalPrice
            }.value
            for (seatNumber in seatNumbers) {
                PriceHoldSeats.insert {
                    it[PriceHoldSeats.holdId] = newHoldId
                    it[PriceHoldSeats.flightId] = flightId
                    it[PriceHoldSeats.seatNumber] = seatNumber
                }
            }
            // lock the held seats by marking them unavailable in the Seats table
            Seats.update({
                (Seats.flightId eq flightId) and
                (Seats.seatNumber inList seatNumbers)
            }) {
                it[isAvailable] = false 
            }
            PriceHold(
                holdId = newHoldId,
                userId = userId,
                flightId = flightId,
                expiryTime = expiryTime,
                totalPrice = totalPrice
            )
        }
    }

    // remove an exisitng hold and release its seats back to available
    fun expireHold(holdId: Int): Boolean {
        return transaction {
            // load all seat links for this hold from join table
            val heldSeats = PriceHoldSeats.selectAll().where {
                PriceHoldSeats.holdId eq holdId
            }.toList()

            // if no linked held seats exist, treat the hold as missing
            if (heldSeats.isEmpty()) {
                return@transaction false
            }

            // extract flightid and seat nums so the held seats can be released 
            val flightId = heldSeats.first()[PriceHoldSeats.flightId]
            val seatNumbers = heldSeats.map { it[PriceHoldSeats.seatNumber] }

            // releasse the held seats by marking them avail
            Seats.update({
                (Seats.flightId eq flightId) and
                (Seats.seatNumber inList seatNumbers)
            }) {
                it[isAvailable] = true 
            }

            // delete the seat links for this hold after releasing seats 
            PriceHoldSeats.deleteWhere {
                PriceHoldSeats.holdId eq holdId
            }

            // delete the main hold row now that hold has been expired 
            PriceHolds.deleteWhere {
                PriceHolds.holdId eq holdId
            }

            // ret true - hold was found and removed successfully
            return@transaction true
        }
    }

    // return hold info and linked seat numbers for 1 hold
    fun getHoldDetails(holdId: Int): PriceHoldDetails? {
        // load the main hold row from the database 
        return transaction {
            val holdRow = PriceHolds.selectAll().where {
                PriceHolds.holdId eq holdId
            }.singleOrNull()

            // if no hold row exists, ret null
            if (holdRow == null) {
                return@transaction null
            }

            //convert the db rows into a PriceHold object
            val hold = PriceHold(
                holdId = holdRow[PriceHolds.holdId],
                userId = holdRow[PriceHolds.userId],
                flightId = holdRow[PriceHolds.flightId],
                expiryTime = holdRow[PriceHolds.expiryTime],
                totalPrice = holdRow[PriceHolds.totalPrice]
            )

            // load all seat links for this hold from the join table
            val heldSeats = PriceHoldSeats.selectAll().where {
                PriceHoldSeats.holdId eq holdId
            }.toList()

            // extract just the seat numbers from the linked seat rows 
            val seatNumbers = heldSeats.map { it[PriceHoldSeats.seatNumber] }

            // return the hold together with its linked seat numbes 
            PriceHoldDetails(
                hold = hold,
                seats = seatNumbers
            )
        }
    }

    // convert a valid hold into a permanent booking 
    fun confirmHoldToBooking(holdId: Int): Booking? {
        return transaction {
            // load the main hold row so it can be converted into a booking
            val holdRow = PriceHolds.selectAll().where {
                PriceHolds.holdId eq holdId
            }.singleOrNull()

            if (holdRow == null) {
                return@transaction null
            }

            val userId = holdRow[PriceHolds.userId]
            val flightId = holdRow[PriceHolds.flightId]

            // load the held seat links so they can be copied into BookingSeats
            val heldSeats = PriceHoldSeats.selectAll().where {
                PriceHoldSeats.holdId eq holdId
            }.toList()

            if (heldSeats.isEmpty()) {
                return@transaction null
            }

            // create the main booking row using the user and flight from the hold
            val newBookingId = Bookings.insertAndGetId {
                it[Bookings.userId] = userId
                it[Bookings.flightId] = flightId
            }.value

            // copy each held seat into the booking seat join table
            for (heldSeat in heldSeats) {
                BookingSeats.insert {
                    it[BookingSeats.bookingId] = newBookingId
                    it[BookingSeats.flightId] = heldSeat[PriceHoldSeats.flightId]
                    it[BookingSeats.seatNumber] = heldSeat[PriceHoldSeats.seatNumber]
                }
            }

            // remove temp hold-seat links after copying tgem into the booking 
            PriceHoldSeats.deleteWhere {
                PriceHoldSeats.holdId eq holdId
            }
            
            // delete the temp hold row after conversion to booking
            PriceHolds.deleteWhere{
                PriceHolds.holdId eq holdId
            }

            // ret the new perm booking created from the hold
            Booking(
                bookingId = newBookingId,
                userId = userId,
                flightId = flightId
            )
        }
    }
}