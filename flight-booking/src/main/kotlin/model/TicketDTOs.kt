package model

import kotlinx.serialization.Serializable

@Serializable
data class CreateTicketRequest(
    val bookingId: Int,
    val customerName: String,
    val customerEmail: String,
    val requestType: String,
    val message: String
)

@Serializable
data class UpdateTicketRequest(
    val status: TicketStatus,
    val managerNote: String? = null
)

@Serializable
data class TicketResponse(
    val id: Int,
    val bookingId: Int,
    val customerName: String,
    val customerEmail: String,
    val requestType: String,
    val message: String,
    val status: TicketStatus,
    val createdAt: String,
    val updatedAt: String? = null,
    val managerNote: String? = null,
    val archived: Boolean = false
)

@Serializable
data class TicketHistoryResponse(
    val historyId: Int,
    val ticketId: Int,
    val oldStatus: TicketStatus,
    val newStatus: TicketStatus,
    val managerNote: String? = null,
    val changedAt: String
)