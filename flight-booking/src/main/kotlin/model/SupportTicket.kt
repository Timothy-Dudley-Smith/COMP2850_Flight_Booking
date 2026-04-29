package model

import org.jetbrains.exposed.sql.Table

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}

object SupportTickets : Table("support_tickets") {
    val suppTickId = integer("id").autoIncrement()
    val bookingId = integer("booking_id")
    val customerName = varchar("customer_name", 100)
    val customerEmail = varchar("customer_email", 150)
    val requestType = varchar("request_type", 50)
    val message = text("message")
    val status = enumerationByName("status", 20, TicketStatus::class)
    val createdAt = varchar("created_at", 50)
    val updatedAt = varchar("updated_at", 50).nullable()
    val managerNote = text("manager_note").nullable()
    val archived = bool("archived").default(false)

    override val primaryKey = PrimaryKey(suppTickId)
}

object SupportTicketHistory : Table("support_ticket_history") {
    val historyId = integer("history_id").autoIncrement()
    val ticketId = integer("tcket_id")
    val oldStatus = enumerationByName("old_status", 20, TicketStatus::class)
    val newStatus = enumerationByName("new_status", 20, TicketStatus::class)
    val managerNote = text("manager_note").nullable()
    val changedAt = varchar("changed_at", 50)

    override val primaryKey = PrimaryKey(historyId)
}