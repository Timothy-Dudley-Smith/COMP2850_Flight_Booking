package com.flightsystem.model
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Table

private const val VARIABLE_LENGTH = 128

object Users : Table() {
    val userId = integer("user_id", VARIABLE_LENGTH)
    val name = varchar("user_name", VARIABLE_LENGTH)
    val email = varchar("email", VARIABLE_LENGTH).uniqueIndex()
    val passwordHash = varchar("passwordHash", VARIABLE_LENGTH)
    val salt = varchar("salt", VARIABLE_LENGTH)
    val seatPreference = varchar("seat_preference", 10).default("ANY")
    val accountLocked = bool("account_locked").default("false")
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedAt = varchar("locked_at", VARIABLE_LENGTH).nullable()
    val lastLogin = varchar("last_login", VARIABLE_LENGTH).nullable()
    val role = varchar("role", 20).default("USER")
    override val primaryKey = PrimaryKey(userId)
}