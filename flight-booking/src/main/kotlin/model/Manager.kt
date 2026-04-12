package com.flightsystem.model

import com.flightsystem.service.BookingService

class Manager(
    userId: Int,
    name: String,
    email: String,
    passwordHash: String,
    salt: String
) : User(userId, name, email, passwordHash, salt) {

    fun resetuserPassword(user: User, newPasswordHash: String, newSalt: String) {
        user.updatePassword(newPasswordHash, newSalt)
        println("Password reset for user: ${user.userId}")
    }

    fun unlockuserAccount(user: User) {
        user.unlockAccount()
        println("Account unlocked for user: ${user.userId}")
    }

    fun viewUserBookings(userId: Int): List<Booking> {
        return BookingService().getBookingsByUser(userId)
    }

    fun cancelBooking(bookingId: Int): Boolean {
        return BookingService().cancelBooking(bookingId)
    }


}