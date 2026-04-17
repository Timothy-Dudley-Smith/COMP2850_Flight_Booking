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

class TicketService {

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
                managerNote = null
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
                    managerNote = row[SupportTickets.managerNote]
                )
            }
        }
    }

    fun updateTicket(ticketId: Int, request: UpdateTicketRequest): TicketResponse? {
        return transaction {
            val now = LocalDateTime.now().toString()

            val row = SupportTickets.selectAll().firstOrNull { resultRow ->
                resultRow[SupportTickets.suppTickId] == ticketId
            }

            if (row == null) {
                null
            } else {
                SupportTickets.update({ SupportTickets.suppTickId eq ticketId }) {
                    it[status] = request.status
                    it[updatedAt] = now
                    it[managerNote] = request.managerNote
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
                    managerNote = request.managerNote
                )
            }
        }
    }
}