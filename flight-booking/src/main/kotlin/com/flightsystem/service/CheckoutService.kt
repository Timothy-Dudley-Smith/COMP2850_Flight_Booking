package com.flightsystem.service

import com.flightsystem.model.PaymentRequest
import com.flightsystem.model.Users
import com.flightsystem.model.Flights
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.flightsystem.model.PaymentResponse
import com.flightsystem.AppEnv
import java.time.LocalDateTime

class CheckoutService(
    private val priceHoldService: PriceHoldService,
    private val paymentService: PaymentService,
    private val loyaltyService: LoyaltyService,
    private val promoCodeService: PromoCodeService

) {
    private val ticketPdfService = TicketPdfService()


    private val emailService = EmailService(
        smtpHost = "smtp.gmail.com",
        smtpPort = "587",
        smtpUsername = AppEnv.require("SMTP_USERNAME"),
        smtpPassword = AppEnv.require("SMTP_PASSWORD"),
        fromEmail = AppEnv.require("SMTP_USERNAME")
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
        returnHoldId: Int? = null,
        request: PaymentRequest,
        pointsToRedeem: Int = 0,
        promoCode: String? = null,
        cabin: String? = null,
        addOns: String? = null,
        guestEmail: String? = null

    ): PaymentResponse {

        val holdDetails = priceHoldService.getHoldDetails(holdId)
            ?: return PaymentResponse(
                success = false,
                message = "Invalid hold ID or hold not found",
                paymentId = null,
                bookingId = null
            )

        val hold = holdDetails.hold

        val returnHoldDetails = returnHoldId?.let {
            priceHoldService.getHoldDetails(it)
        }

        val returnHold = returnHoldDetails?.hold



        val userId = hold.userId

        val userRow = transaction {
            Users.selectAll().where { Users.userId eq userId }.singleOrNull()
        }
        if (userRow == null) {
            return PaymentResponse(
                success = false,
                message = "Invalid user ID",
                paymentId = null,
                bookingId = null
            )
        }

        val userEmail = userRow[Users.email]

        val isGuestBooking = userEmail == "guest@astraeus.local"
        val trimmedGuestEmail = guestEmail?.trim()
        val confirmationEmail = if (isGuestBooking) trimmedGuestEmail else userEmail


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

        returnHold?.let {
            finalAmount += it.totalPrice
        }

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
                originalPrice = finalAmount,
                pointsToRedeem = pointsToRedeem
            )
        }

        if (!promoCode.isNullOrBlank()) {
            if (promoCodeService.hasUserUsedPromoCode(hold.userId, promoCode)) {
                return PaymentResponse(
                    success = false,
                    message = "You have already used this promo code",
                    paymentId = null,
                    bookingId = null
                )
            }

            val promoResult = promoCodeService.applyPromoCode(
                codeValue = promoCode,
                originalAmount = finalAmount
            )

            if (promoResult.isFailure) {
                return PaymentResponse(
                    success = false,
                    message = promoResult.exceptionOrNull()?.message ?: "Invalid promo code",
                    paymentId = null,
                    bookingId = null
                )
            }

            finalAmount = promoResult.getOrNull()!!
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

        val outboundBooking = priceHoldService.confirmHoldToBooking(holdId, cabin, addOns)
            ?: return PaymentResponse(
                success = false,
                message = "Payment succeeded but outbound booking creation failed",
                paymentId = payment.paymentID,
                bookingId = null,
            )
        val returnBooking = if (returnHoldId != null) {
            priceHoldService.confirmHoldToBooking(returnHoldId, cabin, addOns)
        } else {
            null
        }



        val pointsEarned = finalAmount.toInt()
        loyaltyService.addPoints(hold.userId, pointsEarned)

        if (!promoCode.isNullOrBlank()) {
            promoCodeService.recordPromoCodeUsage(
                userId = hold.userId,
                codeValue = promoCode,
            )
        }


        val updatedLoyaltyAccount = loyaltyService.getLoyaltyAccount(hold.userId)

        try {
            val userDetails = getUserEmailAndName(hold.userId)
            val flightDetails = getFlightDisplayDetails(hold.flightId)
            val returnFlightDetails = returnHold?.let { getFlightDisplayDetails(it.flightId) }

            if (userDetails != null) {
                val (email, fullName) = userDetails

                val route = flightDetails?.first ?: hold.flightId
                val date = flightDetails?.second ?: "Date unavailable"
                val timeRange = flightDetails?.third ?: "Time unavailable"

                val returnRoute = returnFlightDetails?.first
                val returnDateText = returnFlightDetails?.let { "${it.second} . ${it.third}" }

                val returnSeatsText = returnHoldDetails?.seats?.joinToString ( ", " )



                val ticketPdf = ticketPdfService.generateTicketPdf(
                    bookingId = outboundBooking.bookingId.toString(),
                    passengerName = fullName,
                    route = route,
                    date = "$date • $timeRange",
                    seats = holdDetails.seats.joinToString(", "),
                    total = finalAmount,
                    returnBookingId = returnBooking?.bookingId?.toString(),
                    returnRoute = returnRoute,
                    returnDate = returnDateText,
                    returnSeats = returnSeatsText

                )

                emailService.sendBookingConfirmationEmail(
                    toEmail = confirmationEmail,
                    passengerName = fullName,
                    bookingId = outboundBooking.bookingId.toString(),
                    route = route,
                    date = "$date • $timeRange",
                    seats = holdDetails.seats.joinToString(", "),
                    total = finalAmount,
                    ticketPdfBytes = ticketPdf,
                    returnBookingId = returnBooking?.bookingId?.toString(),
                    returnRoute = returnRoute,
                    returnDate = returnDateText,
                    returnSeats = returnSeatsText
                )

                println("Booking confirmed!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Booking email failed to send: ${e.message}")
        }

        return PaymentResponse(
            success = true,
            message = "Payment successful and booking confirmed",
            paymentId = payment.paymentID,
            bookingId = outboundBooking.bookingId,
            returnBookingId = returnBooking?.bookingId,
            pointsEarned = pointsEarned,
            pointsUsed = pointsToRedeem,
            updatedPointsTotal = updatedLoyaltyAccount?.loyaltyPoints,
            finalAmountPaid = finalAmount
        )
    }
}