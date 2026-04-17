package com.flightsystem.service

import com.flightsystem.model.PaymentRequest
import com.flightsystem.model.Users
import com.flightsystem.model.Flights
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import com.flightsystem.model.PaymentResponse
import java.time.LocalDateTime

class CheckoutService(
    private val priceHoldService: PriceHoldService,
    private val paymentService: PaymentService,
    private val loyaltyService: LoyaltyService,



) {
    private val ticketPdfService = TicketPdfService()


    private val emailService = EmailService(
        smtpHost = "smtp.gmail.com",
        smtpPort = "587",
        smtpUsername = "",
        smtpPassword = "",
        fromEmail = "YOUR_EMAIL@gmail.com"
    )

    private fun getUserEmailAndName(userId: Int): Pair<String, String>? {
        return transaction {
            val row = Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                ?: return@transaction null

            val email = row[Users.email]
            val fullName = "${row[Users.firstName]} ${row[Users.lastName]}".trim()

            Pair(email, fullName)
        }
    }

    private fun getFlightDisplayDetails(flightId: String): Triple<String, String, String>? {
        return transaction {
            val row = Flights.selectAll().where { Flights.flightId eq flightId }.singleOrNull()
                ?: return@transaction null

            val route = "${row[Flights.departureAirport]} → ${row[Flights.arrivalAirport]}"
            val date = row[Flights.date]
            val timeRange = "${row[Flights.departureTime]} - ${row[Flights.arrivalTime]}"

            Triple(route, date, timeRange)
        }
    }

    fun checkout(
        holdId: Int,
        request: PaymentRequest,
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

        val pointsEarned = finalAmount.toInt()
        loyaltyService.addPoints(hold.userId, pointsEarned)

        try {
            val userDetails = getUserEmailAndName(hold.userId)
            val flightDetails = getFlightDisplayDetails(hold.flightId)

            if (userDetails != null) {
                val (email, fullName) = userDetails

                val route = flightDetails?.first ?: hold.flightId
                val date = flightDetails?.second ?: "Date unavailable"
                val timeRange = flightDetails?.third ?: "Time unavailable"

                val ticketPdf = ticketPdfService.generateTicketPdf(
                    bookingId = booking.bookingId.toString(),
                    passengerName = fullName,
                    route = route,
                    date = "$date • $timeRange",
                    seats = holdDetails.seats.joinToString(", "),
                    total = finalAmount
                )

                emailService.sendBookingConfirmationEmail(
                    toEmail = email,
                    passengerName = fullName,
                    bookingId = booking.bookingId.toString(),
                    route = route,
                    date = "$date • $timeRange",
                    seats = holdDetails.seats.joinToString(", "),
                    total = finalAmount,
                    ticketPdfBytes = ticketPdf
                )
            }
        } catch (e: Exception) {
            println("Booking email failed to send: ${e.message}")
        }

        return PaymentResponse(
            success = true,
            message = "Payment successful and booking confirmed",
            paymentId = payment.paymentID,
            bookingId = booking.bookingId
        )
    }
}