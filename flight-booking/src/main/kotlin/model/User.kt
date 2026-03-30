package com.flightsystem.model

import org.jetbrains.exposed.sql.Table

import java.time.LocalDateTime
private const val VARCHAR_LENGTH = 255

open class User(
    val userId: String,
    var name: String,
    var email: String,
    private var passwordHash: String,
    private var salt: String
) {
    private val bookings: MutableList<Booking> = mutableListOf()

    private var loyaltyPoints: Int = 0

    private var lastLogin: LocalDateTime? = null
    private var accountLocked: Boolean = false
    private var lockedAt: LocalDateTime? = null
    private var failedLoginAttempts: Int = 0


    private var seatPreference = "ANY"

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_MINUTES = 30L
        private const val POINTS_PER_UNIT_SPENT = 10

    }

    fun updateDetails(newName: String, newEmail: String) {
        require(newName.isNotBlank()) {"Name cannot be empty"}
        require(EMAIL_REGEX.matches(newEmail)) {"Invalid email format"}
        name = newName
        email = newEmail
    }

    protected fun updatePassword(newPasswordHash: String, newSalt: String) {
        passwordHash = newPasswordHash
        salt = newSalt
    }

    fun verifyPassword(candidateHash: String): Boolean {
        return candidateHash == passwordHash
    }

    fun getPasswordHash(): String {
        return passwordHash
    }

    fun getSalt(): String {
        return salt
    }

    fun addBooking(booking: Booking) {
        bookings.add(booking)

        loyaltyPoints += (booking.totalPrice / POINTS_PER_UNIT_SPENT).toInt()
    }

    fun removeBooking(booking: Booking) {
        if (bookings.remove(booking)) {
            loyaltyPoints -= (booking.totalPrice / POINTS_PER_UNIT_SPENT).toInt()
            if (loyaltyPoints < 0) {
                loyaltyPoints = 0
            }
        }
    }

    fun getBookings(): List<Booking> {
        return bookings
    }

    fun getLoyaltyPoints(): Int = loyaltyPoints

    fun redeemPoints(points: Int): Boolean {
        return if (points <= loyaltyPoints) {
            loyaltyPoints -= points
            true
        } else false
    }

    fun setSeatPreference(preference: String) {
        seatPreference = preference
    }

    fun getSeatPreference(): String = seatPreference

    fun recordLoginSuccess() {
        failedLoginAttempts = 0
        accountLocked = false
        lockedAt = null
        lastLogin = LocalDateTime.now()
    }

    fun recordLoginFailure() {
        failedLoginAttempts++
        if (failedLoginAttempts >=MAX_FAILED_ATTEMPTS ) {
            accountLocked = true
            lockedAt = LocalDateTime.now()
        }
    }

    fun isLocked(): Boolean {
        if (accountLocked && lockedAt != null) {
            if (LocalDateTime.now().isAfter(lockedAt!!.plusMinutes(LOCKOUT_MINUTES))) {
                unlockAccount()
            }
        }

        return accountLocked
    }

    fun unlockAccount() {
        accountLocked = false
        failedLoginAttempts = 0
        lockedAt = null
    }

    fun getLastLogin(): LocalDateTime? = lastLogin



}

object Users : Table() {
    val userId = integer("user_id").autoIncrement()
    val name = varchar("user_name", VARCHAR_LENGTH)
    val email = varchar("email", VARCHAR_LENGTH)
    val passwordHash = varchar("passwordHash", VARCHAR_LENGTH)
    val salt = varchar("salt", VARCHAR_LENGTH)

    override val primaryKey = PrimaryKey(userId)
}