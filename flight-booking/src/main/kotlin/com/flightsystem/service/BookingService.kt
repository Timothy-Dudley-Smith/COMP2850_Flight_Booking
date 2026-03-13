package com.flightsystem.service 

import com.flightsystem.model.*

class BookingService {

    fun calculateTotalPrice(flight: Flight, seats: List<Seat>): Double {
        return flight.price * seats.size
    }

    fun validateSeats(requestedSeats: List<Seat>): Boolean {
        for (seat in requestedSeats) {
            if(!seat.isAvailable) {
                return false
            }
        }
        return true    
    }

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


        return Booking(
            bookingId = bookingID,
            user = user, 
            flight = flight, 
            seatsBooked = requestedSeats, 
            payment = payment
        )
    }

    fun cancelBooking(booking: Booking) {
        for(seat in booking.seatsBooked) {
            seat.isAvailable = true
        }
    }
}