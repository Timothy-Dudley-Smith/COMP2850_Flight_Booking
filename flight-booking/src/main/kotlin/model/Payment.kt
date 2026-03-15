package com.flightsystem.model

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table

data class Payment(
    val paymentId: Int,
    val bookingId: Int,
    val amount: Double,
    val status: Boolean
)

object Payments : Table() {
    val paymentId = integer("paymentId").autoIncrement()
    val bookingId = reference("bookingId", Bookings.bookingId)
    val amount = double("amount")
    val status = bool("status")

    override val primaryKey = PrimaryKey(paymentId)
}