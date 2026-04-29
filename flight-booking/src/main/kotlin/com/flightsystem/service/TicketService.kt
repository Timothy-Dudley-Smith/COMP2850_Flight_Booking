package com.flightsystem.service

import model.CreateTicketRequest
import model.SupportTickets
import model.TicketResponse
import model.TicketStatus
import model.UpdateTicketRequest
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import model.SupportTicketHistory
import model.TicketHistoryResponse
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq


class TicketService (

    private val emailService: EmailService

) {



    fun createTicket(request: CreateTicketRequest): TicketResponse {
        return transaction {
            val now = LocalDateTime.now().toString()

            val insertedRow = SupportTickets.insert {
                it[bookingId] = request.bookingId
                it[customerName] = request.customerName
                it[customerEmail] = request.customerEmail
                it[requestType] = request.requestType
                it[message] = request.message
                it[status] = TicketStatus.OPEN
                it[createdAt] = now
                it[updatedAt] = null
                it[managerNote] = null
            }

            val insertedId = insertedRow[SupportTickets.suppTickId]

            TicketResponse(
                id = insertedId,
                bookingId = request.bookingId,
                customerName = request.customerName,
                customerEmail = request.customerEmail,
                requestType = request.requestType,
                message = request.message,
                status = TicketStatus.OPEN,
                createdAt = now,
                updatedAt = null,
                managerNote = null,
                archived = false
            )
        }
    }

    fun getAllTickets(): List<TicketResponse> {
        return transaction {
            SupportTickets.selectAll().map { row ->
                TicketResponse(
                    id = row[SupportTickets.suppTickId],
                    bookingId = row[SupportTickets.bookingId],
                    customerName = row[SupportTickets.customerName],
                    customerEmail = row[SupportTickets.customerEmail],
                    requestType = row[SupportTickets.requestType],
                    message = row[SupportTickets.message],
                    status = row[SupportTickets.status],
                    createdAt = row[SupportTickets.createdAt],
                    updatedAt = row[SupportTickets.updatedAt],
                    managerNote = row[SupportTickets.managerNote],
                    archived = row[SupportTickets.archived]
                )
            }
        }
    }

    fun updateTicket(ticketId: Int, request: UpdateTicketRequest): TicketResponse? {
        return transaction {
            val now = LocalDateTime.now().toString()

            // reuse existing booking logic 
            val bookingService = BookingService()

            val row = SupportTickets.selectAll().firstOrNull { resultRow ->
                resultRow[SupportTickets.suppTickId] == ticketId
            }

            if (row == null) {
                null
            } else {
                val oldStatus = row[SupportTickets.status]

                if (
                    request.status == TicketStatus.RESOLVED &&
                    row[SupportTickets.requestType] == "CHANGE_BOOKING"
                ) {
                    val managerNote = request.managerNote ?: ""

                    val looksLikeSeatUpdate = Regex("""^\s*\d+[A-F](\s*,\s*\d+[A-F])*\s*$""")
                        .matches(managerNote)

                    if (looksLikeSeatUpdate) {
                        val newSeatNumbers = managerNote
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        val bookingUpdated = bookingService.updateBookingSeats(
                            bookingId = row[SupportTickets.bookingId],
                            newSeatNumbers = newSeatNumbers
                        )

                        if (!bookingUpdated) {
                            return@transaction null
                        }
                    }
                }
                SupportTickets.update({ SupportTickets.suppTickId eq ticketId }) {
                    it[status] = request.status
                    it[updatedAt] = now
                    it[managerNote] = request.managerNote
                }
                if (oldStatus != request.status || request.managerNote != row[SupportTickets.managerNote]) {
                    SupportTicketHistory.insert {
                        it[SupportTicketHistory.ticketId] = ticketId
                        it[SupportTicketHistory.oldStatus] = oldStatus
                        it[SupportTicketHistory.newStatus] = request.status
                        it[SupportTicketHistory.managerNote] = request.managerNote
                        it[SupportTicketHistory.changedAt] = now
                    }
                }


                if (request.status == TicketStatus.RESOLVED || request.status == TicketStatus.REJECTED) {
                    val email = row[SupportTickets.customerEmail]
                    val name = row[SupportTickets.customerName]
                    val note = request.managerNote ?: "No additional information provided."

                    val subject = "Update on your Astraeus support ticket #$ticketId"

                    val body = """
        Hello $name,

        Your support ticket (ID: $ticketId) has been updated.

        New status: ${request.status}

        Manager message:
        $note

        If you need any further help, please contact Astraeus Support again.

        Kind regards,
        Astraeus Support
    """.trimIndent()

                    try {
                        emailService.sendEmail(
                            toEmail = email,
                            subject = subject,
                            body = body
                        )
                    } catch (e: Exception) {
                        println("Failed to send support ticket email: ${e.message}")
                    }
                }

                TicketResponse(
                    id = row[SupportTickets.suppTickId],
                    bookingId = row[SupportTickets.bookingId],
                    customerName = row[SupportTickets.customerName],
                    customerEmail = row[SupportTickets.customerEmail],
                    requestType = row[SupportTickets.requestType],
                    message = row[SupportTickets.message],
                    status = request.status,
                    createdAt = row[SupportTickets.createdAt],
                    updatedAt = now,
                    managerNote = request.managerNote,
                    archived = row[SupportTickets.archived]
                )
            }
        }
    }

    fun getTicketHistory(ticketId: Int): List<TicketHistoryResponse> {
        return transaction {
            SupportTicketHistory.selectAll()
                .where { SupportTicketHistory.ticketId eq ticketId }
                .map { row ->
                TicketHistoryResponse(
                    historyId = row[SupportTicketHistory.historyId],
                    ticketId = row[SupportTicketHistory.ticketId],
                    oldStatus = row[SupportTicketHistory.oldStatus],
                    newStatus = row[SupportTicketHistory.newStatus],
                    managerNote = row[SupportTicketHistory.managerNote],
                    changedAt = row[SupportTicketHistory.changedAt]
                )}
        }
    }

    fun archiveTicket(ticketId: Int): Boolean {
        return transaction {
            val updatedRows = SupportTickets.update(
                where = { SupportTickets.suppTickId eq ticketId }
            ) { row -> 
                row[SupportTickets.archived] = true 
            }
            updatedRows > 0
        }
    }
}
