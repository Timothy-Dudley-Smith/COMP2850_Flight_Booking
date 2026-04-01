package com.flightsystem.model

import java.time.LocalDateTime

import com.flightsystem.model.Bookings.bookingId
import org.jetbrains.exposed.sql.Table


enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}


data class Payment(
    val paymentID: String,
    val bookingID: String,
    val userID: String,
    val amount: Double,
    val lastfourdigits: String, number
    val cardHolderName: String,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val refundedAt: LocalDateTime? = null
) {



    fun setstatusSuccess() {
        status = PaymentStatus.SUCCESS
    }

    fun setStatusFailed() {
        status = PaymentStatus.FAILED
    }



}

object Payments : Table() {
    val paymentId = integer("paymentId").autoIncrement()
    val bookingId = reference("bookingId", Bookings.bookingId)
    val amount = double("amount")
    val status = bool("status")

    override val primaryKey = PrimaryKey(paymentId)
}