package com.flightsystem.controller

import com.flightsystem.model.PaymentRequest
import com.flightsystem.model.PaymentResponse
import com.flightsystem.service.PaymentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class PaymentController {

    private val paymentService = PaymentService()

    @PostMapping("/payment")
    fun processPayment(@RequestBody request: PaymentRequest): PaymentResponse {

        val result = paymentService.processPayment(
            bookingID = "TEMP-BOOKING-001",
            userID = "TEMP-USER-001",
            amount = 199.99,
            cardNumber = request.cardNumber,
            cardHolderName = request.cardholderName,
            expiryMonth = request.expiryMonth,
            expiryYear = request.expiryYear,
            cvv = request.cvv
        )

        return if (result.isSuccess) {
            val payment = result.getOrNull()!!
            PaymentResponse(
                success = true,
                message = "Payment successful",
                paymentID = payment.paymentID
            )
        } else {
            PaymentResponse(
                success = false,
                message = result.exceptionOrNull()?.message ?: "Payment failed"
            )
        }
    }
}