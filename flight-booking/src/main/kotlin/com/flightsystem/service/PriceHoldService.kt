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
}