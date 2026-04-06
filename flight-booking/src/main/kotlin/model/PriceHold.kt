package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

data class PriceHold(
    val holdId: Int,
    val userId: Int,
    val flightId: String,
    val expiryTime: String,
    val totalPrice: Double
)

// join model links a hold to a selected seat
data class PriceHoldSeat(
    val holdId: Int,
    val flightId: String,
    val seatNumber: String
)

// main DB table for temp price holds
// store main hold info: who created it, flight?, expiry, price
object PriceHolds : Table() {
    val holdId = integer("holdId").autoIncrement()
    val userId = reference("userId", Users.userId)
    val flightId = reference("flightId", Flights.flightId)
    val expiryTime = varchar("expiryTime", VARCHAR_LENGTH)
    val totalPrice = double("totalPrice")
    override val primaryKey = PrimaryKey(holdId)
    init {
        uniqueIndex(holdId, flightId) // composite pair lets the join table reference hold and flight together 
    }
}

// join table stores each seat linked to price hold
object PriceHoldSeats : Table() {
    val holdId = reference("holdId", PriceHolds.holdId)
    val flightId = reference("flightId", Flights.flightId)
    val seatNumber = varchar("seatNumber", VARCHAR_LENGTH)
    override val primaryKey = PrimaryKey(flightId, holdId, seatNumber) // prevent duplicate seats for the same hold
    init {
        foreignKey(flightId, seatNumber, target = Seats.primaryKey)
        foreignKey(holdId to PriceHolds.holdId, flightId to PriceHolds.flightId)
    }
}