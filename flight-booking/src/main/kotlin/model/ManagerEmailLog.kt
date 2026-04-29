package model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

object ManagerSentEmails : Table("manager_sent_emails") {
    val emailId = integer("email_id").autoIncrement()
    val managerId = integer("manager_id")
    val managerEmail = varchar("manager_email", 150)
    val toEmail = varchar("to_email", 150)
    val subject = varchar("subject", 255)
    val message = text("message")
    val sentAt = varchar("sent_at", 50)

    override val primaryKey = PrimaryKey(emailId)
}

@Serializable
data class ManagerSentEmailResponse(
    val emailId: Int,
    val managerId: Int,
    val managerEmail: String,
    val toEmail: String,
    val subject: String,
    val message: String,
    val sentAt: String
)