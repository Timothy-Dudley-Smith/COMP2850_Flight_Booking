package com.flightsystem.service

import com.flightsystem.model.Payment
import java.time.LocalDateTime
import java.util.UUID

class PaymentService {

    private val payments: MutableList<Payment> = mutableListOf()

    companion object {
        private const val ESTIMATED_FAILURE_RATE = 0.1
        private const val DELAY_MS = 1500L
    }


    fun processPayment(
        bookingID: String,
        userID: Int,
        amount: Double,
        cardNumber: String,
        cardHolderName: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvv: String
    ): Result<Payment> {


        val validationResult = validateCard(
            cardNumber,
            cardHolderName,
            expiryMonth,
            expiryYear,
            cvv
        )

        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        val lastFourDigits = cardNumber.replace(" ", "").takeLast(4)

        val payment = Payment(
            paymentID = UUID.randomUUID().toString(),
            bookingID = bookingID,
            userID = userID,
            amount = amount,
            lastFourDigits = lastFourDigits,
            cardHolderName = cardHolderName
        )

        Thread.sleep(DELAY_MS)

        return if (Math.random() > ESTIMATED_FAILURE_RATE) {
            payment.setStatusSuccess()
            payments.add(payment)
            Result.success(payment)
        } else {
            payment.setStatusFailed()
            payments.add(payment)
            Result.failure(IllegalStateException("Payment declined. Ensure card details are correct. "))
        }

    }


    fun refundPayment(paymentID: String): Result<Payment> {
        val payment = payments.find {it.paymentID == paymentID}
            ?: return Result.failure(IllegalArgumentException("Payment $paymentID not found"))

        if (!payment.isRefundable())
            return Result.failure(IllegalStateException("Payment $paymentID cannot be refunded"))

        Thread.sleep(DELAY_MS)

        payment.setRefunded()
        return Result.success(payment)

    }

    fun getPaymentbooking(bookingID: String): Payment? {
        return payments.find {it.bookingID == bookingID }

    }

    fun getPaymentuser(userID: Int): List<Payment> {
        return payments.filter {it.userID == userID }
    }

    fun getPaymentid(paymentID: String): Payment? {
        return payments.find {it.paymentID == paymentID}
    }

    private fun validateCard(
        cardNumber: String,
        cardHolderName: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvv: String
    ): Result<Unit> {


        val digitsOnly = cardNumber.replace(" ", "")
        if (digitsOnly.length != 16 || !digitsOnly.all {it.isDigit() })
            return Result.failure(IllegalArgumentException("Invalid card number - Needs to be 16 digits "))


        if (!validatingNumberOnCard(digitsOnly))
            return Result.failure(IllegalArgumentException("Invalid card number"))

        if (cardHolderName.isBlank())
            return Result.failure(IllegalArgumentException("Cardholder name must not be blank"))

        val present = LocalDateTime.now()

        if (expiryMonth < 1 || expiryMonth > 12)
            return Result.failure(IllegalArgumentException("Invalid expiry month"))

        if (expiryYear < present.year || (expiryYear == present.year && expiryMonth < present.monthValue))
            return Result.failure(IllegalArgumentException("Card has expired"))


        if (cvv.length !in 3..4 || !cvv.all {it.isDigit() })
            return Result.failure(IllegalArgumentException("Invalid CVV"))

        return Result.success(Unit)


    }

    private fun validatingNumberOnCard(cardNumber: String): Boolean {
        var sum = 0
        var isEven = false

        for (x in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[x].digitToInt()

            if (isEven) {
                digit *= 2
                if (digit > 9) digit -=9
            }

            sum += digit
            isEven = !isEven
        }

        return sum % 10 == 0
    }






}
