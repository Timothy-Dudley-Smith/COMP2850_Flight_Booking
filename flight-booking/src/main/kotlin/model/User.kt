package com.flightsystem.model



import java.time.LocalDateTime


open class User(
    val userId: String,
    var name: String,
    var email: String,
    private var passwordHash: String,
    private var salt: String
) {


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

    fun updatePassword(newPasswordHash: String, newSalt: String) {
        passwordHash = newPasswordHash
        salt = newSalt
    }

    fun verifyPassword(candidateHash: String): Boolean {
        return candidateHash == passwordHash
    }

    fun getSalt(): String {
        return salt
    }

    fun addBooking(booking: Booking) {
        bookings.add(booking)

        loyaltyPoints += calculatePoints(booking.totalPrice)
    }

    fun removeBooking(booking: Booking) {
        if (bookings.remove(booking)) {
            loyaltyPoints -= calculatePoints(booking.totalPrice)
            if (loyaltyPoints < 0) {
                loyaltyPoints = 0
            }
        }
    }

    private fun calculatePoints(price: Double): Int {
        return (price / POINTS_PER_UNIT_SPENT).toInt()
    }

    fun getBookings(): List<Booking> {
        return bookings.toList()
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
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS ) {
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

