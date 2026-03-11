package com.flightsystem.model

open class User(
    val userId: String,
    val name: String,
    var email: String,
    private var passwordHash: String
) {
    private val bookings: MutableList<Booking> = MutableListOf()

    fun updateDetails(newName: String, newEmail: String) {
        name = newName
        email = newEmail
    }

    fun updatePassword(newPasswordHash: String) {
        passwordHash = newPasswordHash
    }

    fun getPasswordHash(): String {
        return passwordHash
    }

    fun addBooking(booking: Booking) {
        bookings.add(booking)
    }

    fun getBookings(): List<booking> {
        return bookings
    }
}