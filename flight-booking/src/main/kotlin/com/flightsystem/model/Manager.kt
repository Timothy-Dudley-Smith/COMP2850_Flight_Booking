package.com.flightsystem.model

class Manager(
    userId: String,
    name: String,
    email: String,
    passwordHash: String
) : User(userId, name, email, passwordHash) {

    fun viewBookings(bookings: List<Booking>) {
        println("Bookings as requested by the manager: ")
        bookings.forEach {println(it)}

    }

    fun viewRevenueReport(totalRevenue: Double) {
        println("Total Revenue: $totalRevenue")
    }

    fun addFlight(flight: Flight, flights: MutableList<Flight>) {
        flights.add(flight)
        println("Flight added: ${flight.flightID}")
    }

    fun removeFlight(flight: Flight, flights: MutableList<Flight>) {
        flights.remove(flight)
        println("Flight removed: ${flight.flightID}")
    }

    fun updateFlightPrice(flight: Flight, newPrice: Double) {
        flight.price = newPrice
        println("Flight price updated to $newPrice")
    }

    fun cancelBooking(booking: Booking, bookings: MutableList<Booking>) {
        bookings.remove(booking)
        println("Booking cancelled: ${booking.bookingID}")
    }



}