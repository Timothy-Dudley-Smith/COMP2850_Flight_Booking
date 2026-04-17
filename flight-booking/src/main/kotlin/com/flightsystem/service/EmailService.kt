package com.flightsystem.service

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.activation.DataHandler
import javax.mail.util.ByteArrayDataSource

class EmailService(
    private val smtpHost: String,
    private val smtpPort: String,
    private val smtpUsername: String,
    private val smtpPassword: String,
    private val fromEmail: String
) {

    fun sendBookingConfirmationEmail(
        toEmail: String,
        passengerName: String,
        bookingId: String,
        route: String,
        date: String,
        seats: String,
        total: Double,
        ticketPdfBytes: ByteArray
    ) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUsername, smtpPassword)
            }
        })

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(fromEmail))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
        message.subject = "Booking Confirmation - $bookingId"

        val textPart = MimeBodyPart()
        textPart.setText(
            """
    Hello $passengerName,

    Thank you for booking with us. Your booking has been confirmed.

    BOOKING SUMMARY
    ------------------------------
    Booking Reference: $bookingId
    Route: $route
    Travel Date: $date
    Seat Assignment: $seats
    Total Paid: £${"%.2f".format(total)}
    ------------------------------

    Your e-ticket is attached as a PDF.

    Please keep this email for your records and arrive at the airport at least 2 hours before departure.

    Kind regards,
    Astraeus Support
    """.trimIndent()
        )

        val attachmentPart = MimeBodyPart()
        attachmentPart.dataHandler = DataHandler(
            ByteArrayDataSource(ticketPdfBytes, "application/pdf")
        )
        attachmentPart.fileName = "ticket-$bookingId.pdf"

        val multipart = MimeMultipart()
        multipart.addBodyPart(textPart)
        multipart.addBodyPart(attachmentPart)

        message.setContent(multipart)

        Transport.send(message)
    }
}