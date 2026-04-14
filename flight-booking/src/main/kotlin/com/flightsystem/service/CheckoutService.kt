package com.flightsystem.service

import com.flightsystem.model.PaymentRequest
import com.flightsystem.model.PaymentResponse
import java.time.LocalDateTime
import com.flightsystem.model.PassengerInput
import com.flightsystem.service.PassengerService


class CheckoutService(
    private val priceHoldService: PriceHoldService,
    private val paymentService: PaymentService,
    private val loyaltyService: LoyaltyService,
    private val passengerService: PassengerService,
) {

    fun checkout(
        holdId: Int,
        request: PaymentRequest,
        passengers: List<PassengerInput>,
        pointsToRedeem: Int = 0
    ): PaymentResponse {

        val holdDetails = priceHoldService.getHoldDetails(holdId)
            ?: return PaymentResponse(
                success = false,
                message = "Invalid hold ID or hold not found",
                paymentId = null,
                bookingId = null
            )

        val hold = holdDetails.hold

        val expiryTime = try {
            LocalDateTime.parse(hold.expiryTime)
        } catch (e: Exception) {
            return PaymentResponse(
                success = false,
                message = "Invalid hold expiry format",
                paymentId = null,
                bookingId = null
            )
        }

        if (LocalDateTime.now().isAfter(expiryTime)) {
            priceHoldService.expireHold(holdId)
            return PaymentResponse(
                success = false,
                message = "This hold has expired",
                paymentId = null,
                bookingId = null
            )
        }

        if (pointsToRedeem < 0) {
            return PaymentResponse(
                success = false,
                message = "Points to redeem cannot be negative",
                paymentId = null,
                bookingId = null
            )
        }

        var finalAmount = hold.totalPrice

        if (pointsToRedeem > 0) {
            val loyaltyAccount = loyaltyService.getLoyaltyAccount(hold.userId)
                ?: return PaymentResponse(
                    success = false,
                    message = "No loyalty account found for this user",
                    paymentId = null,
                    bookingId = null
                )

            if (loyaltyAccount.loyaltyPoints < pointsToRedeem) {
                return PaymentResponse(
                    success = false,
                    message = "Not enough loyalty points",
                    paymentId = null,
                    bookingId = null
                )
            }

            finalAmount = loyaltyService.applyDiscount(
                originalPrice = hold.totalPrice,
                pointsToRedeem = pointsToRedeem
            )
        }

        val paymentResult = paymentService.processPayment(
            bookingID = "HOLD-$holdId",
            userID = hold.userId,
            amount = finalAmount,
            cardNumber = request.cardNumber,
            cardHolderName = request.cardholderName,
            expiryMonth = request.expiryMonth,
            expiryYear = request.expiryYear,
            cvv = request.cvv
        )

        if (paymentResult.isFailure) {
            return PaymentResponse(
                success = false,
                message = paymentResult.exceptionOrNull()?.message ?: "Payment not accepted",
                paymentId = null,
                bookingId = null
            )
        }

        val payment = paymentResult.getOrNull()!!

        if (pointsToRedeem > 0) {
            loyaltyService.redeemPoints(hold.userId, pointsToRedeem)
        }

        val booking = priceHoldService.confirmHoldToBooking(holdId)
            ?: return PaymentResponse(
                success = false,
                message = "Payment succeeded but booking creation failed",
                paymentId = payment.paymentID,
                bookingId = null
            )

            passengerService.addPassengersToBooking(
                booking.bookingId,
                passengers
            )

        val pointsEarned = finalAmount.toInt()
        loyaltyService.addPoints(hold.userId, pointsEarned)

        return PaymentResponse(
            success = true,
            message = "Payment successful and booking confirmed",
            paymentId = payment.paymentID,
            bookingId = booking.bookingId
        )
    }
}