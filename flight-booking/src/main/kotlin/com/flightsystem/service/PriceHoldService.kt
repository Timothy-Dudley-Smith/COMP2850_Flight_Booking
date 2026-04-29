package com.flightsystem.service

import com.flightsystem.model.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.deleteWhere
import java.time.LocalDate
import java.time.LocalTime


class PriceHoldService {
    private fun getSeatMultiplier(seatClass: SeatClass): Double {
        return when (seatClass) {
            SeatClass.ECONOMY -> 1.0
            SeatClass.PREMIUM_ECONOMY -> 1.6
            SeatClass.BUSINESS -> 3.5
        }
    }
    
    
    fun createHold(
        userId: Int,
        flightId: String,
        seatNumbers: List<String>,
        returnFlightId: String? = null,
        returnSeatNumbers: List<String> = emptyList()
    ): PriceHold {

        return transaction {
            require(seatNumbers.isNotEmpty()) {
                "At least 1 seat must be selected" 
            }
            val seatsFromDb = Seats.selectAll().where {
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
            val flightRow = Flights.selectAll().where {
                Flights.flightId eq flightId 
            }.singleOrNull()
            if (flightRow == null) {
                throw IllegalArgumentException("Flight doesn't exist") 
            }
            val basePrice = flightRow[Flights.price]
            var totalPrice = 0.0
            for (seatRow in seatsFromDb) {
                val seatClass = seatRow[Seats.seatClass]
                val multiplier = getSeatMultiplier(seatClass)
                totalPrice += basePrice * multiplier
            }

            if (!returnFlightId.isNullOrBlank() && returnSeatNumbers.isNotEmpty()) {
                val returnSeatsFromDb = Seats.selectAll().where {
                    (Seats.flightId eq returnFlightId) and
                    (Seats.seatNumber inList returnSeatNumbers)
                }.toList()

                val numberOfSeatsFound = returnSeatsFromDb.size
                val numberOfSeatsRequested = returnSeatNumbers.size
                if (numberOfSeatsFound != numberOfSeatsRequested) {
                    throw IllegalArgumentException ("One or more of the seats requeted are not available")

                }

                for (seat in returnSeatsFromDb) {
                    val isAvailabe = seat[Seats.isAvailable]
                    if (!isAvailabe){
                        throw IllegalArgumentException ("One or more of the seats requeted are not available")
                    }
                }

                val returnFlightRow = Flights.selectAll().where{
                    Flights.flightId eq returnFlightId
                }.singleOrNull()

                if (returnFlightRow == null) {
                    throw IllegalArgumentException ("return flight doesnt exist")
                }

                val returnBasePrice = returnFlightRow[Flights.price]

                for (seat in returnSeatsFromDb){
                    val seatClass = seat[Seats.seatClass]
                    val multiplier = getSeatMultiplier(seatClass)
                    val seatPrice = returnBasePrice * multiplier
                    totalPrice = totalPrice + seatPrice
                }

            }
            val expiryTime = LocalDateTime.now().plusMinutes(15).toString() // set hold to expire in 15 mins
            val inserted = PriceHolds.insert {
                it[PriceHolds.userId] = userId
                it[PriceHolds.flightId] = flightId
                it[PriceHolds.expiryTime] = expiryTime
                it[PriceHolds.totalPrice] = totalPrice
            }
            val newHoldId = inserted[PriceHolds.holdId]
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
    fun confirmHoldToBooking(holdId: Int, cabin: String? = null, addOns: String? = null): Booking? {

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

            val flightRow = Flights.selectAll().where {
                Flights.flightId eq flightId
            }.singleOrNull() ?: return@transaction null

            // create the main booking row using the user and flight from the hold
            val inserted = Bookings.insert {
                it[Bookings.userId] = userId
                it[Bookings.flightId] = flightId
                it[Bookings.date] = flightRow[Flights.date]
                it[Bookings.time] = flightRow[Flights.departureTime]
                it[Bookings.cabin] = cabin
                it[Bookings.addOns] = addOns
            }

            val newBookingId = inserted[Bookings.bookingId]

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
            val bookingDate = flightRow[Flights.date]
            val bookingTime = flightRow[Flights.departureTime]
            Booking(
                bookingId = newBookingId,
                userId = userId,
                flightId = flightId,
                totalPrice = holdRow[PriceHolds.totalPrice],
                date = bookingDate,
                time = bookingTime,
            )
        }
    }
}