package com.flightsystem.model
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Table

private const val VARCHAR_LENGTH = 255

object Users : Table() {
    val userId = varchar("user_id", VARCHAR_LENGTH)
    val name = varchar("user_name", VARCHAR_LENGTH)
    val email = varchar("email", VARCHAR_LENGTH).uniqueIndex()
    val passwordHash = varchar("passwordHash", VARCHAR_LENGTH)
    val salt = varchar("salt", VARCHAR_LENGTH)
    val loyaltyPoints = integer("loyalty_points").default(0)
    val seatPreference = varchar("seat_preference", 10).default("ANY")
    val accountLocked = bool("account_locked")
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedAt = varchar("locked_at", VARCHAR_LENGTH).nullable()
    val lastLogin = varchar("last_login", VARCHAR_LENGTH).nullable()
    val role = varchar("role", 20).default("USER")
    override val primaryKey = PrimaryKey(userId)
}