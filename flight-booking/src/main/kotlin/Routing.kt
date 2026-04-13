package com.example.com

import com.flightsystem.model.Airports
import com.flightsystem.model.Flights
import com.flightsystem.model.CheckoutRequest
import com.flightsystem.model.PaymentRequest
import com.flightsystem.service.AuthenticationService
import com.flightsystem.service.CheckoutService
import com.flightsystem.service.LoyaltyService
import com.flightsystem.service.PaymentService
import com.flightsystem.service.PriceHoldService
import com.flightsystem.model.Manager 
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.routing.post


// imports the flight info
import io.ktor.http.*
import kotlinx.serialization.Serializable

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import io.ktor.server.http.content.*
//import org.h2.api.H2Type.row
import org.jetbrains.exposed.sql.transactions.transaction

import java.io.File
import java.time.LocalDate

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
    val dateOfBirth: String?
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String
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

fun Application.configureRouting() {
    routing {

        staticResources("/", "static/user/home")
        staticResources("/log_in", "static/user/log_in")
        staticResources("/home", "static/user/home")
        staticResources("/manager/flight_view", "static/manager/flight_view")
        staticResources("/manager/home", "static/manager/home" )
        staticResources("/manager/support", "static/manager/support")
        staticResources("/manager/edit_bookings", "static/manager/edit_bookings")
        staticResources("/manager/bookings", "static/manager/bookings")

        get("/book") {
            call.respondFile(
                File("src/main/resources/static/user/book/book.html")
            )
        }

        get("/booking-personal") {
            call.respondFile(
                File("src/main/resources/static/user/book/booking-personal.html")
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
        /*
        get("/api/flights") {
            val flights = transaction {
                Flights.selectAll().map { row ->
                    mapOf(
                        "flightId" to row[Flights.flightId],
                        "departureAirport" to row[Flights.departureAirport],
                        "arrivalAirport" to row[Flights.arrivalAirport],
                        "departureTime" to row[Flights.departureTime],
                        "arrivalTime" to row[Flights.arrivalTime],
                        "price" to row[Flights.price]
                    )
                }
            }
            call.respond(flights)
        }

        val bookingService = BookingService()

        post("/api/bookings") {
            val request = call.receive<CreateBookingRequest>()

            try {
                val booking = bookingService.createBooking(
                    request.userId,
                    request.flightId,
                    request.seatNumbers
                )
                call.respond(HttpStatusCode.OK, booking)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Error")
            }
        }
*/


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

        get ("/api/users")  {
            val authservice = AuthenticationService()
            val users = authservice.getAllUsers()
            call.respond(HttpStatusCode.OK, users)
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
            val authenticationService = AuthenticationService()
            val result = authenticationService.login(request.email, request.password)

            if (result.isSuccess) {
                val user = result.getOrThrow()

                call.respond(
                    HttpStatusCode.OK,
                    LoginResponse(
                        userId = user.userId,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        role = if (user is Manager) "MANAGER" else "USER"
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

            val firstName = request.firstName
            val lastName = request.lastName
            val email = request.email
            val password = request.password
            val dateOfBirth = request.dateOfBirth.toString()


            val authenticationService = AuthenticationService()

            val result = authenticationService.register(firstName, lastName, dateOfBirth, email, password)

            if (result.isSuccess) {
                call.respond(HttpStatusCode.OK, "Account registered successfully.")
            }
        }

    }

}

//test comment