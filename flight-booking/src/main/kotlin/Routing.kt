package com.example.com

import com.example.com.service.ManagerAnalyticsService
import com.flightsystem.AppEnv
import com.flightsystem.flightservice.AuthenticationService
import com.flightsystem.flightservice.BookingService
import com.flightsystem.flightservice.CheckoutService
import com.flightsystem.flightservice.EmailService
import com.flightsystem.flightservice.LoyaltyService
import com.flightsystem.flightservice.PassengerService
import com.flightsystem.flightservice.PaymentService
import com.flightsystem.flightservice.PriceHoldService
import com.flightsystem.flightservice.PromoCodeService
import com.flightsystem.flightservice.TicketService
import com.flightsystem.model.AccountStatus
import com.flightsystem.model.Airports
import com.flightsystem.model.BookingDetails
import com.flightsystem.model.Bookings
import com.flightsystem.model.CheckoutRequest
import com.flightsystem.model.Flights
import com.flightsystem.model.Layovers
import com.flightsystem.model.LoyaltyAccounts
import com.flightsystem.model.Manager
import com.flightsystem.model.Passenger
import com.flightsystem.model.PassengerInput
import com.flightsystem.model.PaymentRequest
import com.flightsystem.model.PriceHoldSeats
import com.flightsystem.model.PriceHolds
import com.flightsystem.model.SavePassengersRequest
import com.flightsystem.model.Seats
import com.flightsystem.model.Users
import createEmptySeatMaps
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import model.CreateTicketRequest
import model.ManagerSentEmailResponse
import model.ManagerSentEmails
import model.UpdateTicketRequest
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class UpdateUserRequest(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val email: String,
)

@Serializable
data class FlightResponse(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val price: Double,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double,
)

@Serializable
data class ManagerBookingDetailResponse(
    val booking: BookingDetails?,
    val passengers: List<Passenger>,
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
    val length: Double,
)

@Serializable
data class HistoricFlightData(
    val flightId: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val price: Double,
    val date: String,
    val departureTime: String,
    val arrivalTime: String,
    val length: Double,
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
    val price: Double,
)

@Serializable
data class InsertAirportData(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
)

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val dateOfBirth: String,
)

@Serializable
data class RegisterResponse(
    val success: Boolean,
    val message: String,
)

@Serializable
data class ApplyPromoCodeRequest(
    val code: String,
    val originalAmount: Double,
)

@Serializable
data class ApplyPromoCodeResponse(
    val success: Boolean,
    val code: String? = null,
    val originalAmount: Double,
    val discountedAmount: Double? = null,
    val message: String,
)

@Serializable
data class CreatePromoCodeRequest(
    val code: String,
    val discountType: String,
    val discountValue: Double,
)

@Serializable
data class CreatePromoCodeResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val sessionId: String,
)

@Serializable
data class SessionCheckResponse(
    val valid: Boolean,
    val userId: Int? = null,
    val email: String? = null,
    val role: String? = null,
)

@Serializable
data class ErrorResponse(
    val error: String,
)

@Serializable
data class SendManagerEmailRequest(
    val toEmail: String,
    val subject: String,
    val message: String,
)

@Serializable
data class SendManagerEmailResponse(
    val success: Boolean,
    val message: String,
)

@Serializable
data class CreateBookingRequest(
    val userId: Int,
    val flightId: String,
    val seatNumbers: List<String>,
)

@Serializable
data class CreateHoldRequest(
    val userId: Int?,
    val flightId: String,
    val seatNumbers: List<String>,
    val returnFlightId: String? = null,
    val returnSeatNumbers: List<String> = emptyList(),
)

@Serializable
data class CreateHoldResponse(
    val holdId: Int,
    val userId: Int,
    val flightId: String,
    val seatNumbers: List<String>,
    val totalPrice: Double,
    val expiryTime: String,
    val returnFlightId: String? = null,
    val returnSeatNumbers: List<String>,
)

@Serializable
data class AccountSummary(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val membershipNumber: String,
    val membershipTier: String,
    val loyaltyPoints: Int,
)

@Serializable
data class BookingLookupResponse(
    val bookingId: Int,
    val flightId: String,
    val returnFlightId: String? = null,
    val seats: List<String>,
    val passengers: List<String>,
    val cabin: String?,
    val addOns: String?,
)

@Serializable
data class UpdateSeatsRequest(
    val seats: List<String>,
)

@Serializable
data class ManagerAnalyticsResponse(
    val totalBookings: Int,
    val upcomingFlights: Int,
    val openTickets: Int,
    val mostPopularRoute: RouteBookingCount,
    val bookingsPerHour: List<HourlyBookingCount>,
    val bookingsPerFlight: List<FlightBookingCount>,
    val popularRoutes: List<RouteBookingCount>,
    val bookingsPerRoute: List<RouteBookingCount>,
)

@Serializable
data class HourlyBookingCount(
    val hour: Int,
    val count: Int,
)

@Serializable
data class FlightBookingCount(
    val flightId: String,
    val date: String,
    val departureAirport: String,
    val arrivalAirport: String,
    var bookingCount: Int,
)

@Serializable
data class RouteBookingCount(
    val departureAirport: String,
    val arrivalAirport: String,
    val bookingCount: Int,
)

@Serializable
data class Route(
    val departureAirport: String,
    val arrivalAirport: String,
)

@Serializable
data class OtpWaitingresponse(
    val success: Boolean,
    val otpRequired: Boolean,
)

@Serializable
data class OtpVerifyRequest(
    val email: String,
    val otp: String,
)

@Serializable
data class ManagerAccountChanges(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val status: AccountStatus,
    val loyaltyPoints: Int,
)

@Serializable
data class AddPointsRequest(
    val points: Int,
)

fun Application.configureRouting() {
    val authenticationService = AuthenticationService()

    val emailService =
        EmailService(
            smtpHost = "smtp.gmail.com",
            smtpPort = "587",
            smtpUsername = AppEnv.require("SMTP_USERNAME"),
            smtpPassword = AppEnv.require("SMTP_PASSWORD"),
            fromEmail = AppEnv.require("SMTP_USERNAME"),
        )

    val ticketService = TicketService(emailService)
    val promoCodeService = PromoCodeService()

    routing {
        /**
         *frontend resources for each page on the webapp, the files for these can be found in src/main/recources
         */
        staticResources("/styles", "static/user/home/styles")
        staticResources("/scripts", "static/user/home/scripts")
        staticResources("/log_in/styles", "static/user/log_in/styles")
        staticResources("/log_in/scripts", "static/user/log_in/scripts")
        staticResources("/manager", "static/manager")
        staticResources("/manage-account/styles", "static/user/manage-account/styles")
        staticResources("/manage-account/scripts", "static/user/manage-account/scripts")
        staticResources("/support/styles", "static/user/support/styles")
        staticResources("/support/scripts", "static/user/support/scripts")
        staticResources("/shared", "static/shared")
        staticResources("/", "static/user/home")
        staticResources("/log_in", "static/user/log_in")
        staticResources("/home", "static/user/home")
        staticResources("/images", "static/Images")
        staticResources("/loyalty", "static/user/loyalty")
        staticResources("/manager/flight_view", "static/manager/flight_view")
        staticResources("/manager/home", "static/manager/home")
        staticResources("/manager/support", "static/manager/support")
        staticResources("/manager/edit_bookings", "static/manager/edit_bookings")
        staticResources("/manager/bookings", "static/manager/bookings")

        get("/lounges") {
            call.respondFile(File("src/main/resources/static/user/home/lounges.html"))
        }

        get("/dubai") {
            call.respondFile(File("src/main/resources/static/user/home/dubai.html"))
        }

        get("/refunds") {
            call.respondFile(File("src/main/resources/static/user/home/refunds.html"))
        }

        get("/entertainment") {
            call.respondFile(File("src/main/resources/static/user/home/entertainment.html"))
        }

        /**
         Homepage, HTML file with relevant stylesheet and scripts that begins the paths to every feature on the website
         */
        get("/") {
            call.respondFile(File("src/main/resources/static/user/home/index.html"))
        }

        get("/log_in") {
            call.respondFile(File("src/main/resources/static/user/log_in/index.html"))
        }

        get("/log_in/register.html") {
            call.respondFile(File("src/main/resources/static/user/log_in/register.html"))
        }

        get("/confirmation.html") {
            call.respondFile(File("src/main/resources/static/user/payment/confirmation.html"))
        }

        val passengerService = PassengerService()
        val bookingService = BookingService()

        get("/manage") {
            call.respondFile(File("src/main/resources/static/user/manage-account/index.html"))
        }

        get("/book") {
            call.respondFile(File("src/main/resources/static/user/book/book.html"))
        }

        get("/booking-personal") {
            call.respondFile(File("src/main/resources/static/user/book/booking-personal.html"))
        }

        get("/seatmap") {
            call.respondFile(
                File("src/main/resources/static/user/book/seatmap.html"),
            )
        }

        get("/payment") {
            call.respondFile(File("src/main/resources/static/user/payment/payment.html"))
        }

        get("/support") {
            call.respondFile(File("src/main/resources/static/user/support/support.html"))
        }

        /**
         * Selects airport rows from the Airports table and maps it to airportData
         * This data is received by the frontend and used for the airport selection dropdown
         * menus on the home page
         */
        get("/api/airports") {
            // get airport data for the drop-down search menu
            val airportData =
                transaction {
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

        get("/api/account-summary") {
            val userIdParam = call.request.queryParameters["userId"]

            if (userIdParam.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing userId"),
                )
                return@get
            }

            val userId = userIdParam.toIntOrNull()
            if (userId == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid userId"),
                )
                return@get
            }

            val user = authenticationService.findById(userId)
            if (user == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("User not found"),
                )
                return@get
            }

            val loyaltyAccount = LoyaltyService().getLoyaltyAccount(userId)
            val points = loyaltyAccount?.loyaltyPoints ?: 0

            call.respond(
                HttpStatusCode.OK,
                AccountSummary(
                    userId = user.userId,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    membershipNumber = "BA-${user.userId}",
                    membershipTier = "Member",
                    loyaltyPoints = points,
                ),
            )
        }

        get("/api/flights") {
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]
            val date = call.request.queryParameters["date"]
            val passengers = call.request.queryParameters["passengers"]

            // read user input from the URL so API can filter flights

            val flightData =
                transaction {
                    Flights.selectAll().mapNotNull { row ->

                        val departure = row[Flights.departureAirport]
                        val arrival = row[Flights.arrivalAirport]
                        val flightDate = row[Flights.date]

                        // pull data from the database row into simple variable for comparison

                        var match = true
                        // boolean val used to check if results meet filters or not

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

                        if (date != "") {
                            if (flightDate != date) {
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
                                row[Flights.length],
                            )
                        } else {
                            null
                            // lables flight as non matching (reject)
                        }
                    }
                    // removes all the rejected flights
                }
            call.respond(flightData)
        }

        // seat routing
        get("/api/seats") {
            val flightId =
                call.request.queryParameters["flightId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "flightId required")

            val bookingService = BookingService()
            val seats = bookingService.getAvailableSeats(flightId)
            call.respond(seats)
        }

        get("/api/users") {
            val users = authenticationService.getAllUsers()
            val authenticationService = AuthenticationService()
            call.respond(HttpStatusCode.OK, users)
        }

        post("/api/passengers") {
            val request = call.receive<SavePassengersRequest>()

            val savedPassengers =
                passengerService.addPassengersToBooking(
                    request.bookingId,
                    request.passengers,
                )

            call.respond(HttpStatusCode.Created, savedPassengers)
        }

        route("/api/tickets") {
            post {
                val request = call.receive<CreateTicketRequest>()
                val createdTicket = ticketService.createTicket(request)
                call.respond(HttpStatusCode.Created, createdTicket)
            }

            get {
                val tickets = ticketService.getAllTickets()
                call.respond(HttpStatusCode.OK, tickets)
            }

            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ticket ID")
                    return@put
                }

                val request = call.receive<UpdateTicketRequest>()
                val updatedTicket = ticketService.updateTicket(id, request)

                if (updatedTicket == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Ticket update failed. Ticket may not exist, or booking change could not be processed",
                    )
                } else {
                    call.respond(HttpStatusCode.OK, updatedTicket)
                }
            }

            get("/{id}/history") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ticket ID")
                    return@get
                }
                val history = ticketService.getTicketHistory(id)
                call.respond(HttpStatusCode.OK, history)
            }

            put("/{id}/archive") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid ticket ID")
                    return@put
                }
                val archived = ticketService.archiveTicket(id)
                if (archived) {
                    call.respond(HttpStatusCode.OK, "Ticket archived")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Ticket not found")
                }
            }
        }
        /**
         * selects flight rows from the Flights table that are today or after
         * stores them in upcomingFlightData which is then sent to the frontend
         */
        get("/api/manager/flights") {
            val upcomingFlightData =
                transaction {
                    Flights
                        .selectAll()
                        .where { Flights.date greaterEq LocalDate.now().toString() }
                        .orderBy(Flights.date to SortOrder.ASC, Flights.departureTime to SortOrder.ASC)
                        .map { row ->
                            UpcomingFlightData(
                                flightId = row[Flights.flightId],
                                departureAirport = row[Flights.departureAirport],
                                arrivalAirport = row[Flights.arrivalAirport],
                                date = row[Flights.date],
                                departureTime = row[Flights.departureTime],
                                arrivalTime = row[Flights.arrivalTime],
                                price = row[Flights.price],
                                length = row[Flights.length],
                            )
                        }
                }
            call.respond(upcomingFlightData)
        }

        get("/manager/flight_view") {
            call.respondFile(File("src/main/resources/static/manager/flight_view/flight_view.html"))
        }

        /**
         * receives data from the insert flight form on flight-view and inserts it into the Flights table
         */
        post("/api/manager/flight_view") {
            val request = call.receive<InsertFlightData>()

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
            transaction {
                val flights = Flights.selectAll().map { it[Flights.flightId] }
                createEmptySeatMaps(flights)
            }
            call.respond(HttpStatusCode.Created)
        }

        /**
         * Receives data from the insert flight form on flight-view and inserts it into the Airports table
         */
        post("/api/manager/airports") {
            val sessionId: String
            val sessionIdFromUrl = call.request.queryParameters["sessionId"]
            // get user session id
            if (sessionIdFromUrl == null) {
                sessionId = ""
                // if session id is empty ie not logged in then sessionid = ""
            } else {
                sessionId = sessionIdFromUrl
            }
            // else get there real sessionid
            val isManager = authenticationService.isManagerSession(sessionId)
            // checks if the sessionid is a manager sessionid
            if (!isManager) {
                call.respondRedirect("/log_in")
                return@post // exit this handler, don't run the code below
            }

            val request = call.receive<InsertAirportData>()

            transaction {
                Airports.insert {
                    it[code] = request.code
                    it[name] = request.name
                    it[city] = request.city
                    it[country] = request.country
                }
            }
            call.respond(HttpStatusCode.Created)
        }

        /**
         * deletes the corresponding flight from the database when the suer clicks delete
         * on the flight vew page
         */
        delete("/api/manager/flights/{flightId}") {
            val flightId = call.parameters["flightId"]

            if (flightId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "flightId required")
                return@delete
            }
            val emptyBookingData =
                transaction {
                    Bookings.selectAll().where { Bookings.flightId eq flightId }.count() > 0
                }

            if (emptyBookingData) {
                call.respond(HttpStatusCode.BadRequest, "flight has existing bookings.")
            } else {
                transaction {
                    PriceHoldSeats.deleteWhere { PriceHoldSeats.flightId eq flightId }
                    PriceHolds.deleteWhere { PriceHolds.flightId eq flightId }
                    Seats.deleteWhere { Seats.flightId eq flightId }
                    Layovers.deleteWhere { Layovers.flightId eq flightId }
                    Flights.deleteWhere { Flights.flightId eq flightId }
                }

                call.respond(HttpStatusCode.OK, "Flights successfully deleted.")
            }
        }

        get("/manager") {
            call.respondFile(File("src/main/resources/static/manager/home/manager_home.html"))
        }

        get("/manager/support") {
            call.respondFile(File("src/main/resources/static/manager/support/support.html"))
        }

        post("/checkout") {
            val request = call.receive<CheckoutRequest>()

            val checkoutService =
                CheckoutService(
                    priceHoldService = PriceHoldService(),
                    paymentService = PaymentService(),
                    loyaltyService = LoyaltyService(),
                    promoCodeService = PromoCodeService(),
                )

            val paymentRequest =
                PaymentRequest(
                    cardholderName = request.cardholderName,
                    cardNumber = request.cardNumber,
                    expiryMonth = request.expiryMonth,
                    expiryYear = request.expiryYear,
                    cvv = request.cvv,
                    billingAddress = request.billingAddress,
                )

            val response =
                checkoutService.checkout(
                    holdId = request.holdId,
                    returnHoldId = request.returnHoldId,
                    request = paymentRequest,
                    pointsToRedeem = request.pointsToRedeem,
                    promoCode = request.promoCode,
                    guestEmail = request.guestEmail,
                    cabin = request.cabin,
                    addOns = request.addOns,
                    finalAmountFromFrontEnd = request.finalAmountFromFrontEnd,
                )

            if (response.success) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        post("/api/auth/login") {
            val request = call.receive<LoginRequest>()
            // val authenticationService = AuthenticationService()
            val result = authenticationService.login(request.email, request.password)

            if (result.isSuccess) {
                val user = result.getOrThrow()
                val otp = authenticationService.createOtpChallenge(user)
                // val sessionId = authenticationService.createSession(user)

                if (user.email == "manager@astraeus.com") {
                    emailService.sendEmail(
                        toEmail = "bhamani01@gmail.com, musaddakali14@gmail.com , mikaeel4760@gmail.com , tods2006@gmail.com",
                        subject = "MANAGER ADMIN ACCESS REQUESTED",
                        body = "Your one-time login code is: $otp\n\nThis code expires in 5 minutes.",
                    )
                } else {
                    emailService.sendEmail(
                        toEmail = user.email,
                        subject = "Your Astraeus Airways login code",
                        body = "Your one-time login code is: $otp\n\nThis code expires in 5 minutes. Do not share it.",
                    )
                }

                call.respond(HttpStatusCode.OK, OtpWaitingresponse(success = true, otpRequired = true))
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid email or password"),
                )
            }
        }

        post("/api/auth/verify-otp") {
            val request = call.receive<OtpVerifyRequest>()
            val result = authenticationService.verifyOtp(request.email, request.otp)

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
                        sessionId = sessionId,
                    ),
                )
            } else {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(result.exceptionOrNull()?.message ?: "Invalid otp"))
            }
        }

        // browser bar testing
        get("/api/auth/login-test") {
            val email = call.request.queryParameters["email"]
            val password = call.request.queryParameters["password"]

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("missing email or password"),
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
                        sessionId = sessionId,
                    ),
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("invalid emaol or password"),
                )
            }
        }

        // session check route
        get("/api/auth/session") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing sessionId"),
                )
                return@get
            }
            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    SessionCheckResponse(valid = false),
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    SessionCheckResponse(
                        valid = true,
                        userId = user.userId,
                        email = user.email,
                        role = if (user is Manager) "MANAGER" else "USER",
                    ),
                )
            }
        }

        // logout route
        post("/api/auth/logout") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Missing sessionId"),
                )
                return@post
            }
            authenticationService.logout(sessionId)
            call.respond(
                HttpStatusCode.OK,
                RegisterResponse(
                    success = true,
                    message = "Successfully logged out",
                ),
            )
        }

        post("/api/auth/register") {
            val request = call.receive<RegisterRequest>()

            val result =
                authenticationService.register(
                    request.firstName,
                    request.lastName,
                    request.dateOfBirth,
                    request.email,
                    request.password,
                )

            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    RegisterResponse(
                        success = true,
                        message = "Account registered successfully.",
                    ),
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        result.exceptionOrNull()?.message ?: "Registration failed",
                    ),
                )
            }
        }

        get("/loyaltypage") {
            call.respondFile(File("src/main/resources/static/user/loyalty/loyaltypage.html"))
        }

        get("/api/loyalty/{userId}") {
            val userId = call.parameters["userId"]?.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                return@get
            }

            val loyaltyService = LoyaltyService()
            val loyaltyAccount =
                loyaltyService.getLoyaltyAccount(userId)
                    ?: loyaltyService.createLoyaltyAccount(userId)

            call.respond(HttpStatusCode.OK, loyaltyAccount)
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

                var userId =
                    request.userId ?: transaction {
                        val guestEmail = "guest@astraeus.local"
                        val existingGuest =
                            Users
                                .selectAll()
                                .where {
                                    Users.email eq guestEmail
                                }.singleOrNull()

                        if (existingGuest != null) {
                            existingGuest[Users.userId]
                        } else {
                            val insertedGuest =
                                Users.insert {
                                    it[firstName] = "Guest"
                                    it[lastName] = "Customer"
                                    it[dateOfBirth] = "1900-01-01"
                                    it[email] = guestEmail
                                    it[passwordHash] = "guest"
                                    it[salt] = "guest"
                                    it[role] = "USER"
                                    it[status] = AccountStatus.ACTIVE
                                }

                            insertedGuest[Users.userId]
                        }
                    }
                var flightId = request.flightId
                val seatNumbers = request.seatNumbers
                val returnFlightId = request.returnFlightId
                val returnSeatNumbers = request.returnSeatNumbers
                val hold = priceHoldService.createHold(userId, flightId, seatNumbers, returnFlightId, returnSeatNumbers)
                val user =
                    transaction {
                        Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                    }

                if (user == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid user")
                    return@post
                }

                val accountStatus = user[Users.status]

                if (accountStatus != AccountStatus.ACTIVE) {
                    call.respond(HttpStatusCode.BadRequest, "Account status is inactive")
                    return@post
                }

                val holdId = hold.holdId
                userId = hold.userId
                flightId = hold.flightId
                val expiryTime = hold.expiryTime
                val totalPrice = hold.totalPrice

                val holdResponse =
                    CreateHoldResponse(
                        holdId,
                        userId,
                        flightId,
                        seatNumbers,
                        totalPrice,
                        expiryTime,
                        hold.returnFlightId,
                        returnSeatNumbers,
                    )
                call.respond(HttpStatusCode.Created, holdResponse)
            } catch (e: Exception) {
                e.printStackTrace()
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
                    bookingId = details.booking.bookingId,
                    flightId = details.booking.flightId,
                    returnFlightId = details.booking.returnFlightId,
                    seats = details.seats,
                    passengers = passengerNames,
                    cabin = details.booking.cabin,
                    addOns = details.booking.addOns,
                ),
            )
        }

        post("/api/promo/apply") {
            val request = call.receive<ApplyPromoCodeRequest>()

            val result =
                promoCodeService.applyPromoCode(
                    codeValue = request.code,
                    originalAmount = request.originalAmount,
                )

            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    ApplyPromoCodeResponse(
                        success = true,
                        code = request.code.uppercase(),
                        originalAmount = request.originalAmount,
                        discountedAmount = result.getOrNull(),
                        message = "Promo code applied successfully",
                    ),
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApplyPromoCodeResponse(
                        success = false,
                        code = request.code.uppercase(),
                        originalAmount = request.originalAmount,
                        discountedAmount = null,
                        message = result.exceptionOrNull()?.message ?: "Invalid promo code",
                    ),
                )
            }
        }

        post("/api/manager/send-email") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Missing sessionId"),
                )
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Invalid session"),
                )
                return@post
            }

            if (user !is Manager) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ErrorResponse("Only managers can send emails"),
                )
                return@post
            }

            val request = call.receive<SendManagerEmailRequest>()

            if (request.toEmail.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SendManagerEmailResponse(
                        success = false,
                        message = "Recipient email cannot be blank",
                    ),
                )
                return@post
            }

            if (request.subject.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SendManagerEmailResponse(
                        success = false,
                        message = "Subject cannot be blank",
                    ),
                )
                return@post
            }

            if (request.message.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SendManagerEmailResponse(
                        success = false,
                        message = "Message cannot be blank",
                    ),
                )
                return@post
            }

            try {
                emailService.sendEmail(
                    toEmail = request.toEmail,
                    subject = request.subject,
                    body = request.message,
                )

                val now = LocalDateTime.now().toString()

                transaction {
                    ManagerSentEmails.insert {
                        it[ManagerSentEmails.managerId] = user.userId
                        it[ManagerSentEmails.managerEmail] = user.email
                        it[ManagerSentEmails.toEmail] = request.toEmail
                        it[ManagerSentEmails.subject] = request.subject
                        it[ManagerSentEmails.message] = request.message
                        it[ManagerSentEmails.sentAt] = now
                    }
                }

                call.respond(
                    HttpStatusCode.OK,
                    SendManagerEmailResponse(
                        success = true,
                        message = "Email sent successfully",
                    ),
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SendManagerEmailResponse(
                        success = false,
                        message = e.message ?: "Failed to send email",
                    ),
                )
            }

            get("/api/manager/sent-emails") {
                val sentEmails =
                    transaction {
                        ManagerSentEmails.selectAll().map { row ->
                            ManagerSentEmailResponse(
                                emailId = row[ManagerSentEmails.emailId],
                                managerId = row[ManagerSentEmails.managerId],
                                managerEmail = row[ManagerSentEmails.managerEmail],
                                toEmail = row[ManagerSentEmails.toEmail],
                                subject = row[ManagerSentEmails.subject],
                                message = row[ManagerSentEmails.message],
                                sentAt = row[ManagerSentEmails.sentAt],
                            )
                        }
                    }
                call.respond(HttpStatusCode.OK, sentEmails)
            }
        }

        post("/api/manager/promo-codes") {
            val request = call.receive<CreatePromoCodeRequest>()

            val result =
                promoCodeService.createPromoCode(
                    codeValue = request.code,
                    discountType = request.discountType.uppercase(),
                    discountValue = request.discountValue,
                )

            if (result.isSuccess) {
                call.respond(
                    HttpStatusCode.Created,
                    CreatePromoCodeResponse(
                        success = true,
                        message = "Promo code created successfully",
                    ),
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    CreatePromoCodeResponse(
                        success = false,
                        message = result.exceptionOrNull()?.message ?: "Unable to create promo code",
                    ),
                )
            }
        }

// serves the view booking html page
        get("/view-booking") {
            call.respondFile(File("src/main/resources/static/user/booking/view-booking.html"))
        }

        get("/api/manager/bookings") {
            val bookingService = BookingService()
            val bookings = bookingService.getAllBookings()
            call.respond(HttpStatusCode.OK, bookings)
        }

        get("/manager/bookings") {
            val sessionId: String
            val sessionIdFromUrl = call.request.queryParameters["sessionId"]
            // get user session id
            if (sessionIdFromUrl == null) {
                sessionId = ""
                // if session id is empty ie not logged in then sessionid = ""
            } else {
                sessionId = sessionIdFromUrl
            }
            // else get there real sessionid
            val isManager = authenticationService.isManagerSession(sessionId)
            // checks if the sessionid is a manager sessionid
            if (isManager == false) {
                call.respondRedirect("/log_in")
                return@get // exit this handler, don't run the code below
            }
            // if not then whenever they try access manager site redirect to homepage

            call.respondFile(File("src/main/resources/static/manager/edit_bookings/edit_bookings.html"))
            // else redirect to manager site
        }

        // get booking + its passengers
        get("/api/manager/bookings/{bookingId}") {
            val bookingId =
                call.parameters["bookingId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "invalid booking id")

            val booking = bookingService.getBookingDetails(bookingId)
            val passengers = passengerService.getPassengersByBooking(bookingId)

            call.respond(ManagerBookingDetailResponse(booking = booking, passengers = passengers))
        }

        // update a passenger in a booking
        put("/api/manager/passengers/{passengerId}") {
            val passengerId =
                call.parameters["passengerId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "invalid passenger id")

            val input = call.receive<PassengerInput>()

            passengerService.updatePassenger(passengerId, input)

            call.respond(HttpStatusCode.OK)
        }

        // update seats on a booking (frees old seats, books new ones)
        put("/api/manager/bookings/{bookingId}/seats") {
            // get booking id
            val bookingId =
                call.parameters["bookingId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "invalid booking id")
            // get the new seats list from request body
            val request = call.receive<UpdateSeatsRequest>()
            val newSeats = request.seats
            // try to update the seats
            val success = bookingService.updateBookingSeats(bookingId, newSeats)

            // send back result
            if (success) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.BadRequest, "invalid seats")
            }
        }

        // Delete entire booking + free seats + delete passengers
        // cancel a booking (removes passengers, frees seats, deletes booking)
        delete("/api/manager/bookings/{bookingId}") {
            // get booking id from url
            val bookingId =
                call.parameters["bookingId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "invalid booking id")

            // remove all passengers on this booking first
            passengerService.deletePassengersByBooking(bookingId)

            // cancel the booking (frees seats + deletes it)
            val success = bookingService.cancelBooking(bookingId)

            // send back result
            if (success) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/api/debug/passengers") {
            val all = passengerService.getPassengersByBooking(161)
            call.respond(all)
        }

        // used for the manage account page so user can view their details
        get("/api/user/details") {
            // get the user id
            val userIdText = call.request.queryParameters["userId"]
            val userId = userIdText?.toIntOrNull()

            // if no valid user id was given, return an error
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid userId"))
                return@get
            }

            // search the database for a user with this id
            val userRow =
                transaction {
                    Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                }

            // if no user was found, return an error
            if (userRow == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                return@get
            }

            // pull each piece of info out of the database row
            val firstName = userRow[Users.firstName]
            val lastName = userRow[Users.lastName]
            val dateOfBirth = userRow[Users.dateOfBirth]
            val email = userRow[Users.email]

            // send the user details back as a response
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "dateOfBirth" to dateOfBirth,
                    "email" to email,
                ),
            )
        }

        get("/api/user/bookings") {
            // get userid
            val userId = call.request.queryParameters["userId"]?.toIntOrNull()

            // if nulll send error
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid userId"))
                return@get
            }

            // call the service to get the bookings for this user
            val bookings = bookingService.getBookingDetailsByUser(userId)

            // send the bookings back
            call.respond(HttpStatusCode.OK, bookings)
        }

        put("/api/user/update") {
            // read the new user details
            val request = call.receive<UpdateUserRequest>()

            // update the user in the database
            val numberOfRowsUpdated =
                transaction {
                    Users.update({ Users.userId eq request.userId }) {
                        it[Users.firstName] = request.firstName
                        it[Users.lastName] = request.lastName
                        it[Users.dateOfBirth] = request.dateOfBirth
                        it[Users.email] = request.email
                    }
                }

            // if no rows were updated, the user was not found
            if (numberOfRowsUpdated == 0) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            } else {
                // otherwise the update worked
                call.respond(HttpStatusCode.OK, mapOf("message" to "Updated successfully"))
            }
        }

        get("/addons") {
            call.respondFile(File("src/main/resources/static/user/loyalty/addons.html"))
        }

        get("/manager/analytics") {
            call.respondFile(File("src/main/resources/static/manager/analytics/analytics.html"))
        }

        get("/api/manager/analytics") {
            val analytics = ManagerAnalyticsService().getAnalytics()
            call.respond(analytics)
        }
        /**
         * Validate manager account is accessing
         * Then select all accounts in the Users table and respond with map of all user rows
         */
        get("/api/manager/users") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@get
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@get
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@get
            }

            val pointsByUserId =
                transaction {
                    LoyaltyAccounts.selectAll().associate { row -> row[LoyaltyAccounts.userId] to row[LoyaltyAccounts.loyaltyPoints] }
                }

            val managerAccountChanges =
                transaction {
                    Users.selectAll().map { row ->
                        ManagerAccountChanges(
                            userId = row[Users.userId],
                            firstName = row[Users.firstName],
                            lastName = row[Users.lastName],
                            email = row[Users.email],
                            role = row[Users.role],
                            status = row[Users.status],
                            loyaltyPoints = pointsByUserId[row[Users.userId]] ?: 0,
                        )
                    }
                }
            call.respond(HttpStatusCode.OK, managerAccountChanges)
        }
        /**
         * Validate manager is accessing
         * marks account as frozen when manager selects it on manage account page
         */
        post("/api/manager/users/{userId}/freeze") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val userId = call.parameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val account =
                transaction {
                    Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                }

            if (account == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            transaction {
                Users.update({ Users.userId eq userId }) { it[Users.status] = AccountStatus.FROZEN }
            }

            call.respond(HttpStatusCode.OK)
        }

        /**
         * Marks account as active again when changed from frozen to active
         */
        post("/api/manager/users/{userId}/unfreeze") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val userId = call.parameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val account =
                transaction {
                    Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                }

            if (account == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            transaction {
                Users.update({ Users.userId eq userId }) { it[Users.status] = AccountStatus.ACTIVE }
            }

            call.respond(HttpStatusCode.OK)
        }

        /**
         * Soft deletes accounts, they remain in the database but marked as deleted
         */
        post("/api/manager/users/{userId}/delete") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val userId = call.parameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val account =
                transaction {
                    Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                }

            if (account == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            transaction {
                Users.update({ Users.userId eq userId }) { it[Users.status] = AccountStatus.DELETED }
            }

            call.respond(HttpStatusCode.OK)
        }

        /**
         * Restores account marked as deleted to active.
         */
        post("/api/manager/users/{userId}/restore") {
            val sessionId = call.request.queryParameters["sessionId"]

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val userId = call.parameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val account =
                transaction {
                    Users.selectAll().where { Users.userId eq userId }.singleOrNull()
                }

            if (account == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            transaction {
                Users.update({ Users.userId eq userId }) { it[Users.status] = AccountStatus.ACTIVE }
            }

            call.respond(HttpStatusCode.OK)
        }

        /**
         * Validates manager access
         * Receives points and userId from input object on manage accounts page
         * Increases the points on that users account by the amount specified.
         */
        post("/api/manager/users/{userId}/points") {
            val sessionId = call.request.queryParameters["sessionId"]

            val request = call.receive<AddPointsRequest>()

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val userId = call.parameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val loyaltyService = LoyaltyService()
            val loyaltyAccount = loyaltyService.getLoyaltyAccount(userId) ?: loyaltyService.createLoyaltyAccount(userId)

            val success = loyaltyService.addPoints(userId, request.points)

            call.respond(HttpStatusCode.OK, success)
        }

        post("/api/manager/users/{userId}/points/remove") {
            val sessionId = call.request.queryParameters["sessionId"]

            val request = call.receive<AddPointsRequest>()

            if (sessionId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid sessionId")
                return@post
            }

            val user = authenticationService.validateSession(sessionId)

            if (user == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            if (user !is Manager) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val userId = call.parameters["userId"]?.toIntOrNull()

            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "invalid user")
                return@post
            }

            val loyaltyService = LoyaltyService()

            val success = loyaltyService.removePoints(userId, request.points)

            call.respond(HttpStatusCode.OK, success)
        }
    }
}
