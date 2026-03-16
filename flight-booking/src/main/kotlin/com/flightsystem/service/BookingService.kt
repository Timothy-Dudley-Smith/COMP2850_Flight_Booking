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

    // return a booking that matches the provided booking Id
    fun getBookingDetails(bookingID: String): Booking? {
        for (booking in bookings) {
            if (booking.bookingId == bookingID) {
                return booking
            }
        }
        return null
    }

    // return a list of all bookings 
    fun getAllBookings(): List<Booking> {
        return bookings
    }

    // return all bookings made by a specific user
    fun getBookingsByUser(user: User): List<Booking> {
        val userBookings = mutableListOf<Booking>()

        for (booking in bookings) {
            if (booking.user == user) {
                userBookings.add(booking)
            }
        }
        return userBookings
    }

    // temporarily hold seat so it can't be booked by others
    fun holdSeat(seat: Seat): Boolean {
        if (!seat.isAvailable) {
            return false
        }
        seat.isAvailable = false 
        return true
    }

    // release a held seat 
    fun releaseSeat(seat: Seat) {
        seat.isAvailable = true
    }

    // calc total price based on seat class
    fun calculatePriceByClass(flight: Flight, seats: List<Seat>): Double {
        var totalPrice = 0.0

        for (seat in seats) {
            val seatPrice = when (seat.seatClass) {
                SeatClass.ECONOMY -> flight.price
                SeatClass.BUSINESS -> flight.price * 1.5
                SeatClass.FIRST -> flight.price * 2
            }
            totalPrice += seatPrice
        }
        return totalPrice
    }

    // update the seats assigned to an existing booking
    fun updateBookingSeats(
        booking: Boking,
        newSeats: List<Seat>
    ): Boolean {
        //check if new seats are available
        if (!validateSeats(newSeats)) {
            return false
        }

        // release old seats
        for (seat in booking.seatsBooked) {
            seat.isAvailable = true
        }

        // reserve new seats
        for (seat in newSeats) {
            seat.isAvailable = false
        }
        //update the booking with the new seats
        booking.seatBooked.clear()
        booking.seatsBooked.addAll(newSeats)
        return true
    }
}