package com.flightsystem.model

import com.flightsystem.flightservice.BookingService

class Manager(
    userId: Int,
    firstName: String,
    lastName: String,
    dateOfBirth: String,
    email: String,
    passwordHash: String,
    salt: String,
) : User(userId, firstName, lastName, dateOfBirth, email, passwordHash, salt) {
    fun resetuserPassword(
        user: User,
        newPasswordHash: String,
        newSalt: String,
    ) {
        user.updatePassword(newPasswordHash, newSalt)
        println("Password reset for user: ${user.userId}")
    }

    fun unlockuserAccount(user: User) {
        user.unlockAccount()
        println("Account unlocked for user: ${user.userId}")
    }

    fun viewUserBookings(userId: Int): List<Booking> = BookingService().getBookingsByUser(userId)

    fun cancelBooking(bookingId: Int): Boolean = BookingService().cancelBooking(bookingId)
}
