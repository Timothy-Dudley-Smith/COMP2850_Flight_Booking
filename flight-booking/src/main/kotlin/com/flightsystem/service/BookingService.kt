package com.flightsystem.service 

import com.flightsystem.model.*

class BookingService {
    // booking storage list
    private val bookings = mutableListOf<Booking>()

    fun calculateTotalPrice(flight: Flight, seats: List<Seat>): Double {
        return flight.price * seats.size
    }

    // check if seat is available
    fun validateSeats(requestedSeats: List<Seat>): Boolean {
        for (seat in requestedSeats) {
            if(!seat.isAvailable) {
                return false
            }
        }
        return true    
    }

    // create booking, use validateSeats()
    fun createBooking(
        bookingID: String,
        user: User,
        flight: Flight,
        requestedSeats: List<Seat>,
        payment: Payment
    ): Booking {

        if(!validateSeats(requestedSeats)) {
            throw IllegalArgumentException("One or more seats are unavailable")
        }

        for (seat in requestedSeats) {
            seat.isAvailable = false
        }


        val booking = Booking(
            bookingId = bookingID,
            user = user, 
            flight = flight, 
            seatsBooked = requestedSeats, 
            payment = payment
        )
        bookings.add(booking)
        return booking 
    }

    fun cancelBooking(booking: Booking) {
        for(seat in booking.seatsBooked) {
            seat.isAvailable = true
        }
    }

    // return a list of all seats that are still available 
    fun getAvailableSeats(flight: Flight): List<Seat> {
        val availableSeats = mutableListOf<Seat>()

        for(seat in flight.seats) {
            if (seat.isAvailable) {
                availableSeats.add(seat)
            }
        }
        return availableSeats
    }

    fun getBookingDetails(bookingID: String): Booking? {
        for (booking in bookings) {
            if (booking.bookingId == bookingID) {
                return booking
            }
        }
        return null
    }
}