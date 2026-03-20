package com.flightsystem.model
import java.time.LocalDateTime

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
    private var failedLoginAttempts: Int = 0

    private var seatPreference = "ANY"

    fun updateDetails(newName: String, newEmail: String) {
        require(newName.isNotBlank()) {"Name cannot be empty"}
        require(newEmail.contains("@")) {"Invalid email format"}
        name = newName
        email = newEmail
    }

    fun updatePassword(newPasswordHash: String, newSalt: String) {
        passwordHash = newPasswordHash
        salt = newSalt
    }

    fun getPasswordHash(): String {
        return passwordHash
    }

    fun getSalt(): String {
        return salt
    }

    fun addBooking(booking: Booking) {
        bookings.add(booking)

        loyaltyPoints += (booking.totalPrice / 10).toInt()
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

    fun getSeatPreference(): String = preference

    fun recordLoginSuccess() {
        failedLoginAttempts = 0
        lastLogin = LocalDateTime.now()
    }

    fun recordLoginFailure() {
        failedLoginAttempts++
        if (failedLoginAttempts >=5 ) {
            accountLocked = true
        }
    }

    fun isLocked(): Boolean = accountLocked



}