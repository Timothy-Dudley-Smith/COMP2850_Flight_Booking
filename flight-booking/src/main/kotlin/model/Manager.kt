package com.flightsystem.model

class Manager(
    userId: String,
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

    fun viewBookings(bookings: List<Booking>) {
        println("Bookings as requested by the manager: ")
        bookings.forEach {println(it)}

    }

    fun viewRevenueReport(totalRevenue: Double) {
        println("Total Revenue: $${"%.2f".format(totalRevenue)}")
    }

    fun addFlight(flight: Flight, flights: MutableList<Flight>) {
        flights.add(flight)
        println("Flight added: ${flight.flightId}")
    }

    fun removeFlight(flight: Flight, flights: MutableList<Flight>) {
        if (flights.remove(flight)) {
            println("Flight removed: ${flight.flightId}")

        } else {
            println("Flight not found: ${flight.flightId}")
        }


    }

    fun updateFlightPrice(flight: Flight, newPrice: Double) {
        require(newPrice > 0) {"Price must be greater than 0"}
        flight.price = newPrice
        println("Flight ${flight.flightId} price updated to $${"%.2f".format(newPrice)}")

    }

    fun cancelBooking(booking: Booking, user: User, bookings: MutableList<Booking>) {
        booking.cancel()
        user.removeBooking(booking)
        bookings.remove(booking)
        println("Booking cancelled: ${booking.bookingId}")
    }

    fun generateReport(bookings: List<Booking>): String {
        val total = bookings.size
        val confirmed = bookings.count {it.status == BookingStatus.CONFIRMED}
        val cancelled = bookings.count {it.status == BookingStatus.CANCELLED}

        val revenue = bookings
            .filter {it.status == BookingStatus.CONFIRMED}
            .sumOf {it.totalPrice}

        return """
            --- Manager Report ---
            Total bookings:     $total
            Confirmed:      $confirmed
            Cancelled:      $cancelled
            Total Revenue: $${"%.2f".format(revenue)}

            """.trimIndent()          
    }



}