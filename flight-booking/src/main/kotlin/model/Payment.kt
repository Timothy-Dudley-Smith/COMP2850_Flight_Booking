package com.flightsystem.model

import java.time.LocalDateTime

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
    val lastFourDigits: String,
    val cardHolderName: String,
    var status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    var refundedAt: LocalDateTime? = null
) {



    fun setStatusSuccess() {
        status = PaymentStatus.SUCCESS
    }

    fun setStatusFailed() {
        status = PaymentStatus.FAILED
    }

    fun setRefunded() {
        status = PaymentStatus.REFUNDED
        refundedAt = LocalDateTime.now()
    }

    fun isRefundable(): Boolean = status == PaymentStatus.SUCCESS

    fun isPending(): Boolean = status == PaymentStatus.PENDING

    fun isSuccessful(): Boolean = status == PaymentStatus.SUCCESS

    fun getSummary(): String {
        return """
            --- Payment Summary ---
            Payment ID:   $paymentID
            Booking ID:   $bookingID
            Amount:       £${"%.2f".format(amount)}
            Card:         **** **** **** $lastFourDigits
            Cardholder:   $cardHolderName
            Status:       $status
            Date:         $timestamp
            ${if (refundedAt != null) "Refunded at: $refundedAt" else ""}
        """.trimIndent()
            
    }



}

object Payments : Table() {
    val paymentID = varchar("paymentID", 50)
    val bookingID = varchar("bookingID", 50)
    val userID = varchar("userID", 50)
    val amount = double("amount")
    val lastFourDigits = varchar("lastFourDigits", 4)
    val cardHolderName = varchar("cardHolderName", 100)
    val status = varchar("status", 20)
    val timestamp = varchar("timestamp", 50)
    val refundedAt = varchar("refundedAt", 50).nullable()

    override val primaryKey = PrimaryKey(paymentID)
}