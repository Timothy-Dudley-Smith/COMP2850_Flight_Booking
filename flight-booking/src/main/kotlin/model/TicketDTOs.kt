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
    val managerNote: String? = null
)