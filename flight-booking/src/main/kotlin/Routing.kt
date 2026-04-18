package com.example.com

import com.flightsystem.model.Airport
import com.flightsystem.model.Airports
import com.flightsystem.model.Flights
import com.flightsystem.model.CheckoutRequest
import com.flightsystem.model.Manager
import com.flightsystem.model.PaymentRequest
import com.flightsystem.service.AuthenticationService
import com.flightsystem.service.CheckoutService
import com.flightsystem.service.LoyaltyService
import com.flightsystem.service.PaymentService
import com.flightsystem.service.PriceHoldService


import io.ktor.server.request.receive
import io.ktor.server.routing.post




import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.time
import com.flightsystem.service.PassengerService
import com.flightsystem.model.SavePassengersRequest


import io.ktor.server.request.receive
import io.ktor.server.routing.post
import com.flightsystem.service.BookingService



// imports the flight info
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

import io.ktor.server.application.*
import io.ktor.server.pebble.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.loader.ClasspathLoader
import org.jetbrains.exposed.sql.*
import io.ktor.http.*
import io.ktor.server.http.content.*
import org.h2.api.H2Type.row
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
//import org.h2.api.H2Type.row
import org.jetbrains.exposed.sql.transactions.transaction

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class FlightResponse(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val price: Double,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double
)

@Serializable
data class UpcomingFlightData(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val price: Double,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double
)

@Serializable
data class InsertFlightData(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double,
    val price: Double
)

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val dateOfBirth: String
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String
)



@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val sessionId: String
)

@Serializable
data class SessionCheckResponse(
    val valid: Boolean,
    val userId: Int? = null,
    val email: String? = null,
    val role: String? = null
)

@Serializable
data class ErrorResponse(
    val error: String
)


@Serializable
data class CreateBookingRequest(
    val userId: Int,
    val flightId: String,
    val seatNumbers: List<String>
)

@Serializable
data class CreateHoldRequest(
    val userId: Int,
    val flightId: String,
    val seatNumbers: List<String>
)

@Serializable
data class CreateHoldResponse(
    val holdId: Int,
    val userId: Int,
    val flightId: String,
    val seatNumbers: List<String>,
    val totalPrice: Double,
    val expiryTime: String
)

@Serializable
data class BookingLookupResponse(
    val bookingId: Int,
    val flightId: String,
    val seats: List<String>,
    val passengers: List<String>
)

fun Application.configureRouting() {
    val authenticationService = AuthenticationService()

    routing {

        staticResources("/styles", "static/user/home/styles")
        staticResources("/scripts", "static/user/home/scripts")

        staticResources("/log_in/styles", "static/user/log_in/styles")
        staticResources("/log_in/scripts", "static/user/log_in/scripts")

        staticResources("/manager", "static/manager")

        get("/") {
            call.respondFile(File("src/main/resources/static/user/home/index.html"))
        }

        get("/log_in") {
            call.respondFile(File("src/main/resources/static/user/log_in/index.html"))
        }

        get("/log_in/register.html") {
            call.respondFile(File("src/main/resources/static/user/log_in/register.html"))
        }
        val passengerService = PassengerService()

        staticResources("/", "static/user/home")
        staticResources("/log_in", "static/user/log_in")
        staticResources("/home", "static/user/home")
        staticResources("/images", "static/Images")
        staticResources("/manager/flight_view", "static/manager/flight_view")
        staticResources("/manager/home", "static/manager/home" )
        staticResources("/manager/support", "static/manager/support")
        staticResources("/manager/edit_bookings", "static/manager/edit_bookings")
        staticResources("/manager/bookings", "static/manager/bookings")

        get("/book") {
            call.respondFile(File("src/main/resources/static/user/book/book.html"))
        }

        get("/booking-personal") {
            call.respondFile(File("src/main/resources/static/user/book/booking-personal.html"))
        }

        get("/seatmap") {
            call.respondFile(
                File("src/main/resources/static/user/book/seatmap.html")
            )
        }




        get("/api/airports") {                          //get airport data for the drop-down search menu
            val airportData = transaction {
                Airports.selectAll().orderBy(Airports.country).map { row ->
                    mapOf(
                        "code" to row[Airports.code],
                        "name" to row[Airports.name],
                        "country" to row[Airports.country],
                        "city" to row[Airports.city],
                    )
                }
            }
            call.respond(airportData)
        }


        get("/api/flights") {
            val from = call.request.queryParameters["from"]
            val to   = call.request.queryParameters["to"]
            val date = call.request.queryParameters["date"]
            val passengers = call.request.queryParameters["passengers"]

            //read user input from the URL so API can filter flights

            val flightData = transaction {
                //TODO:
                //Remove requirement for date in search

                Flights.selectAll().mapNotNull { row ->


                    val departure = row[Flights.departureAirport]
                    val arrival = row[Flights.arrivalAirport]
                    val flightDate = row[Flights.date]





                    //pull data from the database row into simple variable for comparison

                    var match = true
                    //boolean val used to check if results meet filters or not

                    if (from != null) {
                        if (departure != from) {
                            match = false
                        }
                    }
                    // if the user inputted a departure airport remove results with different departure airport

                    if (to != null) {
                        if (arrival != to) {
                            match = false
                        }
                    }
                    // if the user inputted an arrival airport remove results with different arrival airports


                    if (date != "" ) {
                        if (flightDate != date){
                            match = false
                        }
                    }
                    // if the user inputted a date remove results with a different date

                    if (match == true) {
                        // only include flights that match everything

                        FlightResponse(
                            row[Flights.flightId],
                            row[Flights.departureAirport],
                            row[Flights.arrivalAirport],
                            row[Flights.price],
                            row[Flights.date],
                            row[Flights.departureTime],
                            row[Flights.arrivalTime],
                            row[Flights.length]
                        )
                    }
                    else {
                        null
                        //lables flight as non matching (reject)
                    }

                }
                // removes all the rejected flights
            }
            call.respond(flightData)
        }

        // seat routing
        get("/api/seats") {
            val flightId = call.request.queryParameters["flightId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "flightId required")

            val bookingService = BookingService()
            val seats = bookingService.getAvailableSeats(flightId)
            call.respond(seats)
        }

        get ("/api/users")  {
            val users = authenticationService.getAllUsers()
            val authenticationService = AuthenticationService()
            call.respond(HttpStatusCode.OK, users)
        }

        post("/api/passengers") {
            val request = call.receive<SavePassengersRequest>()

            val savedPassengers = passengerService.addPassengersToBooking(
                request.bookingId,
                request.passengers
            )

            call.respond(HttpStatusCode.Created, savedPassengers)
        }

        get("/api/manager/flights") {
            //TODO:
            //Add manager only access - requires manager log-in key.
            val upcomingFlightData = transaction {
                Flights.selectAll().where { Flights.date greaterEq LocalDate.now().toString() }
                    .orderBy(Flights.date to SortOrder.ASC, Flights.departureTime to SortOrder.ASC).map { row ->
                        UpcomingFlightData (
                            flightId = row[Flights.flightId],
                            departureAirport = row[Flights.departureAirport],
                            arrivalAirport = row[Flights.arrivalAirport],
                            date = row[Flights.date],
                            departureTime = row[Flights.departureTime],
                            arrivalTime = row[Flights.arrivalTime],
                            price = row[Flights.price],
                            length = row[Flights.length]

                            //TODO:
                            //Add quantity of tickets sold / still available
                        )
                    }
            }
            call.respond(upcomingFlightData)
        }

        get("/manager/flight_view") {
            call.respondFile(File("src/main/resources/static/manager/flight_view/flight_view.html"))

            //TODO:
            //Add ability to see historic flights
        }

        post("/api/manager/flight_view") {
            val request = call.receive<InsertFlightData>()

            //TODO:
            //validate flights attempted to be inserted


            transaction {
                Flights.insert {
                    it[flightId] = request.flightId
                    it[departureAirport] = request.departureAirport
                    it[arrivalAirport] = request.arrivalAirport
                    it[date] = request.date
                    it[departureTime] = request.departureTime
                    it[arrivalTime] = request.arrivalTime
                    it[length] = request.length
                    it[price] = request.price
                }
            }
            call.respond(HttpStatusCode.Created)
        }

        get("/manager") {
            call.respondFile(File("src/main/resources/static/manager/home/manager_home.html"))
        }

        post("/checkout") {
            val request = call.receive<CheckoutRequest>()

            val checkoutService = CheckoutService(
                priceHoldService = PriceHoldService(),
                paymentService = PaymentService(),
                loyaltyService = LoyaltyService()
            )

            val paymentRequest = PaymentRequest(
                cardholderName = request.cardholderName,
                cardNumber = request.cardNumber,
                expiryMonth = request.expiryMonth,
                expiryYear = request.expiryYear,
                cvv = request.cvv,
                billingAddress = request.billingAddress
            )

            val response = checkoutService.checkout(
                holdId = request.holdId,
                request = paymentRequest,
                pointsToRedeem = request.pointsToRedeem
            )

            if (response.success) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        post("/api/auth/login") {
            val request = call.receive<LoginRequest>()
            //val authenticationService = AuthenticationService()
            val result = authenticationService.login(request.email, request.password)

            if (result.isSuccess) {
                val user = result.getOrThrow()
                val sessionId = authenticationService.createSession(user)

                call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                        success = true,
                        userId = user.userId,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        role = if (user is Manager) "MANAGER" else "USER",
                        sessionId = sessionId
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password")
                )
            }
        }

        // browser bar testing
        get("/api/auth/login-test") {
            val email = call.request.queryParameters["email"]
            val password = call.request.queryParameters["password"]

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("missing email or password")
                )
                return@get
            }
            val result = authenticationService.login(email, password)

            if (result.isSuccess) {
                val user = result.getOrThrow()
                val sessionId = authenticationService.createSession(user)

                call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                        success = true,
                        userId = user.userId,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        role = if (user is Manager) "MANAGER" else "USER",
                        sessionId = sessionId
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("invalid emaol or password")
                )
            }
        }

        // session check route
        get("/api/auth/session") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing sessionId")
                )
                return@get
            }
            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    SessionCheckResponse(valid = false)
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    SessionCheckResponse(
                        valid = true,
                        userId = user.userId,
                        email = user.email,
                        role = if (user is Manager) "MANAGER" else "USER"
                    )
                )
            }
        }

        // logout route
        post("/api/auth/logout") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing sessionId")
                )
                return@post
            }
            authenticationService.logout(sessionId)
            call.respond(HttpStatusCode.OK, RegisterResponse(
                success = true,
                message = "Successfully logged out"
            ))
        }


        post("/api/auth/login") {
            val request = call.receive<LoginRequest>()
            val authenticationService = AuthenticationService()

            val result = authenticationService.login(request.email, request.password)

            if (result.isSuccess) {
                val user = result.getOrThrow()

                val sessionId = authenticationService.createSession(user)

                call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                        success = true,
                        userId = user.userId,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        role = if (user is Manager) "MANAGER" else "USER",
                        sessionId = sessionId
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password")
                )
            }
        }

        post("/api/auth/register") {
            val request = call.receive<RegisterRequest>()

            val result = authenticationService.register(
                request.firstName,
                request.lastName,
                request.dateOfBirth,
                request.email,
                request.password
            )

            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    RegisterResponse(
                        success = true,
                        message = "Account registered successfully."
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        result.exceptionOrNull()?.message ?: "Registration failed"
                    )
                )
            }
        }


        get("/loyaltypage") {
            call.respondFile(File("src/main/resources/static/user/loyalty/loyaltypage.html"))
        }

        get("/checkout") {
            call.respondFile(File("src/main/resources/static/user/payment/payment.html"))
        }

        get("/confirmation") {
            call.respondFile(File("src/main/resources/static/user/payment/confirmation.html"))
        }

        post("/api/holds") {
            try {

                val request = call.receive<CreateHoldRequest>()
                val priceHoldService = PriceHoldService()

                var userId = request.userId
                var flightId = request.flightId
                val seatNumbers = request.seatNumbers

                val hold = priceHoldService.createHold(userId, flightId, seatNumbers)

                val holdId = hold.holdId
                userId = hold.userId
                flightId = hold.flightId
                val expiryTime = hold.expiryTime
                val totalPrice = hold.totalPrice

                val holdResponse = CreateHoldResponse(holdId, userId, flightId, seatNumbers, totalPrice, expiryTime)
                call.respond(HttpStatusCode.Created, holdResponse)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error while creating hold")
            }
        }
        get("/api/bookings/lookup") {

            // get  parameters from the request url
            val bookingIdParam = call.request.queryParameters["bookingId"]
            val lastName = call.request.queryParameters["lastName"]?.trim()

            // convert booking id to int, if fails it will be null
            val bookingId = bookingIdParam?.toIntOrNull()

            // validate both fields are present
            if (bookingId == null || lastName.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing bookingId or lastName"))
                return@get
            }

            // create services
            val bookingService = BookingService()
            val passengerService = PassengerService()

            // look up the booking by id
            val details = bookingService.getBookingDetails(bookingId)

            // if no booking found return 404
            if (details == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Booking not found"))
                return@get
            }

            // get all passengers on this booking
            val passengers = passengerService.getPassengersByBooking(bookingId)

            // check if any passenger last name matches what was entered (not case sensitive)
            var lastNameMatches = false
            for (passenger in passengers) {
                if (passenger.lastName.equals(lastName, ignoreCase = true)) {
                    lastNameMatches = true
                    break
                }
            }

            // if no name match, return 404 (same message to avoid exposing booking exists)
            if (lastNameMatches == false) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Booking not found"))
                return@get
            }

            // build list of passenger full names
            val passengerNames = mutableListOf<String>()
            for (passenger in passengers) {
                passengerNames.add("${passenger.firstName} ${passenger.lastName}")
            }

            // return the booking details
            call.respond(
                HttpStatusCode.OK,
                BookingLookupResponse(
                    bookingId  = details.booking.bookingId,
                    flightId   = details.booking.flightId,
                    seats      = details.seats,
                    passengers = passengerNames
                )
            )
        }

// serves the view booking html page
        get("/view-booking") {
            call.respondFile(File("src/main/resources/static/user/booking/view-booking.html"))
        }
    }

}
