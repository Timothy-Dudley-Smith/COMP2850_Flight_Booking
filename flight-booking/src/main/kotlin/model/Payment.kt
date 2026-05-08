package com.flightsystem.model

import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
}

/**
Represents a payment transaction within the system.

Stores payment details, status, timestamps, and provides helper
methods for updating and checking payment state.
 */

data class Payment(
    val paymentID: String,
    val bookingID: String,
    val userID: Int,
    val amount: Double,
    val lastFourDigits: String,
    val cardHolderName: String,
    var status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    var refundedAt: LocalDateTime? = null,
) {
    /**
     Marks the payment as successful.
     */

    fun setStatusSuccess() {
        status = PaymentStatus.SUCCESS
    }

    /**
     Marks the payment as failed.
     */

    fun setStatusFailed() {
        status = PaymentStatus.FAILED
    }

    /**
     Marks the payment as refunded and records the refund time.
     */

    fun setRefunded() {
        status = PaymentStatus.REFUNDED
        refundedAt = LocalDateTime.now()
    }

    /**
     Checks whether the payment is eligible for a refund.
     */

    fun isRefundable(): Boolean = status == PaymentStatus.SUCCESS

    /**
     Checks whether the payment is still pending.
     */

    fun isPending(): Boolean = status == PaymentStatus.PENDING

    /**
     Checks whether the payment was successful.
     */

    fun isSuccessful(): Boolean = status == PaymentStatus.SUCCESS

    /**
     Returns a formatted summary of the payment for display or logging.
     */

    fun getSummary(): String =
        """
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

/**
Database table for storing payment records.

Includes payment status, card details (last digits only),
timestamps, and refund tracking.
 */

object Payments : Table() {
    val paymentID = varchar("paymentID", 50)
    val bookingID = varchar("bookingID", 50)
    val userID = reference("userID", Users.userId)
    val amount = double("amount")
    val lastFourDigits = varchar("lastFourDigits", 4)
    val cardHolderName = varchar("cardHolderName", 100)
    val status = varchar("status", 20)
    val timestamp = varchar("timestamp", 50)
    val refundedAt = varchar("refundedAt", 50).nullable()

    override val primaryKey = PrimaryKey(paymentID)
}
