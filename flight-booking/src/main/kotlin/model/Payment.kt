package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Payment(
    val paymentId: Int,
    val amount: Double,
    val status: String
)

object Payments : Table() {
    val paymentId = integer("paymentId").autoIncrement()
    val amount = double("amount")
    val status = varchar("status", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(paymentId)
}